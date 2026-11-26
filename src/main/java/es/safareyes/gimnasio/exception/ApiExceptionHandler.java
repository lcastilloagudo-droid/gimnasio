package es.safareyes.gimnasio.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
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
        return problema(HttpStatus.BAD_REQUEST, "Peticion no valida", ex.getMessage());
    }

    // Las restricciones UNIQUE protegen tambien frente a altas concurrentes.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return problema(HttpStatus.CONFLICT, "Conflicto", "Ya existe un registro con esos datos");
            }
        }
        return problema(HttpStatus.CONFLICT, "Conflicto de integridad",
                "La operacion incumple una restriccion de la base de datos");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail detalle = problema(HttpStatus.BAD_REQUEST,
                "Error de validacion", "Revisa los campos de la peticion");
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detalle.setProperty("errores", errores);
        return handleExceptionInternal(ex, detalle, headers, status, request);
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
