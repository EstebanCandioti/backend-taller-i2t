# Mesa de Ayuda IT — Poder Judicial Provincial

API REST del sistema de gestión de tickets de soporte técnico (Help Desk) para el Poder Judicial Provincial. Desarrollada con Spring Boot 3.5 como parte del Taller de Certificación en Desarrollo Asistido por IA — Campus 2026.

> **Repositorio del frontend:** [Angular — Mesa de Ayuda](https://github.com/EstebanCandioti/frontend-taller-I2T)

---

## Descripción

Backend completo que expone una API REST consumida por una SPA Angular. Implementa autenticación JWT, auditoría automática con AOP, notificaciones en tiempo real via WebSocket, jobs programados de alertas por email, y gestión de inventario IT para el contexto judicial.

---

## Stack Tecnológico

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Spring Boot | 3.5.11 | Framework principal |
| Java | 17 LTS | Lenguaje |
| Maven | 3.9 | Build |
| MySQL | 8.0+ | Base de datos |
| Spring Data JPA + Hibernate | — | ORM |
| Spring Security + JJWT | 0.12.6 | Autenticación JWT (HS256) |
| Spring AOP | — | Auditoría automática |
| Spring WebSocket + STOMP | — | Notificaciones en tiempo real |
| Spring Mail | — | Emails asincrónicos |
| Spring Scheduling | — | Jobs diarios de alertas |
| Lombok | — | Reducción de boilerplate |

---

## Funcionalidades

### API REST — 11 Controllers
- **Auth** — Login con JWT
- **Tickets** — Máquina de estados completa (SOLICITADO → ASIGNADO → EN_CURSO → CERRADO)
- **Hardware** — Inventario de activos físicos con soft-delete
- **Software** — Control de licencias con validación de cupo
- **Contratos** — Gestión con renovación automática de activos vinculados
- **Juzgados y Circunscripciones** — Estructura territorial judicial
- **Usuarios** — CRUD con activación/desactivación
- **Roles** — Lectura de roles del sistema
- **Auditoría** — Log inmutable consultable con filtros
- **Dashboard** — Métricas globales y por técnico

### Características Técnicas
- **JWT stateless** (HS256, 8 horas de expiración) con filtro por request
- **3 roles:** Admin, Operario, Técnico — con matriz de permisos en SecurityConfig
- **Soft-delete universal** con `@SQLRestriction("eliminado = 0")` en todas las entidades
- **Restore** de registros eliminados via native query que bypasea el filtro JPA
- **Auditoría AOP** con `@Auditable` — captura estado anterior/posterior sin ensuciar el código de negocio
- **WebSocket STOMP** con autenticación JWT en handshake — canales privados y broadcast
- **Jobs programados** (08:00 y 08:15) para alertas de vencimiento de contratos y licencias
- **Emails asincrónicos** con `@Async` para no bloquear la respuesta HTTP
- **Soft-delete con restricciones** — validaciones de integridad antes de eliminar
- **Respuesta uniforme** `ApiResponse<T>` en todos los endpoints
- **Mappers manuales estáticos** — nunca se exponen entidades JPA directamente

---

## Arquitectura

```
com.judicial.mesadeayuda/
├── Audit/        # @Auditable (anotación) + AuditAspect (AOP)
├── Config/       # AsyncConfig, WebSocketConfig
├── Controller/   # 11 REST Controllers (sin lógica de negocio)
├── DTO/
│   ├── Request/  # DTOs de entrada con validaciones @Valid
│   └── Response/ # DTOs de salida + ApiResponse<T>
├── Entities/     # 9 entidades JPA + 2 tablas pivot
├── Exceptions/   # NotFoundException, BusinessException, GlobalExceptionHandler
├── Job/          # ContratoVencimientoJob, SoftwareVencimientoJob
├── Mapper/       # Mappers estáticos Entity → ResponseDTO
├── Repositories/ # Spring Data JPA (11 repositorios)
├── Security/     # JWT completo (filter, provider, config, UserDetails, WS interceptor)
└── Service/      # 12 servicios de negocio con @Transactional
```

### Modelo de Base de Datos

```
roles ──────────────── usuarios
circunscripciones ──── juzgados ──── tickets
                           │              ↑
                           ├──── hardware ┘ (opcional)
                           └──── software (via pivot)

contratos (1) ←──── hardware   (FK directa, opcional)
contratos (1) ←──── software   (FK directa, obligatorio)

software (M) ←──→ (N) hardware   (pivot: software_hardware, con soft-delete)
software (M) ←──→ (N) juzgados   (pivot: software_juzgado, con soft-delete)

audit_log ──→ usuarios (nullable — jobs del sistema no tienen usuario)
```

**Soft-delete universal:** `eliminado`, `fecha_eliminacion`, `eliminado_por_id` en todas las tablas excepto `audit_log` y `roles`.

---

## Instalación y Ejecución

### Prerrequisitos
- Java 17 (JDK)
- Maven 3.9+
- MySQL 8.0 corriendo en `localhost:3306`

### Configuración

Copiar `application.properties` y completar las variables de entorno:

```properties
# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/mesa_de_ayuda
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD

# Email (Gmail)
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-app-password
```

### Inicializar la base de datos

```bash
mysql -u root -p < ../../../db/init.sql
```

### Ejecutar

```bash
# Desarrollo
mvn spring-boot:run

# Build JAR
mvn clean package -DskipTests
```

El backend estará disponible en `http://localhost:8080`

### Con Docker (recomendado)

La forma más simple es levantar el stack completo con Docker Compose. Ver las instrucciones en el [repositorio del frontend](https://github.com/EstebanCandioti/frontend-taller-I2T).

---

## Endpoints Principales

**Base URL:** `http://localhost:8080/api`

| Módulo | Endpoints |
|--------|-----------|
| Auth | `POST /auth/login` |
| Tickets | `GET/POST /tickets`, `PUT /tickets/:id/asignar`, `PUT /tickets/:id/cerrar` |
| Hardware | `GET/POST/PUT/DELETE /hardware` |
| Software | `GET/POST/PUT/DELETE /software` |
| Contratos | `GET/POST/PUT/DELETE /contratos`, `POST /contratos/:id/renovar` |
| Juzgados | `GET/POST/PUT/DELETE /juzgados` |
| Usuarios | `GET/POST/PUT/DELETE /usuarios`, `PUT /usuarios/:id/activar` |
| Auditoría | `GET /audit/filtro` |
| Dashboard | `GET /dashboard` |

Todas las respuestas usan el wrapper:
```json
{
  "success": true,
  "message": "...",
  "data": {},
  "errors": null,
  "timestamp": "2026-03-24T10:00:00"
}
```

---

## Usuarios de Prueba

| Rol | Email | Password |
|-----|-------|----------|
| Admin | admin@judicial.gob.ar | Admin123! |
| Operario | operario@judicial.gob.ar | Operario123! |
| Técnico | tecnico@judicial.gob.ar | Tecnico123! |

---

## Contexto del Proyecto

Este sistema fue desarrollado como proyecto integrador del **Taller de Certificación en Desarrollo Asistido por IA — Campus 2026**, abarcando 3 fases:

| Fase | Descripción |
|------|-------------|
| Fase 1 | Base de datos MySQL normalizada con soft-delete universal |
| Fase 2 | Este repositorio — API REST completa |
| Fase 3 | SPA Angular 21 |

---

## Autor

**Esteban Candioti**
