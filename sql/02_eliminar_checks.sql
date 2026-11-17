-- FitZone: eliminar restricciones CHECK antiguas del esquema gimnasio_test.
-- No comprueba filas ni aplica reglas de formato o negocio en PostgreSQL.
-- Las claves primarias, foraneas y UNIQUE no se modifican.
-- Ejecutar una vez antes de sql/data.sql. No ejecutar sobre gimnasio.

BEGIN;

DO $$
DECLARE
    restriccion RECORD;
BEGIN
    FOR restriccion IN
        SELECT n.nspname AS esquema, t.relname AS tabla, c.conname AS nombre
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'gimnasio_test'
          AND c.contype = 'c'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            restriccion.esquema, restriccion.tabla, restriccion.nombre
        );
    END LOOP;
END $$;

COMMIT;
