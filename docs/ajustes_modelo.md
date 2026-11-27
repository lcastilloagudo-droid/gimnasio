# Modelo y migración de FitZone

Referencia: enunciado de DWES, secciones 3, 6 y 9, y manuales de Spring Boot, partes 2 y 3.

## Plan y cuotas

El RF-01 permite un socio sin plan y RN-03/RN-04 necesitan un precio para generar la cuota de cada socio activo. Las altas con plan quedan activas y las altas sin plan quedan inactivas. Un socio inactivo puede quitar su plan o asignarse uno antes de activarse. La API devuelve 422 al quitar el plan de un socio activo o al activar sin plan. Las escrituras sobre el socio se serializan con un bloqueo de fila y el CHECK protege también las escrituras directas en PostgreSQL.

El PUT sustituye el contacto, el plan y la matrícula. Un `null` en matrícula la elimina. Cambiar el plan del socio afecta a cuotas futuras (RN-09). Cada cuota ya generada conserva su relación al plan histórico y el importe aplicado.

## Base existente

Aplica una sola vez `sql/03_consolidacion_fitzone.sql` sobre la base existente si ya tiene el modelo de `01_ajustes_fitzone.sql` y los CHECK de `02_checks_fitzone.sql`. Elige la base y revisa el esquema de `SET LOCAL search_path`; el valor del repositorio es `gimnasio_test`. Haz una copia de seguridad antes y ejecuta el script completo en una transacción. Con `psql`, usa `-v ON_ERROR_STOP=1`. Si el cliente deja abierta una transacción fallida, ejecuta `ROLLBACK`.

Antes de aplicarla puedes detectar incompatibilidades con estas consultas de solo lectura en el esquema elegido:

```sql
SELECT id_socio FROM socio WHERE activo AND id_plan IS NULL;
SELECT id_socio, count(*) FROM usuario WHERE id_socio IS NOT NULL
GROUP BY id_socio HAVING count(*) > 1;
SELECT id_plan FROM plan WHERE tipo NOT IN ('BASICO', 'PREMIUM');
SELECT id_cuota FROM cuota WHERE estado NOT IN ('PENDIENTE', 'PAGADA');
SELECT id_socio, id_actividad, count(*) FROM reserva
WHERE estado <> 'CANCELADA'
GROUP BY id_socio, id_actividad HAVING count(*) > 1;
```

Si alguna consulta devuelve filas, consulta qué plan, estado o vínculo corresponde antes de corregir esos datos manualmente. La migración valida las restricciones sin reasignar socios ni cambiar importes o estados. Si aún existe la antigua tabla `plan_socio`, la migración conserva sus filas y su estructura. El esquema de instalaciones nuevas ya no incluye esa tabla.

## Autenticación e integración pendientes

FitZone identifica las cuentas por `nomUsuario`. El DNI del registro localiza la ficha, pero cualquiera podría conocerlo. Antes de vincular una cuenta, habrá que comprobar un código de un solo uso y con caducidad entregado por recepción tras verificar la identidad del socio, o enviado a un canal previamente verificado que ya figure en su ficha. La solicitud no debe poder elegir ese canal. El servidor debe consumir el código y vincular el usuario en la misma transacción, asignar siempre `SOCIO` en el registro público y limitar los intentos.

La futura autorización de propiedad comparará el `idSocio` del usuario autenticado con el socio del recurso. La FK opcional y única permite tener cuentas de personal sin ficha de socio y como máximo una cuenta por ficha. El JWT caducará a los 60 minutos. Las integraciones usarán `X-API-KEY` y configuración sensible externa al repositorio. Un fallo de ParkControl se reflejará en el bloque `integracion` sin revocar la entrada guardada. El 503 se reservará para operaciones que dependan del servicio externo para poder completarse.
