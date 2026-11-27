-- Aplicar al esquema existente de FitZone. Sustituir gimnasio_test si corresponde.
-- La transacción falla si los datos no cumplen las nuevas restricciones.
-- Los registros y la antigua tabla plan_socio, si aún existe, se conservan.
BEGIN;
SET LOCAL search_path TO gimnasio_test;

ALTER TABLE acceso DROP CONSTRAINT IF EXISTS acceso_tipo_resultado_check;
ALTER TABLE actividad
    DROP CONSTRAINT IF EXISTS actividad_aforo_max_check,
    DROP CONSTRAINT IF EXISTS actividad_duracion_check;
ALTER TABLE cuota
    DROP CONSTRAINT IF EXISTS cuota_anio_check,
    DROP CONSTRAINT IF EXISTS cuota_estado_check,
    DROP CONSTRAINT IF EXISTS cuota_importe_check,
    DROP CONSTRAINT IF EXISTS cuota_mes_check,
    DROP CONSTRAINT IF EXISTS chk_cuota_estado,
    ADD CONSTRAINT chk_cuota_estado CHECK (estado IN ('PENDIENTE', 'PAGADA'));
ALTER TABLE reserva DROP CONSTRAINT IF EXISTS reserva_estado_check;

ALTER TABLE plan ADD CONSTRAINT chk_plan_tipo CHECK (tipo IN ('BASICO', 'PREMIUM'));
ALTER TABLE socio ADD CONSTRAINT chk_socio_activo_con_plan CHECK (NOT activo OR id_plan IS NOT NULL);
ALTER TABLE usuario ADD CONSTRAINT uq_usuario_socio UNIQUE (id_socio);
DROP INDEX IF EXISTS idx_usuario_socio;

CREATE UNIQUE INDEX uq_reserva_socio_actividad_vigente
    ON reserva (id_socio, id_actividad) WHERE estado <> 'CANCELADA';
ALTER TABLE reserva DROP CONSTRAINT IF EXISTS uq_reserva_socio_actividad;

COMMIT;
