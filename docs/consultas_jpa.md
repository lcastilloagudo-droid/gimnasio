# Consultas JPA de FitZone (fase 2)

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
- Quedan sin uso desde servicios los demás métodos declarados de `ISocioRepository` y `IPlanRepository`, y todas las consultas de `ICuotaRepository`, `IAccesoRepository`, `IActividadRepository`, `IReservaRepository`, `IMonitorRepository` e `IUsuarioRepository`. Varias ya se verifican en las pruebas Q1–Q8. El requisito de usarlas desde servicios sigue pendiente.

## Cuotas e informes

- La proyección de morosos trae el identificador del socio, su DNI y nombre, y el identificador e importe de la cuota pendiente.
- Para el endpoint de ingresos usar la fecha real de cobro y el plan histórico. Por ejemplo, una cuota de enero pagada en febrero cuenta en febrero. La proyección JPQL por mes de cuota conserva la consulta por devengo. Ambas suman importes históricos y pueden devolver un grupo sin plan para cuotas antiguas.
- En la generación mensual, el servicio debe ejecutar la desactivación y la generación en una transacción. La actualización masiva recibe la hora de Madrid para completar `actualizadoEn`, ya que no ejecuta `@PreUpdate`. La unicidad por socio, año y mes evita duplicados, pero comprobar que la cuota no existe y luego insertarla no es atómico. Serializar las ejecuciones de generación, o usar una inserción idempotente que cuente solo las filas creadas. Coordinar la lectura del plan vigente con el bloqueo de socio al generar cada cuota.
- En las consultas SQL nativas, pasar `Enum.name()` al comparar un enum persistido como texto.

## Aforo y reservas

- La entrada debe serializar el recuento y la inserción con un bloqueo común del torno. El bloqueo de un socio solo evita la carrera entre peticiones del mismo socio y no protege el aforo global. Guardar el acceso antes de intentar la bonificación de ParkControl.
- Para reservar, bloquear el socio y la actividad en un orden fijo antes de comprobar el máximo de reservas futuras y las plazas de la actividad. El índice parcial permite reservar otra vez tras cancelar, conserva el historial y bloquea otra reserva para una actividad con una vigente o ya asistida.
- Los listados paginados de socios e historial de cuotas cargan el plan y el historial de reservas carga la actividad. El listado diario mantiene el `EntityGraph` de monitor y reservas sin paginar la colección. Los mappers reciben las plazas libres y el resultado de integración calculados por el servicio.
