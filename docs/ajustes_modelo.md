# Adaptacion de FitZone a los requisitos de backend

## Referencia
Proyecto_Backend_SafaReyes.pdf, caso 2 FitZone, secciones 6.1 a 6.8.

## Ajustes de modelo (fase 1)
- `Plan.precioMensual`: cada plan tiene su tarifa, que no se inventa en la migracion.
- `Socio.matricula`: texto opcional con formato 1234BCD, no booleano.
- `Socio.plan`: asociacion al plan actual; los cambios solo afectan cuotas futuras.
- `Cuota.plan`: plan historico para informes por mes y plan; `importe` conserva el precio cobrado.
- `Socio.creadoEn/actualizadoEn`: auditoria mediante callbacks JPA.
- `Rol`: ADMIN, RECEPCION y SOCIO, como exige FitZone.

## Validaciones en el código
Las restricciones CHECK de formato y de negocio no se utilizan en el esquema de pruebas.
Se eliminan con `sql/02_eliminar_checks.sql`, incluidas las restricciones antiguas
`usuario_rol_check`, `chk_socio_matricula` y `chk_plan_precio_mensual`.
`SocioRequest` valida DNI, matrícula, email y campos obligatorios.
`PlanRequest` valida el importe y datos del plan.
`Rol` es un enum Java con ADMIN, RECEPCION y SOCIO.
Los controladores deberán usar `@Valid` y los servicios comprobar duplicados,
estados y permisos. Las PK, FK y UNIQUE siguen en PostgreSQL como integridad estructural.

**Aviso:** el enunciado del profesor pide CHECK en el esquema.
Esta decisión sigue la preferencia del proyecto, pero debe revisarse antes de entregar.

## Pasos para aplicar
1. Haz copia de seguridad de la base si contiene datos.
2. En DBeaver, selecciona la base `postgres` y ejecuta `sql/01_ajustes_fitzone.sql`.
3. Verifica que `DB_SCHEMA=gimnasio_test` y ejecuta Spring Boot con `ddl-auto=validate`.
4. Introduce precios reales en los planes y asigna el plan de los socios que ya existan.
5. La tabla anterior `plan_socio` permanece sin borrar mientras se revisa su contenido.

## Pendiente para completar fase 1
- Confirmar precios de planes BASICO y PREMIUM.
- Consolidar `schema.sql` completo, `data.sql` (min. 10 filas en tablas principales)
  y `docs/consultas.sql` (3 consultas).
- Revisar datos existentes y eventualmente retirar `plan_socio` sin perder historial.
- Verificar convenciones y restricciones de dominio que faltan en el esquema.

## Pendiente fase 2
Las 16 consultas Q1-Q8 y sus pruebas; `@EntityGraph` para evitar N+1.
