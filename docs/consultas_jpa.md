# Consultas JPA de FitZone

Referencia: Proyecto_Backend_SafaReyes.pdf, secciones 3.3 y 6.7.
La trazabilidad completa hasta cada endpoint se incorporará conforme se implementen los servicios.

| Tipo exigido | Ubicacion | Ejemplos de consultas |
| --- | --- | --- |
| Q1 derivadas (min. 5) | ISocioRepository, ICuotaRepository, IAccesoRepository, IActividadRepository, IReservaRepository | findByDniIgnoreCase, existsByIdSocio_IdAndMesAndAnyo, findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc, countByIdActividad_IdAndEstado |
| Q2 JPQL (min. 3) | ISocioRepository, ICuotaRepository, IAccesoRepository, IActividadRepository | buscarConPlan, buscarSociosConCuotaPendiente, resumirCuotasPagadasPorMesYPlan, contarPersonasDentro |
| Q3 nativa (min. 2) | ICuotaRepository, IAccesoRepository | ingresosCobradosPorMesYPlan (EXTRACT), contarEntradasPorHora (DATE_TRUNC) |
| Q4 modificacion (min. 1) | ISocioRepository | desactivarSociosMorosos (UPDATE + @Modifying con fecha de auditoría) |
| Q5 proyecciones (min. 2) | ISocioRepository, ICuotaRepository | SocioResumen (interfaz), IngresoMensualDto y SocioMorosoResponse (records + SELECT new) |
| Q6 paginacion (min. 1) | ISocioRepository, ICuotaRepository, IReservaRepository | Page y Pageable en listados |
| Q7 filtros dinamicos (min. 1) | SocioSpecifications + ISocioRepository | conFiltros(...) y findAll(spec, pageable) |
| Q8 evitar N+1 (min. 1) | IActividadRepository | @EntityGraph de actividades con sus reservas (coleccion) y monitor |

## Uso actual

- `SocioService` usa `buscarConPlan`, `bloquearPorId`, `existsByDniIgnoreCase`, `existsByEmailIgnoreCase`, `existsByEmailIgnoreCaseAndIdNot`, `findAll(spec, pageable)` y las operaciones CRUD de socio y plan.
- `PlanService` usa `findAllByOrderByPrecioMensualAsc`.
- Los métodos de cuotas que ya usa `CuotaService` se detallan abajo. Las consultas restantes de esas interfaces y las de Acceso, Actividad, Reserva, Monitor y Usuario se conservan para fases y servicios posteriores. Varias ya se verifican en las pruebas Q1–Q8.

## Gestión de cuotas

| Endpoint | Consultas que usa |
| --- | --- |
| `POST /api/v1/cuotas/generar` | `bloquearParaGenerarCuotas`, `desactivarSociosMorosos`, `findByActivoTrue`, `existsByIdSocio_IdAndMesAndAnyo`, `saveAll` |
| `POST /api/v1/cuotas/pagar` | `bloquearParaPago`, `save` |
| `GET /api/v1/socios/{id}/cuotas` | `existsById`, `findByIdSocio_IdOrderByAnyoDescMesDesc` o `findByIdSocio_Id` si se indica `sort`. Ambas consultas de cuotas cargan el plan histórico. |
| `GET /api/v1/cuotas/pendientes` | `buscarSociosConCuotaPendiente` proyecta el DNI, nombre, ID del socio, ID de la cuota e importe. Ordena por apellidos, nombre e ID del socio. |

La generación toma un bloqueo exclusivo de PostgreSQL sobre la tabla `socio` durante una transacción. Otras generaciones y las escrituras de socios esperan, incluidas las que primero bloquean una fila para cambiar el plan o el estado. Las lecturas ordinarias pueden continuar. La restricción única por socio, año y mes protege también las escrituras fuera del servicio.

RN-04 se aplica antes de insertar. Para el umbral de dos pendientes se cuentan las cuotas de otros periodos y se excluye la del periodo solicitado. Si un socio tenía una pendiente y se genera otra, repetir la generación de ese mes no lo desactiva por esa cuota nueva. Si los datos siguen igual, el reintento devuelve ambos contadores a cero. Una nueva generación puede incorporar socios que se hayan activado o desactivar socios cuya deuda en otros periodos haya cambiado. La actualización masiva fija `actualizadoEn` con la hora de Madrid.

Al generar una cuota se guarda el plan vigente y su precio. Las cuotas existentes conservan importe y plan, incluso si el socio cambia de plan. El pago bloquea la fila antes de comprobar su estado. De dos pagos concurrentes solo uno se registra y el otro obtiene `409`. La fecha se fija con la zona Europe/Madrid y el socio conserva su estado.

El historial devuelve una página vacía para un socio existente sin cuotas y `404` si el socio no existe. El orden por defecto es año y mes descendentes. La unicidad del periodo para cada socio hace estable ese orden. El orden personalizado admite `id`, `mes`, `anio`, `importe`, `estado` y `fechaPago`, con `id` como desempate. En la fase 4 se aplicarán los roles de cada ruta y el límite de acceso a su propio historial para el rol `SOCIO`.

La consulta `existsByIdSocio_IdAndMesAndAnyoAndEstado` queda disponible con `EstadoCuota.PAGADA` para Acceso, Reserva e integración. Una cuota pendiente aparece en el listado correspondiente aunque sea la única pendiente del socio.

### Peticiones para Postman o Bruno

Usa `http://localhost:8082` como base. Los identificadores y los importes de los ejemplos son ilustrativos. Para probar el pago, toma un `idCuota` del listado de pendientes.

| Petición | Respuesta de ejemplo |
| --- | --- |
| `POST /api/v1/cuotas/generar?mes=4&anio=2090` | `200` con `{"cuotasCreadas":16,"sociosDesactivados":1}` en una base de prueba recién poblada. Un reintento inmediato devuelve `{"cuotasCreadas":0,"sociosDesactivados":0}`. |
| `GET /api/v1/cuotas/pendientes?mes=4&anio=2090` | `200` con una lista como `[{"idSocio":1,"dni":"00000001R","nombre":"Ana","apellidos":"Lopez Martin","idCuota":61,"importe":50.00}]` o `[]` para un periodo sin pendientes. |
| `POST /api/v1/cuotas/pagar` con `Content-Type: application/json` y cuerpo `{"idCuota":61}` | `200` con la cuota, `estado: "PAGADA"` y `fechaPago` del día en Madrid. Un segundo pago devuelve `409`. Un ID inexistente devuelve `404`. |
| `GET /api/v1/socios/1/cuotas?page=0&size=2` | `200` con `content` y los totales de paginación. Cada cuota incluye el plan histórico si está disponible. Un socio inexistente devuelve `404`. |

Ejemplo de cuerpo que devuelve el pago:

```json
{
  "id": 61,
  "idSocio": 1,
  "plan": {"id": 2, "nombre": "PREMIUM", "tipo": "PREMIUM", "precioMensual": 50.00},
  "mes": 4,
  "anio": 2090,
  "importe": 50.00,
  "estado": "PAGADA",
  "fechaPago": "2026-11-27"
}
```

El historial devuelve las cuotas con esa misma estructura en `content`. Un extracto de una página vacía es `{"content":[],"totalElements":0,"totalPages":0}`. Un extracto del error al repetir un pago es `{"status":409,"detail":"La cuota ya está pagada","instance":"/api/v1/cuotas/pagar"}`.

Prueba también `mes=13` o la ausencia de `anio` en las dos rutas por periodo, y `{"idCuota":0}` en el pago. La API devuelve `400` con `application/problem+json` y un objeto `errores` por campo. Por ejemplo, `{"status":400,"errores":{"mes":"debe ser menor o igual que 12"}}` muestra un extracto de la respuesta.

## Informes pendientes

- Para el endpoint de ingresos se usará la fecha real de cobro y el plan histórico. Una cuota de enero pagada en febrero cuenta en febrero. La proyección JPQL por mes de cuota conserva la consulta por devengo. Ambas pueden devolver un grupo sin plan para cuotas antiguas.
- En las consultas SQL nativas, pasar `Enum.name()` al comparar un enum persistido como texto.

## Aforo y reservas

- La entrada debe serializar el recuento y la inserción con un bloqueo común del torno. El bloqueo de un socio solo evita la carrera entre peticiones del mismo socio y no protege el aforo global. Guardar el acceso antes de intentar la bonificación de ParkControl.
- Para reservar, bloquear el socio y la actividad en un orden fijo antes de comprobar el máximo de reservas futuras y las plazas de la actividad. El índice parcial permite reservar otra vez tras cancelar, conserva el historial y bloquea otra reserva para una actividad con una vigente o ya asistida.
- Los listados paginados de socios e historial de cuotas cargan el plan y el historial de reservas carga la actividad. El listado diario mantiene el `EntityGraph` de monitor y reservas sin paginar la colección. Los mappers reciben las plazas libres y el resultado de integración calculados por el servicio.
