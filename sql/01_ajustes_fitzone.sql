-- FitZone: adaptacion incremental de la BD del proyecto.
-- Ejecutar en DBeaver conectado a la BD postgres, schema gimnasio_test.
-- IMPORTANTE: haz copia de seguridad si ya hay datos; no ejecutar sobre gimnasio.
BEGIN;
SET LOCAL search_path TO gimnasio_test;

-- El precio de cada plan es obligatorio para el negocio.
-- Se deja NULL hasta configurar los precios reales de BASICO y PREMIUM.
ALTER TABLE plan
    ADD COLUMN IF NOT EXISTS precio_mensual NUMERIC(10,2);
ALTER TABLE plan
    DROP CONSTRAINT IF EXISTS chk_plan_precio_mensual;
ALTER TABLE plan
    ADD CONSTRAINT chk_plan_precio_mensual
    CHECK (precio_mensual IS NULL OR precio_mensual >= 0);

-- 'matricula' almacenaba un booleano. No representa una matricula real.
-- Si fuera boolean, convertimos el campo a texto opcional.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'gimnasio_test'
          AND table_name = 'socio' AND column_name = 'matricula'
          AND data_type = 'boolean'
    ) THEN
        ALTER TABLE gimnasio_test.socio ALTER COLUMN matricula DROP DEFAULT;
        ALTER TABLE gimnasio_test.socio ALTER COLUMN matricula DROP NOT NULL;
        ALTER TABLE gimnasio_test.socio
            ALTER COLUMN matricula TYPE VARCHAR(7) USING NULL::VARCHAR(7);
        -- Se pierden los antiguos true/false, que nunca fueron matriculas.
    END IF;
END $$;

ALTER TABLE socio ALTER COLUMN matricula DROP NOT NULL;
ALTER TABLE socio DROP CONSTRAINT IF EXISTS chk_socio_matricula;
ALTER TABLE socio ADD CONSTRAINT chk_socio_matricula
    CHECK (matricula IS NULL OR matricula ~ '^[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}$');

-- Plan vigente del socio; se asignara en las altas y cambios de plan.
ALTER TABLE socio ADD COLUMN IF NOT EXISTS id_plan BIGINT;
ALTER TABLE socio DROP CONSTRAINT IF EXISTS fk_socio_plan;
ALTER TABLE socio ADD CONSTRAINT fk_socio_plan
    FOREIGN KEY (id_plan) REFERENCES plan(id_plan) ON DELETE RESTRICT;

-- Guardar en cada cuota el plan historico, sin afectar cuotas anteriores al cambiarlo.
ALTER TABLE cuota ADD COLUMN IF NOT EXISTS id_plan BIGINT;
ALTER TABLE cuota DROP CONSTRAINT IF EXISTS fk_cuota_plan;
ALTER TABLE cuota ADD CONSTRAINT fk_cuota_plan
    FOREIGN KEY (id_plan) REFERENCES plan(id_plan) ON DELETE RESTRICT;

-- Auditoria automatica en la entidad Socio.
ALTER TABLE socio ADD COLUMN IF NOT EXISTS creado_en TIMESTAMP;
ALTER TABLE socio ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_socio_id_plan ON socio(id_plan);
CREATE INDEX IF NOT EXISTS idx_cuota_id_plan ON cuota(id_plan);

COMMIT;

-- DESPUES: asignar precio_mensual a BASICO y PREMIUM, sin inventar importes.
-- Para datos existentes: completar socio.id_plan y cuota.id_plan segun historial real.
-- La antigua tabla plan_socio se conserva por seguridad hasta migrar datos.
