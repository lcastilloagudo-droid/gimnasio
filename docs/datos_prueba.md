# Datos de prueba de FitZone

El archivo `sql/data.sql` contiene exclusivamente **datos ficticios** para el esquema `gimnasio_test`. No es un script para producción.

## Ejecución en DBeaver

1. Comprueba que el esquema `gimnasio_test` está vacío.
2. Ejecuta primero `sql/01_ajustes_fitzone.sql` si no lo has aplicado.
3. Selecciona la base `postgres` y ejecuta `sql/data.sql` completo.
4. No lo ejecutes dos veces: los registros se insertan una sola vez y no se borran datos.
5. Arranca Spring Boot y comprueba los repositorios.

## Filas esperadas

| Tabla | Registros |
| --- | ---: |
| plan | 2 |
| socio | 20 |
| monitor | 4 |
| cuota | 60 |
| actividad | 16 |
| reserva | 20 |
| acceso | 14 |
| usuario | 12 |

## Escenarios de prueba

- **Socios**: `socio01@ejemplo.test` está activo, tiene plan PREMIUM y matrícula `1234BCD`; el socio 12 es BASICO; los socios 7, 16 y 19 están inactivos.
- **Cuotas**: los socios 2, 7, 8, 15, 16 y 19 tienen cuota actual pendiente; el socio 4 cambió de BASICO a PREMIUM (las cuotas anteriores conservan el plan anterior). Hay 12 cuotas pendientes en total.
- **Modificación Q4**: el socio 15 tiene dos cuotas antiguas pendientes pero sigue activo para probar `desactivarSociosMorosos("PENDIENTE", 2)`. Si se ejecuta una sola vez sobre los datos recién cargados, debe actualizar **1 socio**.
- **Torno**: hay tres accesos permitidos de hoy sin salida (socios 1, 3 y 6), más entradas con salida y denegaciones por INACTIVO, CUOTA_PENDIENTE y YA_DENTRO. No se simula el aforo completo de 150 personas.
- **Actividades**: Spinning de mañana a las 18:00 tiene aforo 2 y dos reservas ACTIVA: está completo.
- **Reservas**: socios 1, 4 y 6 tienen 3 reservas futuras ACTIVA; hay también canceladas y asistidas.
- **Usuarios de prueba**: `admin_fitzone`, `recepcion_fitzone` y `socio01` a `socio10`; todos con contraseña de **pruebas** `Test1234!` y hash BCrypt. La autenticación todavía no está implementada.

## Comprobaciones SQL rápidas

```sql
SET search_path TO gimnasio_test;

SELECT 'plan' AS tabla, COUNT(*) AS filas FROM plan
UNION ALL SELECT 'socio', COUNT(*) FROM socio
UNION ALL SELECT 'monitor', COUNT(*) FROM monitor
UNION ALL SELECT 'cuota', COUNT(*) FROM cuota
UNION ALL SELECT 'actividad', COUNT(*) FROM actividad
UNION ALL SELECT 'reserva', COUNT(*) FROM reserva
UNION ALL SELECT 'acceso', COUNT(*) FROM acceso
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario;

SELECT COUNT(*) AS cuotas_pendientes FROM cuota WHERE estado = 'PENDIENTE';

SELECT COUNT(*) AS aforo_actual
FROM acceso
WHERE tipo_resultado = 'PERMITIDO'
  AND fecha_hora_salida IS NULL
  AND fecha_hora_entrada >= CURRENT_DATE
  AND fecha_hora_entrada < CURRENT_DATE + 1;

SELECT a.nombre, a.fecha_hora, a.aforo_max, COUNT(r.id_reserva) AS plazas_ocupadas
FROM actividad a
LEFT JOIN reserva r ON r.id_actividad = a.id_actividad AND r.estado = 'ACTIVA'
WHERE a.nombre = 'Spinning' AND a.fecha_hora = CURRENT_DATE + 1 + TIME '18:00'
GROUP BY a.id_actividad, a.nombre, a.fecha_hora, a.aforo_max;
```

## Limitaciones y advertencias

- El script se ha escrito a partir de las entidades JPA y la migración que están en GitHub. **El DDL original de las tablas no está en el repositorio**: si hay restricciones adicionales diferentes, habrá que ajustar los datos a ese DDL antes de ejecutarlo.
- Los precios de 30 y 50 euros son **ficticios**, solo para pruebas.
- Las fechas son relativas a `CURRENT_DATE`; los recuentos esperados de actividades futuras y aforo corresponden al día de inserción.
- Para probar el aforo global completo de 150 personas, se necesita otra carga específica.
- El script usa una transacción: si PostgreSQL encuentra un error, no se insertan los datos parcialmente. Comprueba el resultado de DBeaver.
