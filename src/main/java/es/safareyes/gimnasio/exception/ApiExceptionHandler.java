package es.safareyes.gimnasio.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(PeticionInvalidaException.class)
    public ProblemDetail peticionInvalida(PeticionInvalidaException ex) {
        return validacion(Map.of(ex.getCampo(), ex.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio", ex.getMessage());
    }

    @ExceptionHandler(IntegracionException.class)
    public ProblemDetail integracion(IntegracionException ex) {
        log.warn("Servicio externo no disponible", ex);
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible",
                "El servicio externo requerido no está disponible");
    }

    // Las restricciones UNIQUE protegen tambien frente a altas concurrentes.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql) {
                if ("23505".equals(sql.getSQLState())) {
                    return problema(HttpStatus.CONFLICT, "Conflicto", "Ya existe un registro con esos datos");
                }
                if ("23503".equals(sql.getSQLState()) || "23514".equals(sql.getSQLState())) {
                    return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio",
                            "La operación incumple una restricción de los datos");
                }
            }
        }
        log.error("Error de integridad al guardar datos", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "No se pudo guardar la operación");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return handleExceptionInternal(ex, validacion(errores), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (ex.isForReturnValue()) {
            return super.handleHandlerMethodValidationException(ex, headers, status, request);
        }
        Map<String, String> errores = new LinkedHashMap<>();
        for (var resultado : ex.getParameterValidationResults()) {
            if (resultado instanceof ParameterErrors campos) {
                campos.getFieldErrors().forEach(error ->
                        errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
            } else {
                String nombre = resultado.getMethodParameter().getParameterName();
                resultado.getResolvableErrors().forEach(error ->
                        errores.putIfAbsent(nombre, error.getDefaultMessage()));
            }
        }
        return handleExceptionInternal(ex, validacion(errores), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String campo = ex.getPropertyName() == null ? "parametro" : ex.getPropertyName();
        return handleExceptionInternal(ex, validacion(Map.of(campo, "Formato no válido")),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, validacion(Map.of(ex.getParameterName(), "Es obligatorio")),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, validacion(Map.of("cuerpo", "El cuerpo debe contener un JSON válido")),
                headers, status, request);
    }

    private ProblemDetail validacion(Map<String, String> errores) {
        ProblemDetail detalle = problema(HttpStatus.BAD_REQUEST,
                "Error de validación", "Revisa los campos de la petición");
        detalle.setProperty("errores", errores);
        return detalle;
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail detalle) {
            detalle.setProperty("timestamp", LocalDateTime.now(ZoneId.of("Europe/Madrid")));
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String mensaje) {
        ProblemDetail detalle = ProblemDetail.forStatusAndDetail(estado, mensaje);
        detalle.setTitle(titulo);
        detalle.setProperty("timestamp", LocalDateTime.now(ZoneId.of("Europe/Madrid")));
        return detalle;
    }
}
