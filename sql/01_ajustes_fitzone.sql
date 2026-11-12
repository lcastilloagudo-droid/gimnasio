-- FitZone: adaptacion incremental de la BD del proyecto.
-- Ejecutar en DBeaver conectado a la BD postgres, schema gimnasio_test.
-- La BD no contiene datos, por lo que no se incluyen comprobaciones defensivas.

BEGIN;
SET LOCAL search_path TO gimnasio_test;

-- El precio mensual forma parte del plan.
ALTER TABLE plan
    ADD COLUMN precio_mensual NUMERIC(10,2);

ALTER TABLE plan
    ADD CONSTRAINT chk_plan_precio_mensual
    CHECK (precio_mensual IS NULL OR precio_mensual >= 0);

-- La matricula estaba almacenada como BOOLEAN.
-- Como una matricula real tiene formato 1234BCD, se convierte a texto.
-- Al no existir datos, no necesitamos conservar ningun valor anterior.
ALTER TABLE socio
    ALTER COLUMN matricula TYPE VARCHAR(7)
    USING NULL::VARCHAR(7);

ALTER TABLE socio
    ADD CONSTRAINT chk_socio_matricula
    CHECK (
        matricula IS NULL
        OR matricula ~ '^[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}$'
    );

-- Plan actual del socio: relacion N:1 entre socio y plan.
ALTER TABLE socio
    ADD COLUMN id_plan INTEGER;

ALTER TABLE socio
    ADD CONSTRAINT fk_socio_plan
    FOREIGN KEY (id_plan)
    REFERENCES plan(id_plan)
    ON DELETE RESTRICT;

-- La cuota conserva el plan con el que fue generada.
ALTER TABLE cuota
    ADD COLUMN id_plan INTEGER;

ALTER TABLE cuota
    ADD CONSTRAINT fk_cuota_plan
    FOREIGN KEY (id_plan)
    REFERENCES plan(id_plan)
    ON DELETE RESTRICT;

-- Campos de auditoria para Socio.
ALTER TABLE socio
    ADD COLUMN creado_en TIMESTAMP;

ALTER TABLE socio
    ADD COLUMN actualizado_en TIMESTAMP;

-- Indices para consultas frecuentes por plan.
CREATE INDEX idx_socio_id_plan ON socio(id_plan);
CREATE INDEX idx_cuota_id_plan ON cuota(id_plan);

COMMIT;

-- DESPUES:
-- 1. Asignar precio_mensual a BASICO y PREMIUM.
-- 2. Asignar id_plan a los socios cuando se creen.
-- 3. Guardar id_plan en cada cuota al generarla.
-- 4. Eliminar plan_socio cuando confirmemos que ya no se necesita.
