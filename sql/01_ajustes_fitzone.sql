-- FitZone: migracion incremental e idempotente para PostgreSQL.
-- Ejecutar en la base postgres, esquema gimnasio_test.
-- No aplicar al esquema gimnasio original. Copia de seguridad si hay datos.
-- Despues ejecutar 02_eliminar_checks.sql para quitar todos los CHECK antiguos.
-- Las reglas de negocio y el formato se validan en Java (DTOs y servicios).

BEGIN;
SET LOCAL search_path TO gimnasio_test;

-- Plan: guardar precio mensual; la obligatoriedad y el importe se validan en Java.
ALTER TABLE plan
    ADD COLUMN IF NOT EXISTS precio_mensual NUMERIC(10,2);
ALTER TABLE plan
    ALTER COLUMN precio_mensual DROP NOT NULL;
ALTER TABLE plan
    DROP CONSTRAINT IF EXISTS chk_plan_precio_mensual;

-- Matricula: dato opcional y texto de 7 caracteres, nunca BOOLEAN.
-- La conversion desde BOOLEAN descarta el valor anterior, que no es matricula.
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

-- El formato se comprueba con @Pattern en SocioRequest.
ALTER TABLE socio DROP CONSTRAINT IF EXISTS chk_socio_matricula;

-- Socio: plan vigente; cuota: plan historico al generar el cobro.
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

-- Pendiente: introducir planes BASICO y PREMIUM con sus precios reales;
-- asociar el plan a cada socio, y guardar el plan al generar cada cuota.
-- No borrar plan_socio hasta revisar si contiene informacion necesaria.
