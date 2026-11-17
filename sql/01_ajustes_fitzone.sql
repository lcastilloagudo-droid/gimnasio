-- FitZone: ajustes de esquema para PostgreSQL.
-- Ejecutar en la base postgres, esquema gimnasio_test.
-- No ejecutar en el esquema gimnasio; hacer copia si contiene datos.
-- Los CHECK y las validaciones Java son complementarios.
-- Ejecutar despues sql/02_checks_fitzone.sql para alinear las restricciones
-- de todas las tablas existentes, incluido el rol RECEPCION.

BEGIN;
SET LOCAL search_path TO gimnasio_test;

-- Precio mensual obligatorio y positivo.
ALTER TABLE plan
    ADD COLUMN IF NOT EXISTS precio_mensual NUMERIC(10,2);

ALTER TABLE plan
    ALTER COLUMN precio_mensual SET NOT NULL;
ALTER TABLE plan
    DROP CONSTRAINT IF EXISTS chk_plan_precio_mensual;
ALTER TABLE plan
    ADD CONSTRAINT chk_plan_precio_mensual CHECK (precio_mensual > 0);

-- La matricula es opcional y representa un texto, no un booleano.
-- Si se migra desde BOOLEAN, no se conservan los antiguos true/false.
ALTER TABLE socio ALTER COLUMN matricula DROP DEFAULT;
ALTER TABLE socio ALTER COLUMN matricula DROP NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'gimnasio_test'
          AND table_name = 'socio'
          AND column_name = 'matricula'
          AND data_type = 'boolean'
    ) THEN
        ALTER TABLE gimnasio_test.socio
            ALTER COLUMN matricula TYPE VARCHAR(7)
            USING NULL::VARCHAR(7);
    END IF;
END $$;

ALTER TABLE socio
    DROP CONSTRAINT IF EXISTS chk_socio_matricula;
ALTER TABLE socio
    ADD CONSTRAINT chk_socio_matricula
    CHECK (matricula IS NULL OR matricula ~ '^[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}$');

-- Plan actual del socio y plan historico de cada cuota.
ALTER TABLE socio ADD COLUMN IF NOT EXISTS id_plan BIGINT;
ALTER TABLE cuota ADD COLUMN IF NOT EXISTS id_plan BIGINT;

ALTER TABLE socio DROP CONSTRAINT IF EXISTS fk_socio_plan;
ALTER TABLE cuota DROP CONSTRAINT IF EXISTS fk_cuota_plan;

ALTER TABLE socio ALTER COLUMN id_plan TYPE BIGINT;
ALTER TABLE cuota ALTER COLUMN id_plan TYPE BIGINT;

ALTER TABLE socio
    ADD CONSTRAINT fk_socio_plan
    FOREIGN KEY (id_plan) REFERENCES plan(id_plan) ON DELETE RESTRICT;
ALTER TABLE cuota
    ADD CONSTRAINT fk_cuota_plan
    FOREIGN KEY (id_plan) REFERENCES plan(id_plan) ON DELETE RESTRICT;

-- Auditoria de Socio.
ALTER TABLE socio ADD COLUMN IF NOT EXISTS creado_en TIMESTAMP;
ALTER TABLE socio ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_socio_id_plan ON socio(id_plan);
CREATE INDEX IF NOT EXISTS idx_cuota_id_plan ON cuota(id_plan);

COMMIT;

-- Completar los precios reales de los planes y su asociacion a socios.
-- No eliminar plan_socio hasta revisar su contenido.
