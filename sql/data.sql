-- FITZONE | Datos de prueba del backend SafaReyes.
-- Requisitos: Proyecto_Backend_SafaReyes.pdf, caso 2 (FitZone).
-- Destino: base de datos postgres, esquema gimnasio_test.
-- ANTES: ejecutar sql/01_ajustes_fitzone.sql sobre un esquema sin datos.
-- EJECUTAR UNA SOLA VEZ y sobre TABLAS VACIAS. No usa TRUNCATE ni DELETE.
-- Datos ficticios; no utilizar estas credenciales ni estos importes en produccion.
-- Las fechas dependen de CURRENT_DATE de PostgreSQL (configurar Europe/Madrid).
-- IMPORTANTE: se ha elaborado segun las entidades JPA y la migracion disponibles
-- en GitHub; aun no se dispone del DDL original completo para validar restricciones.

BEGIN;
SET LOCAL search_path TO gimnasio_test;
SET LOCAL TIME ZONE 'Europe/Madrid';

-- 1. PLANES: solo existen los dos planes definidos en el enunciado.
-- Precios INVENTADOS para las pruebas: BASICO 30 EUR, PREMIUM 50 EUR.
INSERT INTO plan (nombre, tipo, precio_mensual) VALUES
    ('BASICO', 'BASICO', 30.00),
    ('PREMIUM', 'PREMIUM', 50.00);

-- 2. SOCIOS: 20 socios ficticios, con DNIs espanoles validos generados
-- matematicamente, correos de ejemplo y distintos estados y planes.
-- Inactivos: 7, 16 y 19. Con cuota actual pendiente: 2, 7, 8, 15, 16, 19.
-- Matriculas: solo algunos socios; otros tienen NULL.
WITH ejemplos(numero, nombre, apellidos) AS (
    VALUES
       ( 1, 'Ana',       'Lopez Martin'),
       ( 2, 'Carlos',    'Ruiz Garcia'),
       ( 3, 'Lucia',     'Fernandez Diaz'),
       ( 4, 'Javier',    'Moreno Perez'),
       ( 5, 'Marta',     'Sanchez Romero'),
       ( 6, 'Daniel',    'Gomez Vega'),
       ( 7, 'Elena',     'Navarro Torres'),
       ( 8, 'Pablo',     'Ramirez Soto'),
       ( 9, 'Nuria',     'Castro Mendez'),
       (10, 'Sergio',    'Ortega Molina'),
       (11, 'Alba',      'Jimenez Gil'),
       (12, 'Manuel',    'Reyes Alvarez'),
       (13, 'Irene',     'Vargas Cano'),
       (14, 'Adrian',    'Prieto Flores'),
       (15, 'Rocio',     'Herrera Leon'),
       (16, 'David',     'Marin Cruz'),
       (17, 'Paula',     'Blanco Rubio'),
       (18, 'Hugo',      'Santos Vega'),
       (19, 'Claudia',   'Serrano Diaz'),
       (20, 'Alejandro', 'Ramos Silva')
)
INSERT INTO socio (
    dni, nombre, apellidos, email, telf, fecha_alta, activo, matricula,
    id_plan, creado_en, actualizado_en
)
SELECT
    LPAD(e.numero::TEXT, 8, '0') || SUBSTR('TRWAGMYFPDXBNJZSQVHLCKE', e.numero % 23 + 1, 1),
    e.nombre, e.apellidos,
    'socio' || LPAD(e.numero::TEXT, 2, '0') || '@ejemplo.test',
    '600' || LPAD(e.numero::TEXT, 6, '0'),
    CURRENT_DATE - (e.numero * 19),
    e.numero NOT IN (7, 16, 19),
    CASE e.numero
        WHEN 1 THEN '1234BCD'
        WHEN 3 THEN '5678FGH'
        WHEN 6 THEN '9012JKL'
        WHEN 12 THEN '3456MNP'
        WHEN 17 THEN '7890RST'
        ELSE NULL
    END,
    p.id_plan,
    CURRENT_TIMESTAMP - (e.numero * INTERVAL '19 days'),
    CURRENT_TIMESTAMP - (e.numero * INTERVAL '19 days')
FROM ejemplos e
JOIN plan p ON p.nombre = CASE WHEN e.numero <= 10 THEN 'PREMIUM' ELSE 'BASICO' END;

-- 3. MONITORES: personas que dirigen las actividades.
INSERT INTO monitor (nombre, apellidos) VALUES
    ('Beatriz', 'Nunez Soto'),
    ('Antonio', 'Perez Garcia'),
    ('Carmen', 'Lara Ruiz'),
    ('Miguel', 'Serrano Lopez');

-- 4. CUOTAS: 20 socios x 3 meses = 60 cuotas.
-- Las del mes actual pendientes estan marcadas arriba.
-- Los socios 7, 15 y 16 deben dos cuotas ANTERIORES: morosos (RN-04).
-- El socio 15 sigue activo para probar la desactivacion masiva Q4 (debe afectar a 1).
-- El socio 4 era BASICO los dos meses anteriores y ahora es PREMIUM;
-- sus cuotas historicas permanecen asociadas al plan y precio antiguos (RN-09).
WITH meses(desplazamiento) AS (VALUES (-2), (-1), (0)),
     base AS (
         SELECT s.id_socio,
                CAST(SUBSTR(s.email, 6, 2) AS INTEGER) AS numero,
                m.desplazamiento,
                (DATE_TRUNC('month', CURRENT_DATE)::DATE +
                 (m.desplazamiento * INTERVAL '1 month'))::DATE AS fecha_mes,
                CASE WHEN s.email = 'socio04@ejemplo.test' AND m.desplazamiento < 0
                     THEN 'BASICO'
                     ELSE p_actual.nombre
                END AS nombre_plan
         FROM socio s
         JOIN plan p_actual ON p_actual.id_plan = s.id_plan
         CROSS JOIN meses m
     ),
     con_estado AS (
         SELECT b.*,
                CASE
                    WHEN b.numero IN (7,15,16) AND b.desplazamiento IN (-2,-1) THEN 'PENDIENTE'
                    WHEN b.numero IN (2,7,8,15,16,19) AND b.desplazamiento = 0 THEN 'PENDIENTE'
                    ELSE 'PAGADA'
                END AS estado_cuota
         FROM base b
     )
INSERT INTO cuota (id_socio, id_plan, mes, anio, importe, estado, fecha_pago)
SELECT c.id_socio, p.id_plan,
       EXTRACT(MONTH FROM c.fecha_mes)::SMALLINT,
       EXTRACT(YEAR FROM c.fecha_mes)::SMALLINT,
       p.precio_mensual,
       c.estado_cuota,
       CASE WHEN c.estado_cuota = 'PAGADA'
            THEN CASE WHEN c.desplazamiento = 0
                      THEN CURRENT_DATE - 1
                      ELSE c.fecha_mes + 5
                 END
            ELSE NULL
       END
FROM con_estado c
JOIN plan p ON p.nombre = c.nombre_plan;

-- 5. ACTIVIDADES: 16 sesiones, ayer, hoy y dias futuros.
-- La sesion de Spinning de manana a las 18:00 tiene solo DOS plazas.
-- Las plazas ocupadas se calculan a partir de reservas ACTIVA.
WITH sesiones(nombre, dias, hora, duracion, sala, aforo, num_monitor) AS (
    VALUES
       ('Yoga',       -1, TIME '09:00', 60, 'Sala A', 12, 1),
       ('Pilates',     0, TIME '09:00', 55, 'Sala B', 10, 3),
       ('Spinning',    0, TIME '18:00', 45, 'Sala C', 12, 2),
       ('Zumba',       0, TIME '20:00', 50, 'Sala A', 16, 4),
       ('Yoga',        1, TIME '08:00', 60, 'Sala A', 12, 1),
       ('Spinning',    1, TIME '18:00', 45, 'Sala C',  2, 2),
       ('Body Pump',   1, TIME '19:00', 50, 'Sala B', 15, 4),
       ('Pilates',     2, TIME '10:00', 55, 'Sala B', 12, 3),
       ('Yoga',        2, TIME '19:00', 60, 'Sala A', 12, 1),
       ('Spinning',    3, TIME '18:00', 45, 'Sala C', 15, 2),
       ('Zumba',       4, TIME '17:00', 50, 'Sala A', 15, 4),
       ('Body Pump',   6, TIME '09:00', 50, 'Sala B', 15, 4),
       ('Yoga',        7, TIME '19:00', 60, 'Sala A', 12, 1),
       ('Pilates',     9, TIME '18:00', 55, 'Sala B', 12, 3),
       ('Spinning',   10, TIME '09:00', 45, 'Sala C', 15, 2),
       ('Zumba',      12, TIME '19:00', 50, 'Sala A', 15, 4)
), monitores AS (
    SELECT id_monitor, ROW_NUMBER() OVER (ORDER BY id_monitor) AS numero
    FROM monitor
)
INSERT INTO actividad (id_monitor, fecha_hora, nombre, duracion, sala, aforo_max)
SELECT m.id_monitor, CURRENT_DATE + ses.dias + ses.hora,
       ses.nombre, ses.duracion, ses.sala, ses.aforo
FROM sesiones ses
JOIN monitores m ON m.numero = ses.num_monitor;

-- 6. RESERVAS: 20 registros, incluyendo reservas futuras, canceladas,
-- clases ya asistidas, tres reservas futuras y una clase completa.
-- Socios 1 y 3 llenan el Spinning de manana a las 18:00.
WITH ejemplos(numero, nombre, dias, hora, estado) AS (
    VALUES
       ( 1, 'Yoga',       1, TIME '08:00', 'ACTIVA'),
       ( 1, 'Spinning',   1, TIME '18:00', 'ACTIVA'),
       ( 1, 'Yoga',       7, TIME '19:00', 'ACTIVA'),
       ( 3, 'Spinning',   1, TIME '18:00', 'ACTIVA'),
       ( 3, 'Body Pump',  1, TIME '19:00', 'ACTIVA'),
       ( 4, 'Pilates',    2, TIME '10:00', 'ACTIVA'),
       ( 4, 'Spinning',   3, TIME '18:00', 'ACTIVA'),
       ( 4, 'Zumba',      4, TIME '17:00', 'ACTIVA'),
       ( 6, 'Yoga',       1, TIME '08:00', 'ACTIVA'),
       ( 6, 'Pilates',    2, TIME '10:00', 'ACTIVA'),
       ( 6, 'Pilates',    9, TIME '18:00', 'ACTIVA'),
       ( 9, 'Body Pump',  1, TIME '19:00', 'ACTIVA'),
       ( 9, 'Yoga',       2, TIME '19:00', 'ACTIVA'),
       (10, 'Yoga',       1, TIME '08:00', 'ACTIVA'),
       ( 5, 'Yoga',       7, TIME '19:00', 'ACTIVA'),
       ( 1, 'Pilates',    2, TIME '10:00', 'CANCELADA'),
       ( 2, 'Yoga',       1, TIME '08:00', 'CANCELADA'),
       ( 1, 'Yoga',      -1, TIME '09:00', 'ASISTIDA'),
       ( 4, 'Yoga',      -1, TIME '09:00', 'ASISTIDA'),
       ( 9, 'Yoga',      -1, TIME '09:00', 'ASISTIDA')
)
INSERT INTO reserva (id_socio, id_actividad, fecha_reserva, estado)
SELECT s.id_socio, a.id_actividad,
       CURRENT_TIMESTAMP - INTERVAL '2 days',
       e.estado
FROM ejemplos e
JOIN socio s ON s.email = 'socio' || LPAD(e.numero::TEXT, 2, '0') || '@ejemplo.test'
JOIN actividad a ON a.nombre = e.nombre
    AND a.fecha_hora = CURRENT_DATE + e.dias + e.hora;

-- 7. ACCESOS: 14 intentos, permitidos y denegados, con y sin salida.
-- Aforo del dia: 3 entradas PERMITIDO sin salida (socios 1, 3 y 6).
-- Incluye INACTIVO, CUOTA_PENDIENTE y YA_DENTRO.
-- AFORO_COMPLETO necesita 150 entradas abiertas y se probara aparte.
WITH ejemplos(numero, dias, hora_entrada, hora_salida, resultado, motivo) AS (
    VALUES
       ( 1,  0, TIME '08:00', NULL::TIME,   'PERMITIDO', NULL::TEXT),
       ( 3,  0, TIME '08:20', NULL::TIME,   'PERMITIDO', NULL::TEXT),
       ( 6,  0, TIME '09:00', NULL::TIME,   'PERMITIDO', NULL::TEXT),
       ( 6,  0, TIME '09:30', NULL::TIME,   'DENEGADO',  'YA_DENTRO'),
       ( 7,  0, TIME '09:45', NULL::TIME,   'DENEGADO',  'INACTIVO'),
       ( 2,  0, TIME '10:00', NULL::TIME,   'DENEGADO',  'CUOTA_PENDIENTE'),
       ( 8,  0, TIME '10:15', NULL::TIME,   'DENEGADO',  'CUOTA_PENDIENTE'),
       (12,  0, TIME '11:00', TIME '12:30', 'PERMITIDO', NULL::TEXT),
       ( 4,  0, TIME '12:00', TIME '13:00', 'PERMITIDO', NULL::TEXT),
       (10,  0, TIME '13:20', TIME '14:05', 'PERMITIDO', NULL::TEXT),
       (19,  0, TIME '15:10', NULL::TIME,   'DENEGADO',  'INACTIVO'),
       (15,  0, TIME '17:00', NULL::TIME,   'DENEGADO',  'CUOTA_PENDIENTE'),
       ( 4, -1, TIME '18:00', TIME '19:00', 'PERMITIDO', NULL::TEXT),
       (12, -1, TIME '08:00', TIME '09:00', 'PERMITIDO', NULL::TEXT)
)
INSERT INTO acceso (
    id_socio, fecha_hora_entrada, fecha_hora_salida,
    tipo_resultado, motivo_denegacion, ticket_parking
)
SELECT s.id_socio,
       CURRENT_DATE + e.dias + e.hora_entrada,
       CASE WHEN e.hora_salida IS NOT NULL
            THEN CURRENT_DATE + e.dias + e.hora_salida
            ELSE NULL
       END,
       e.resultado, e.motivo, NULL
FROM ejemplos e
JOIN socio s ON s.email = 'socio' || LPAD(e.numero::TEXT, 2, '0') || '@ejemplo.test';

-- 8. USUARIOS: 1 ADMIN, 1 RECEPCION y 10 socios con cuenta propia.
-- Contraseña de pruebas de TODOS los usuarios: Test1234!
-- BCrypt cost 10; NUNCA usar en produccion.
INSERT INTO usuario (id_socio, nombre_usuario, contrasena, rol)
VALUES
    (NULL, 'admin_fitzone',
     '$2y$10$BbodHiUpPYx0/7MOFO2B0.VrqGOuUBpC3kzPNo24UW9sDxfuf06fe', 'ADMIN'),
    (NULL, 'recepcion_fitzone',
     '$2y$10$BbodHiUpPYx0/7MOFO2B0.VrqGOuUBpC3kzPNo24UW9sDxfuf06fe', 'RECEPCION');

INSERT INTO usuario (id_socio, nombre_usuario, contrasena, rol)
SELECT s.id_socio,
       SUBSTR(s.email, 1, POSITION('@' IN s.email) - 1),
       '$2y$10$BbodHiUpPYx0/7MOFO2B0.VrqGOuUBpC3kzPNo24UW9sDxfuf06fe',
       'SOCIO'
FROM socio s
WHERE s.email IN (
    'socio01@ejemplo.test', 'socio02@ejemplo.test',
    'socio03@ejemplo.test', 'socio04@ejemplo.test',
    'socio05@ejemplo.test', 'socio06@ejemplo.test',
    'socio07@ejemplo.test', 'socio08@ejemplo.test',
    'socio09@ejemplo.test', 'socio10@ejemplo.test'
);

COMMIT;

-- VERIFICACIONES (ejecutar por separado, despues del COMMIT):
-- SELECT 'plan' AS tabla, COUNT(*) FROM gimnasio_test.plan
-- UNION ALL SELECT 'socio', COUNT(*) FROM gimnasio_test.socio
-- UNION ALL SELECT 'monitor', COUNT(*) FROM gimnasio_test.monitor
-- UNION ALL SELECT 'cuota', COUNT(*) FROM gimnasio_test.cuota
-- UNION ALL SELECT 'actividad', COUNT(*) FROM gimnasio_test.actividad
-- UNION ALL SELECT 'reserva', COUNT(*) FROM gimnasio_test.reserva
-- UNION ALL SELECT 'acceso', COUNT(*) FROM gimnasio_test.acceso
-- UNION ALL SELECT 'usuario', COUNT(*) FROM gimnasio_test.usuario;
