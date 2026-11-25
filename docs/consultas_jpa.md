# Consultas JPA de FitZone (fase 2)

Referencia: Proyecto_Backend_SafaReyes.pdf, secciones 3.3 y 6.7.
La tabla de trazabilidad final se completara al implementar los servicios y endpoints.
Las consultas pasaron las ocho pruebas de RepositoriosJpaTest antes del cambio de enums. Pendiente repetir las pruebas con los nuevos tipos.

| Tipo exigido | Ubicacion | Ejemplos de consultas |
| --- | --- | --- |
| Q1 derivadas (min. 5) | ISocioRepository, ICuotaRepository, IAccesoRepository, IActividadRepository, IReservaRepository | findByDniIgnoreCase, existsByIdSocio_IdAndMesAndAnyo, findFirstByIdSocio_IdOrderByFechaHoraEntradaDesc, countByIdActividad_IdAndEstado |
| Q2 JPQL (min. 3) | ISocioRepository, ICuotaRepository, IAccesoRepository, IActividadRepository | buscarConPlan, buscarSociosConCuotaPendiente, resumirCuotasPagadasPorMesYPlan, contarPersonasDentro |
| Q3 nativa (min. 2) | ICuotaRepository, IAccesoRepository | ingresosCobradosPorMesYPlan (EXTRACT), contarEntradasPorHora (DATE_TRUNC) |
| Q4 modificacion (min. 1) | ISocioRepository | desactivarSociosMorosos (UPDATE + @Modifying) |
| Q5 proyecciones (min. 2) | ISocioRepository, ICuotaRepository | SocioResumen (interfaz), IngresoMensualDto (record + SELECT new) |
| Q6 paginacion (min. 1) | ISocioRepository, ICuotaRepository, IReservaRepository | Page y Pageable en listados |
| Q7 filtros dinamicos (min. 1) | SocioSpecifications + ISocioRepository | conFiltros(...) y findAll(spec, pageable) |
| Q8 evitar N+1 (min. 1) | IActividadRepository | @EntityGraph de actividades con sus reservas (coleccion) y monitor |

## Uso esperado
- Socios: alta, busqueda paginada y baja logica. Con las cuotas pendientes se desactiva al moroso.
- Cuotas: generacion mensual, historial paginado, cobro e informes; el servicio decide estados y transacciones.
- Accesos: comprobar si hay entrada abierta, contar aforo del dia y registrar salida.
- Actividades y reservas: consultar sesiones, plazas, duplicados, maximo tres futuras; usar bloqueo pesimista durante reservas para evitar sobreaforo.
- Usuarios: consulta para autenticacion futura, sin devolver contrasenas en la API.
- Los estados de cuota y los resultados de acceso son enums persistidos con EnumType.STRING. Las consultas derivadas y JPQL reciben el enum; las consultas SQL nativas reciben su nombre mediante `.name()` para comparar con el VARCHAR de PostgreSQL.

## Verificaciones necesarias
1. Actualizar la rama `test` y arrancar Spring Boot con `ddl-auto=validate`.
2. Ejecutar pruebas de repositorios contra PostgreSQL con datos realistas.
3. Comprobar valores reales de `estado` en cuota y `tipo_resultado` en acceso.
4. Completar servicios y controladores: los requisitos requieren **usar** estas consultas, no solo declararlas.
5. Contrastar el informe de cuotas por mes de devengo con los ingresos por fecha de pago: no son la misma magnitud.
6. Validar que una actividad con dos reservas solo aparece una vez en la consulta `findDistinctBy...`.
