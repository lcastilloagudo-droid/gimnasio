-- FitZone: restricciones CHECK exigidas por el proyecto.
-- Ejecutar en la base postgres, esquema gimnasio_test, DESPUES de 01_ajustes_fitzone.sql
-- y ANTES de data.sql. Se puede repetir: reemplaza los CHECK indicados.
-- Mantener tambien las validaciones @Valid y reglas de negocio en Java.
-- Los valores de los CHECK concuerdan con los enums y estados utilizados en el backend.

BEGIN;
SET LOCAL search_path TO gimnasio_test;

-- Corrige usuario_rol_check, que aun aceptaba los roles del modelo antiguo.
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_rol_check;
ALTER TABLE usuario ADD CONSTRAINT usuario_rol_check
    CHECK (rol IN ('ADMIN', 'RECEPCION', 'SOCIO'));

-- Precio mensual: positivo y obligatorio (NOT NULL).
ALTER TABLE plan ALTER COLUMN precio_mensual SET NOT NULL;
ALTER TABLE plan DROP CONSTRAINT IF EXISTS chk_plan_precio_mensual;
ALTER TABLE plan ADD CONSTRAINT chk_plan_precio_mensual
    CHECK (precio_mensual > 0);

-- Matrícula espanola opcional: cuatro numeros y tres consonantes.
ALTER TABLE socio DROP CONSTRAINT IF EXISTS chk_socio_matricula;
ALTER TABLE socio ADD CONSTRAINT chk_socio_matricula
    CHECK (matricula IS NULL OR matricula ~ '^[0-9]{4}[BCDFGHJKLMNPRSTVWXYZ]{3}$');

-- Cuotas: mes, ano, importe y estados permitidos.
ALTER TABLE cuota DROP CONSTRAINT IF EXISTS chk_cuota_mes;
ALTER TABLE cuota ADD CONSTRAINT chk_cuota_mes
    CHECK (mes BETWEEN 1 AND 12);

ALTER TABLE cuota DROP CONSTRAINT IF EXISTS chk_cuota_anio;
ALTER TABLE cuota ADD CONSTRAINT chk_cuota_anio
    CHECK (anio BETWEEN 2000 AND 2100);

ALTER TABLE cuota DROP CONSTRAINT IF EXISTS chk_cuota_importe;
ALTER TABLE cuota ADD CONSTRAINT chk_cuota_importe
    CHECK (importe > 0);

ALTER TABLE cuota DROP CONSTRAINT IF EXISTS chk_cuota_estado;
ALTER TABLE cuota ADD CONSTRAINT chk_cuota_estado
    CHECK (estado IN ('PENDIENTE', 'PAGADA'));

-- Reserva: coincide con el enum Estado de Java.
ALTER TABLE reserva DROP CONSTRAINT IF EXISTS chk_reserva_estado;
ALTER TABLE reserva ADD CONSTRAINT chk_reserva_estado
    CHECK (estado IN ('ACTIVA', 'CANCELADA', 'ASISTIDA'));

-- Acceso: resultados y motivos de denegacion definidos por RN-05.
ALTER TABLE acceso DROP CONSTRAINT IF EXISTS chk_acceso_tipo_resultado;
ALTER TABLE acceso ADD CONSTRAINT chk_acceso_tipo_resultado
    CHECK (tipo_resultado IN ('PERMITIDO', 'DENEGADO'));

ALTER TABLE acceso DROP CONSTRAINT IF EXISTS chk_acceso_motivo_denegacion;
ALTER TABLE acceso ADD CONSTRAINT chk_acceso_motivo_denegacion
    CHECK (motivo_denegacion IS NULL OR motivo_denegacion IN
       ('INACTIVO', 'CUOTA_PENDIENTE', 'YA_DENTRO', 'AFORO_COMPLETO'));

-- Actividades: rango minimo valido.
ALTER TABLE actividad DROP CONSTRAINT IF EXISTS chk_actividad_duracion;
ALTER TABLE actividad ADD CONSTRAINT chk_actividad_duracion
    CHECK (duracion > 0);

ALTER TABLE actividad DROP CONSTRAINT IF EXISTS chk_actividad_aforo;
ALTER TABLE actividad ADD CONSTRAINT chk_actividad_aforo
    CHECK (aforo_max > 0);

COMMIT;
