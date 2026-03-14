-- ============================================================
-- PROYECTO: Mesa de Ayuda - Poder Judicial Provincial
-- ARCHIVO:  mesa_de_ayuda_ddl.sql
-- FASE:     1 - Diseño del modelo de datos
-- FECHA:    2025-02-25
-- VERSIÓN:  1.1 — Soft-delete implementado en todas las tablas
-- ============================================================
-- DECISIÓN DE DISEÑO: Soft-Delete Global
-- ─────────────────────────────────────────────────────────────
-- Ningún registro se elimina físicamente de la base de datos.
-- Toda "eliminación" es una actualización lógica:
--   eliminado         TINYINT(1)  DEFAULT 0  → 1 al "eliminar"
--   fecha_eliminacion DATETIME    DEFAULT NULL → timestamp del momento
--   eliminado_por_id  INT         DEFAULT NULL → FK al usuario que eliminó
--
-- BENEFICIOS:
--   • Trazabilidad completa: el audit_log siempre puede referenciar
--     el registro original aunque esté "eliminado".
--   • Recuperación de datos: posible restaurar cualquier registro.
--   • Historial íntegro: no se rompen relaciones FK existentes.
--
-- IMPACTO EN CONSULTAS:
--   Todas las consultas deben incluir: WHERE eliminado = 0
--   En Spring Boot esto se implementa con @Where(clause = "eliminado = 0")
--   en cada @Entity, de modo que JPA lo aplica automáticamente.
-- ============================================================

CREATE DATABASE IF NOT EXISTS mesa_de_ayuda CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mesa_de_ayuda;

-- ------------------------------------------------------------
-- 1. ROLES
--    Tabla base. Sin dependencias.
--    Soft-delete: permite conservar roles históricos referenciados
--    en audit_log aunque ya no estén activos en el sistema.
-- ------------------------------------------------------------
CREATE TABLE roles (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL COMMENT 'Admin | Operario | Técnico',
    descripcion VARCHAR(255) NULL,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_nombre (nombre)
) ENGINE = InnoDB COMMENT = 'Roles del sistema';

-- ------------------------------------------------------------
-- 2. USUARIOS
--    Depende de: roles
--    NOTA: ya tenía campo "activo" para desactivación operativa.
--    Se agrega soft-delete independiente: un usuario puede estar
--    inactivo (activo=0) sin estar eliminado, o eliminado lógicamente
--    conservando su historial en tickets y audit_log.
-- ------------------------------------------------------------
CREATE TABLE usuarios (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT 'Hash BCrypt',
    telefono VARCHAR(30) NULL,
    rol_id INT NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Desactivación operativa (bloqueo de acceso)',
    fecha_alta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuarios_email (email),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB COMMENT = 'Usuarios del sistema';

-- ------------------------------------------------------------
-- 3. CIRCUNSCRIPCIONES
--    Tabla base de la jerarquía territorial.
-- ------------------------------------------------------------
CREATE TABLE circunscripciones (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL COMMENT 'Ej: 1ra, 2da, 3ra',
    distrito_judicial VARCHAR(100) NOT NULL COMMENT 'Ej: Santa Fe, Rosario',
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = 'Circunscripciones judiciales territoriales';

-- ------------------------------------------------------------
-- 4. JUZGADOS
--    Depende de: circunscripciones
-- ------------------------------------------------------------
CREATE TABLE juzgados (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    fuero VARCHAR(100) NOT NULL COMMENT 'Ej: Civil, Penal, Familia, Laboral',
    ciudad VARCHAR(100) NOT NULL,
    edificio VARCHAR(150) NULL,
    circunscripcion_id INT NOT NULL,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_juzgados_circunscripcion FOREIGN KEY (circunscripcion_id) REFERENCES circunscripciones (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB COMMENT = 'Juzgados: unidad operativa final de la estructura territorial';

-- ------------------------------------------------------------
-- 5. CONTRATOS
--    Módulo independiente, sin dependencias.
-- ------------------------------------------------------------
CREATE TABLE contratos (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    proveedor VARCHAR(150) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    cobertura VARCHAR(255) NULL COMMENT 'Descripción del tipo de cobertura',
    monto DECIMAL(15, 2) NULL,
    dias_alerta_vencimiento INT NOT NULL DEFAULT 30 COMMENT 'Días antes del vencimiento para emitir alerta',
    observaciones TEXT NULL,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    INDEX idx_contratos_fecha_fin (fecha_fin),
    INDEX idx_contratos_eliminado (eliminado)
) ENGINE = InnoDB COMMENT = 'Contratos institucionales con proveedores';

-- ------------------------------------------------------------
-- 6. HARDWARE
--    Depende de: juzgados, contratos (nullable)
-- ------------------------------------------------------------
CREATE TABLE hardware (
    id INT NOT NULL AUTO_INCREMENT,
    nro_inventario VARCHAR(50) NOT NULL COMMENT 'Ej: HW-2025-0001',
    clase VARCHAR(100) NOT NULL COMMENT 'Ej: PC Desktop, Impresora, Scanner, Monitor',
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(150) NOT NULL,
    nro_serie VARCHAR(100) NULL,
    ubicacion_fisica VARCHAR(200) NOT NULL COMMENT 'Ej: Piso 2, Oficina 203',
    juzgado_id INT NOT NULL,
    contrato_id INT NULL COMMENT 'Asociación opcional a contrato',
    fecha_alta DATE NOT NULL DEFAULT(CURRENT_DATE),
    observaciones TEXT NULL,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hardware_nro_inventario (nro_inventario),
    INDEX idx_hardware_juzgado (juzgado_id),
    INDEX idx_hardware_contrato (contrato_id),
    INDEX idx_hardware_eliminado (eliminado),
    CONSTRAINT fk_hardware_juzgado FOREIGN KEY (juzgado_id) REFERENCES juzgados (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_hardware_contrato FOREIGN KEY (contrato_id) REFERENCES contratos (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB COMMENT = 'Inventario de activos de hardware';

-- ------------------------------------------------------------
-- 7. SOFTWARE
--    Depende de: contratos (NOT NULL - obligatorio)
--    v1.3 — Se elimina hardware_id (reemplazado por pivot software_hardware)
--           Se elimina juzgado_id (reemplazado por pivot software_juzgado)
-- ------------------------------------------------------------
CREATE TABLE software (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    proveedor VARCHAR(150) NOT NULL,
    cantidad_licencias INT NOT NULL DEFAULT 1,
    licencias_en_uso INT NOT NULL DEFAULT 0 COMMENT 'Solo cuenta hardware activo vinculado en software_hardware',
    fecha_vencimiento DATE NULL,
    contrato_id INT NOT NULL COMMENT 'Obligatorio: todo software debe tener contrato',
    observaciones TEXT NULL,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    INDEX idx_software_contrato (contrato_id),
    INDEX idx_software_eliminado (eliminado),
    CONSTRAINT fk_software_contrato FOREIGN KEY (contrato_id) REFERENCES contratos (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_software_licencias CHECK (
        licencias_en_uso <= cantidad_licencias
    )
) ENGINE = InnoDB COMMENT = 'Inventario de licencias de software (siempre vinculado a contrato)';

-- ------------------------------------------------------------
-- 7b. SOFTWARE_HARDWARE  (many-to-many)
--     Una licencia puede estar instalada en múltiples equipos.
--     licencias_en_uso se incrementa/decrementa por cada vínculo.
-- ------------------------------------------------------------
CREATE TABLE software_hardware (
    id INT NOT NULL AUTO_INCREMENT,
    software_id INT NOT NULL,
    hardware_id INT NOT NULL,
    fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_software_hardware (software_id, hardware_id),
    INDEX idx_sh_software (software_id),
    INDEX idx_sh_hardware (hardware_id),
    INDEX idx_sh_eliminado (eliminado),
    CONSTRAINT fk_sh_software FOREIGN KEY (software_id) REFERENCES software (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_sh_hardware FOREIGN KEY (hardware_id) REFERENCES hardware (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB COMMENT = 'Relación many-to-many software-hardware con trazabilidad';

-- ------------------------------------------------------------
-- 7c. SOFTWARE_JUZGADO  (many-to-many)
--     Clasificación territorial. NO afecta licencias_en_uso.
--     Un software puede cubrir múltiples juzgados.
-- ------------------------------------------------------------
CREATE TABLE software_juzgado (
    id INT NOT NULL AUTO_INCREMENT,
    software_id INT NOT NULL,
    juzgado_id INT NOT NULL,
    fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_software_juzgado (software_id, juzgado_id),
    INDEX idx_sj_software (software_id),
    INDEX idx_sj_juzgado (juzgado_id),
    INDEX idx_sj_eliminado (eliminado),
    CONSTRAINT fk_sj_software FOREIGN KEY (software_id) REFERENCES software (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_sj_juzgado FOREIGN KEY (juzgado_id) REFERENCES juzgados (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB COMMENT = 'Relación many-to-many software-juzgado (territorial, no consume licencia)';

-- ------------------------------------------------------------
-- 8. CONTRATO_HARDWARE  (many-to-many)
--    NOTA: las tablas pivot NO tienen soft-delete propio.
--    La desasociación es una operación legítima y reversible;
--    se registra en audit_log. Si el contrato o hardware son
--    soft-deleted, la relación queda inactiva de facto.
-- ------------------------------------------------------------
CREATE TABLE contrato_hardware (
    contrato_id INT NOT NULL,
    hardware_id INT NOT NULL,
    PRIMARY KEY (contrato_id, hardware_id),
    CONSTRAINT fk_ch_contrato FOREIGN KEY (contrato_id) REFERENCES contratos (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ch_hardware FOREIGN KEY (hardware_id) REFERENCES hardware (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = 'Relación many-to-many entre contratos y hardware';

-- ------------------------------------------------------------
-- 9. CONTRATO_SOFTWARE  (many-to-many)
--    (misma lógica que contrato_hardware)
-- ------------------------------------------------------------
CREATE TABLE contrato_software (
    contrato_id INT NOT NULL,
    software_id INT NOT NULL,
    PRIMARY KEY (contrato_id, software_id),
    CONSTRAINT fk_cs_contrato FOREIGN KEY (contrato_id) REFERENCES contratos (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_cs_software FOREIGN KEY (software_id) REFERENCES software (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = 'Relación many-to-many entre contratos y software';

-- ------------------------------------------------------------
-- 10. TICKETS
--     Depende de: juzgados, usuarios
-- ------------------------------------------------------------
CREATE TABLE tickets (
    id INT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    prioridad ENUM(
        'BAJA',
        'MEDIA',
        'ALTA',
        'CRITICA'
    ) NOT NULL DEFAULT 'MEDIA',
    estado ENUM(
        'SOLICITADO',
        'ASIGNADO',
        'EN_CURSO',
        'CERRADO'
    ) NOT NULL DEFAULT 'SOLICITADO',
    tipo_requerimiento VARCHAR(100) NULL COMMENT 'Ej: Hardware, Software, Red, Otro',
    juzgado_id INT NOT NULL,
    tecnico_id INT NULL COMMENT 'Técnico asignado manualmente',
    hardware_id INT NULL COMMENT 'Equipo relacionado (opcional)',
    referente_nombre VARCHAR(150) NULL COMMENT 'Nombre del referente en el juzgado',
    referente_telefono VARCHAR(30) NULL,
    creado_por_id INT NOT NULL COMMENT 'Usuario Operario/Admin que creó el ticket',
    resolucion TEXT NULL COMMENT 'Descripción de la resolución al cerrar',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL,
    fecha_asignacion DATETIME NULL,
    fecha_cierre DATETIME NULL,
    -- ── Soft-delete ──────────────────────────────────────────
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_eliminacion DATETIME NULL,
    eliminado_por_id INT NULL,
    PRIMARY KEY (id),
    INDEX idx_tickets_estado (estado),
    INDEX idx_tickets_prioridad (prioridad),
    INDEX idx_tickets_juzgado (juzgado_id),
    INDEX idx_tickets_tecnico (tecnico_id),
    INDEX idx_tickets_creado_por (creado_por_id),
    INDEX idx_tickets_eliminado (eliminado),
    CONSTRAINT fk_tickets_juzgado FOREIGN KEY (juzgado_id) REFERENCES juzgados (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_tickets_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_hardware FOREIGN KEY (hardware_id) REFERENCES hardware (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB COMMENT = 'Tickets de soporte técnico';

-- ------------------------------------------------------------
-- 11. AUDIT_LOG
--     NOTA: audit_log NO tiene soft-delete. Es el registro
--     inmutable de todas las acciones del sistema, incluyendo
--     los propios soft-deletes de otras tablas. No debe
--     modificarse ni eliminarse bajo ninguna circunstancia.
-- ------------------------------------------------------------
CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id INT NULL COMMENT 'NULL si la acción es del sistema',
    entidad VARCHAR(100) NOT NULL COMMENT 'Ticket | Hardware | Software | Contrato | Usuario | Juzgado',
    accion VARCHAR(50) NOT NULL COMMENT 'CREATE | UPDATE | DELETE | RESTORE | ASSIGN | CLOSE',
    registro_id INT NOT NULL COMMENT 'ID del registro afectado',
    valor_anterior JSON NULL COMMENT 'Estado previo (JSON)',
    valor_nuevo JSON NULL COMMENT 'Nuevo estado (JSON)',
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_entidad_registro (entidad, registro_id),
    INDEX idx_audit_usuario (usuario_id),
    INDEX idx_audit_fecha (fecha),
    CONSTRAINT fk_audit_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB COMMENT = 'Log de auditoría inmutable. Registra todos los cambios incluyendo soft-deletes.';

-- ============================================================
-- DATOS INICIALES (SEED COMPLETO)
-- Compatible con mesa_de_ayuda_ddl.sql
-- ============================================================

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO
    roles (nombre, descripcion)
VALUES (
        'Admin',
        'Administrador del sistema. CRUD completo en todos los módulos.'
    ),
    (
        'Operario',
        'Gestiona tickets e inventario. Asigna técnicos manualmente.'
    ),
    (
        'Técnico',
        'Solo lectura de sus tickets asignados. Trabaja fuera del sistema.'
    );

-- ============================================================
-- USUARIOS
-- NOTA:
-- Estos hashes deben corresponder a contraseñas conocidas de prueba.
-- Si querés, después te genero un bloque con contraseñas exactas
-- para:
--   admin@judicial.gob.ar     -> Admin123!
--   operario@judicial.gob.ar  -> Operario123!
--   tecnico@judicial.gob.ar   -> Tecnico123!
--   tecnico2@judicial.gob.ar  -> Tecnico123!
--   tecnico3@judicial.gob.ar  -> Tecnico123!
--   tecnico.inactivo@judicial.gob.ar -> Tecnico123!
-- ============================================================
INSERT INTO
    usuarios (
        nombre,
        apellido,
        email,
        password,
        telefono,
        rol_id,
        activo
    )
VALUES (
        'Admin',
        'Sistema',
        'admin@judicial.gob.ar',
        '$2a$10$5SDtL55JolXAXt014Qi9oOou5nd./x5i.3.gI8VTh1TmZfkANsfGG',
        '3404000000',
        1,
        1
    ),
    (
        'Carlos',
        'García',
        'operario@judicial.gob.ar',
        '$2a$10$tqT8W/Mfd4WE3JJzecNC2OGqBB4seFjTA1qssFdZGpwhaycGHjOea',
        '3404000001',
        2,
        1
    ),
    (
        'Laura',
        'Martínez',
        'tecnico@judicial.gob.ar',
        '$2a$10$pwJDHYpDVoqnjy.KjYKU..kXn2W3EM1/shaxSO1sEJckWVGupwizq',
        '3404000002',
        3,
        1
    ),
    (
        'Marcos',
        'López',
        'tecnico2@judicial.gob.ar',
        '$2a$10$DVF7tH43a2tC.L4.ClFM0eu30el/vqFJ2YKZW8/5Zjc6zC/wS96gS',
        '3404000003',
        3,
        1
    ),
    (
        'Julia',
        'Romero',
        'tecnico3@judicial.gob.ar',
        '$2a$10$hFVMZ/IOeeBHQt5mJHe23eHpwToEMiAr1XhSyWFwOa4x6gI6GllrK',
        '3404000004',
        3,
        1
    ),
    (
        'Pedro',
        'Inactivo',
        'tecnico.inactivo@judicial.gob.ar',
        '$2a$10$v6cYoGpV965g3NmZvfDx6.OU/BnKcZd7oMSsW/Qnm9Rzb0HDzkude',
        '3404000005',
        3,
        0
    );

-- ============================================================
-- CIRCUNSCRIPCIONES
-- ============================================================
INSERT INTO
    circunscripciones (nombre, distrito_judicial)
VALUES (
        '1ra Circunscripción',
        'Santa Fe'
    ),
    (
        '2da Circunscripción',
        'Rosario'
    ),
    (
        '3ra Circunscripción',
        'Reconquista'
    );

-- ============================================================
-- JUZGADOS
-- ============================================================
INSERT INTO
    juzgados (
        nombre,
        fuero,
        ciudad,
        edificio,
        circunscripcion_id
    )
VALUES (
        'Juzgado Civil y Comercial N° 1',
        'Civil y Comercial',
        'Santa Fe',
        'Tribunales I',
        1
    ),
    (
        'Juzgado Laboral N° 2',
        'Laboral',
        'Santa Fe',
        'Tribunales II',
        1
    ),
    (
        'Juzgado Penal N° 3',
        'Penal',
        'Rosario',
        'Edificio Central',
        2
    ),
    (
        'Juzgado de Familia N° 1',
        'Familia',
        'Rosario',
        'Anexo Norte',
        2
    ),
    (
        'Juzgado Civil N° 1',
        'Civil',
        'Reconquista',
        'Sede Reconquista',
        3
    );

-- ============================================================
-- CONTRATOS
-- Casos útiles:
-- - vigente largo plazo
-- - próximo a vencer
-- - vencido
-- - otro vigente sin uso
-- ============================================================
INSERT INTO
    contratos (
        nombre,
        proveedor,
        fecha_inicio,
        fecha_fin,
        cobertura,
        monto,
        dias_alerta_vencimiento,
        observaciones
    )
VALUES (
        'Contrato Soporte Dell 2026',
        'Dell Argentina',
        '2026-01-01',
        '2026-12-31',
        'Soporte y reemplazo de hardware Dell',
        1500000.00,
        30,
        'Cobertura para PCs, notebooks y servidores Dell'
    ),
    (
        'Licenciamiento Microsoft 365',
        'Microsoft',
        '2026-01-15',
        '2026-07-15',
        'Licencias de productividad y correo',
        2400000.00,
        45,
        'Contrato útil para pruebas de próximos a vencer'
    ),
    (
        'Antivirus Corporativo ESET',
        'ESET',
        '2025-01-01',
        '2025-12-31',
        'Licencias antivirus institucionales',
        780000.00,
        20,
        'Contrato vencido para pruebas'
    ),
    (
        'Contrato Impresoras Brother',
        'Brother',
        '2026-02-01',
        '2026-11-30',
        'Soporte de impresoras',
        650000.00,
        30,
        'Contrato vigente adicional'
    );

-- ============================================================
-- HARDWARE
-- Casos útiles:
-- - con contrato
-- - sin contrato
-- - varios juzgados
-- ============================================================
INSERT INTO
    hardware (
        nro_inventario,
        clase,
        marca,
        modelo,
        nro_serie,
        ubicacion_fisica,
        juzgado_id,
        contrato_id,
        observaciones
    )
VALUES (
        'INV-PC-0001',
        'PC',
        'Dell',
        'OptiPlex 7090',
        'SN-DEL-0001',
        'Mesa de entradas',
        1,
        1,
        'Equipo administrativo principal'
    ),
    (
        'INV-NB-0002',
        'Notebook',
        'HP',
        'ProBook 440 G8',
        'SN-HP-0002',
        'Secretaría',
        1,
        NULL,
        'Notebook sin contrato asociado'
    ),
    (
        'INV-IMP-0003',
        'Impresora',
        'Brother',
        'HL-L5100DN',
        'SN-BRO-0003',
        'Oficina técnica',
        2,
        4,
        'Impresora de red'
    ),
    (
        'INV-SRV-0004',
        'Servidor',
        'Dell',
        'PowerEdge T150',
        'SN-DEL-0004',
        'Sala de servidores',
        3,
        1,
        'Servidor de infraestructura'
    ),
    (
        'INV-MON-0005',
        'Monitor',
        'Samsung',
        'F24T35',
        'SN-SAM-0005',
        'Despacho principal',
        4,
        NULL,
        'Monitor adicional'
    );

-- ============================================================
-- SOFTWARE
-- Casos útiles:
-- - con juzgado
-- - con hardware
-- - licencias_en_uso > 0 para pruebas
-- - vencimiento cercano
-- - vencido
-- ============================================================
INSERT INTO
    software (
        nombre,
        proveedor,
        cantidad_licencias,
        licencias_en_uso,
        fecha_vencimiento,
        contrato_id,
        observaciones
    )
VALUES (
        'Microsoft 365 Business',
        'Microsoft',
        50,
        0,
        '2026-07-15',
        2,
        'Licencias generales del juzgado'
    ),
    (
        'ESET Endpoint Antivirus',
        'ESET',
        100,
        0,
        '2025-12-31',
        3,
        'Antivirus institucional'
    ),
    (
        'Adobe Acrobat Pro',
        'Adobe',
        10,
        0,
        '2026-06-20',
        2,
        'Licencias asignadas al juzgado laboral'
    ),
    (
        'Windows 11 Pro OEM',
        'Microsoft',
        1,
        1,
        '2027-01-01',
        1,
        'Licencia asociada a hardware específico'
    ),
    (
        'Brother Control Center',
        'Brother',
        15,
        1,
        '2026-11-30',
        4,
        'Software de impresora'
    );

-- ============================================================

INSERT INTO
    software_hardware (software_id, hardware_id)
VALUES (4, 1),
    (5, 3);

INSERT INTO
    software_juzgado (software_id, juzgado_id)
VALUES (1, 1),
    (3, 2);
-- RELACIONES MANY-TO-MANY
-- Solo si pensás usarlas realmente desde backend.
-- Si tu lógica actual usa principalmente contrato_id directo
-- en hardware/software, estas filas igual pueden quedar.
-- ============================================================
INSERT INTO
    contrato_hardware (contrato_id, hardware_id)
VALUES (1, 1),
    (1, 4),
    (4, 3);

INSERT INTO
    contrato_software (contrato_id, software_id)
VALUES (2, 1),
    (2, 3),
    (1, 4),
    (4, 5),
    (3, 2);

-- ============================================================
-- TICKETS
-- Casos útiles:
-- - SOLICITADO
-- - ASIGNADO
-- - EN_CURSO
-- - CERRADO
-- - tickets para distintos técnicos
-- - ticket con hardware asociado
-- ============================================================
INSERT INTO
    tickets (
        titulo,
        descripcion,
        prioridad,
        estado,
        tipo_requerimiento,
        juzgado_id,
        tecnico_id,
        hardware_id,
        referente_nombre,
        referente_telefono,
        creado_por_id,
        resolucion,
        fecha_creacion,
        fecha_asignacion,
        fecha_cierre
    )
VALUES (
        'PC no enciende en mesa de entradas',
        'El equipo principal no enciende desde primera hora.',
        'ALTA',
        'SOLICITADO',
        'Hardware',
        1,
        NULL,
        NULL, -- ← este NULL faltaba (hardware_id)
        'Ana Pérez',
        '3425551001',
        2,
        NULL,
        NOW(),
        NULL,
        NULL
    ),
    (
        'Impresora no responde',
        'La impresora de red dejó de imprimir documentos.',
        'MEDIA',
        'ASIGNADO',
        'Hardware',
        2,
        3,
        3,
        'Luis Gómez',
        '3415552002',
        2,
        NULL,
        NOW(),
        NOW(),
        NULL
    ),
    (
        'Error al iniciar Microsoft 365',
        'Usuarios reportan error de autenticación en Office.',
        'ALTA',
        'EN_CURSO',
        'Software',
        1,
        3,
        NULL,
        'Carla Ruiz',
        '3425553003',
        2,
        NULL,
        NOW(),
        NOW(),
        NULL
    ),
    (
        'Actualizar antivirus',
        'Se requiere revisión de equipos con antivirus vencido.',
        'MEDIA',
        'CERRADO',
        'Software',
        3,
        4,
        NULL,
        'Miguel Sosa',
        '3482554004',
        2,
        'Se actualizaron firmas y licencia del antivirus.',
        NOW(),
        NOW(),
        NOW()
    ),
    (
        'Notebook con teclado fallando',
        'Varias teclas dejaron de responder.',
        'BAJA',
        'ASIGNADO',
        'Hardware',
        1,
        4,
        2,
        'Sofía Díaz',
        '3425555005',
        2,
        NULL,
        NOW(),
        NOW(),
        NULL
    ),
    (
        'Instalación de Acrobat',
        'Se solicita instalación en puesto nuevo.',
        'MEDIA',
        'SOLICITADO',
        'Software',
        2,
        NULL,
        NULL,
        'Daniel Acosta',
        '3415556006',
        1,
        NULL,
        NOW(),
        NULL,
        NULL
    );

-- ============================================================
-- AUDIT_LOG
-- No es obligatorio sembrarlo, pero podés dejar algunos ejemplos
-- para pruebas visuales si querés.
-- ============================================================
INSERT INTO
    audit_log (
        usuario_id,
        entidad,
        accion,
        registro_id,
        valor_anterior,
        valor_nuevo
    )
VALUES (
        1,
        'Usuario',
        'CREATE',
        2,
        NULL,
        JSON_OBJECT(
            'email',
            'operario@judicial.gob.ar',
            'rol',
            'Operario'
        )
    ),
    (
        1,
        'Ticket',
        'CREATE',
        1,
        NULL,
        JSON_OBJECT(
            'titulo',
            'PC no enciende en mesa de entradas',
            'estado',
            'SOLICITADO'
        )
    ),
    (
        2,
        'Ticket',
        'ASSIGN',
        2,
        JSON_OBJECT(
            'tecnico_id',
            NULL,
            'estado',
            'SOLICITADO'
        ),
        JSON_OBJECT(
            'tecnico_id',
            3,
            'estado',
            'ASIGNADO'
        )
    );
-- ============================================================
-- REFERENCIA RÁPIDA — SOFT-DELETE EN SPRING BOOT (FASE 2)
-- ============================================================
-- En cada @Entity agregar los tres campos:
--
--   @Column(name = "eliminado")
--   private boolean eliminado = false;
--
--   @Column(name = "fecha_eliminacion")
--   private LocalDateTime fechaEliminacion;
--
--   @ManyToOne
--   @JoinColumn(name = "eliminado_por_id")
--   private Usuario eliminadoPor;
--
-- Anotación a nivel clase para filtro automático en JPA:
--   @Where(clause = "eliminado = 0")
--
-- Método eliminar() en el Service:
--   entity.setEliminado(true);
--   entity.setFechaEliminacion(LocalDateTime.now());
--   entity.setEliminadoPor(usuarioActual);
--   repository.save(entity);
--   auditService.log("DELETE", entity, usuarioActual);
--
-- Acción RESTORE en audit_log para registrar restauraciones:
--   entity.setEliminado(false);
--   entity.setFechaEliminacion(null);
--   entity.setEliminadoPor(null);
--   repository.save(entity);
--   auditService.log("RESTORE", entity, usuarioActual);
-- ============================================================
-- FIN DEL SCRIPT DDL — v1.1
-- ============================================================