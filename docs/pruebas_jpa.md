# Pruebas JPA de FitZone

Archivo: `src/test/java/es/safareyes/gimnasio/repositorios/RepositoriosJpaIntegracionTest.java`.

La clase prueba los repositorios reales con `@DataJpaTest` y PostgreSQL.
**No usa una base de datos en memoria** y **revierte cada transaccion** al acabar,
incluida la consulta masiva que desactiva al socio moroso.

## Preparacion

1. En la base `postgres` y solo en el esquema `gimnasio_test`, aplicar
   `sql/01_ajustes_fitzone.sql` si aun no se ha aplicado.
2. Ejecutar `sql/02_checks_fitzone.sql` para mantener los CHECK
   obligatorios y corregir el antiguo `usuario_rol_check`.
3. En tablas vacias, ejecutar `sql/data.sql` **una vez**.
4. Deben existir exactamente 20 socios, 2 planes, 60 cuotas, 16 actividades,
   20 reservas, 14 accesos, 4 monitores y 12 usuarios.

## Ejecutar

Desde IntelliJ, clic derecho en `RepositoriosJpaIntegracionTest` > Run,
configurando **en la ejecucion JUnit** las mismas variables de entorno que
utiliza el arranque de Spring Boot:

- `DB_URL=jdbc:postgresql://localhost:5432/postgres`
- `DB_SCHEMA=gimnasio_test`
- `DB_USER=<tu usuario PostgreSQL>`
- `DB_PASSWORD=<tu contrasena PostgreSQL>`

Alternativamente, en un terminal donde esten definidas esas variables:

```bash
./mvnw -Dtest=RepositoriosJpaIntegracionTest test
```

Tambien puedes usar `./mvnw test` para ejecutar todas las pruebas.

## Que verifica

| Requisito | Prueba |
| --- | --- |
| Q1 metodos derivados | DNI, email, recuentos, cuotas, reservas y catalogos |
| Q2 JPQL | Plan del socio, morosos, informes y plazas por actividad |
| Q3 SQL nativo | EXTRACT de ingresos y DATE_TRUNC de accesos |
| Q4 @Modifying | Desactivar el unico moroso activo, con rollback |
| Q5 Proyecciones | Interfaz SocioResumen y record IngresoMensualDto |
| Q6 Paginacion | Socios y cuotas con Pageable |
| Q7 Specification | Filtros simultaneos por estado, plan, apellido y fecha |
| Q8 @EntityGraph | Actividades, monitor y coleccion de reservas |

## Si falla

- **Error de conexion o configuracion**: revisa las variables en la configuracion
  de **JUnit**, que es diferente de la configuracion de arranque normal.
- **`usuario_rol_check`**: comprueba que ejecutaste `sql/02_checks_fitzone.sql`.
- **Esperaba 20 socios, obtuvo 0**: la carga `sql/data.sql` no se completo.
- **Esperaba mas de 20 socios**: el esquema ya contenia datos; no cargues el script
  otra vez sin revisar el estado de las tablas.
- **Error al iniciar repositorio o JPQL**: copia la primera excepcion y
  la causa `Caused by` para corregir la consulta concreta.
- Si los tests terminan, revisa `target/surefire-reports/` para el detalle.

## Limitaciones

Los tests son de **persistencia**. No validan aun todos los requisitos de negocio:
la comprobacion de DNI y estados en los servicios, el torno completo,
la concurrencia de las reservas, la seguridad ni los endpoints REST
se probaran en sus respectivas fases.

Los datos son de ejemplo y las aserciones estan pensadas para la carga
`sql/data.sql` sin otras modificaciones; se usan fechas derivadas de las
filas cargadas en lugar de depender del reloj del test.
