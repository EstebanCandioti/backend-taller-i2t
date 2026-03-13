# CLAUDE.md — Mesa de Ayuda IT | Poder Judicial Provincial

## 1. Descripción del Proyecto

Sistema web de gestión de tickets de soporte técnico, inventario tecnológico (hardware y software), contratos, organización territorial de juzgados y administración de usuarios con trazabilidad completa.

**Stack:** Spring Boot 3.5.x / Java 17 / MySQL 8.0+ / Maven
**Fase actual:** Fase 2 (Backend completo) — Fase 3 será el Frontend en Angular.

## 2. Estructura de Paquetes

```
src/main/java/com/judicial/mesadeayuda/
├── Audit/              → AuditAspect, @Auditable (anotación custom)
├── Config/             → AsyncConfig (@EnableAsync + @EnableScheduling)
├── Controller/         → 10 REST controllers + AuthController
├── DTO/
│   ├── Request/        → DTOs de entrada (@Valid)
│   └── Response/       → DTOs de salida + ApiResponse<T>
├── Entities/           → 9 entidades JPA con Lombok
├── Exceptions/         → NotFoundException, BusinessException, GlobalExceptionHandler
├── Job/                → ContratoVencimientoJob, SoftwareVencimientoJob (@Scheduled)
├── Mapper/             → Mappers manuales estáticos (Entity → ResponseDTO)
├── Repositories/       → Spring Data JPA interfaces
├── Security/           → JWT (filter, provider, config, UserDetails)
└── Service/            → 10 services + EmailService
```

**IMPORTANTE:** Todos los paquetes usan PascalCase (Entities, Exceptions, Service, etc). Los imports deben coincidir exactamente.

## 3. Entidades y Relaciones

```
Circunscripcion (1) ──→ (N) Juzgado (1) ──→ (N) Ticket
                                    │              ↑
                                    └──→ Hardware ──┘ (opcional)
                                    └──→ Software

Contrato (M) ←──→ (N) Hardware   (contrato_hardware)
Contrato (M) ←──→ (N) Software   (contrato_software)
Contrato (1) ←── (N) Software    (software.contrato_id obligatorio)

Usuario ──→ Rol (Admin | Operario | Técnico)
Ticket.tecnico_id ──→ Usuario (técnico asignado)
Ticket.creado_por_id ──→ Usuario (quien creó el ticket)

AuditLog ──→ Usuario (quien realizó la acción, null si es job del sistema)
```

## 4. Reglas de Negocio Críticas

### 4.1 Tickets — Flujo de Estados
```
SOLICITADO → ASIGNADO → EN_CURSO → CERRADO
```
- **Crear:** siempre en SOLICITADO. Juzgado obligatorio, hardware opcional.
- **Asignar** (PUT /{id}/asignar): solo desde SOLICITADO. Técnico debe existir, estar activo y tener rol "Técnico". Envía email al técnico.
- **Reasignar** (PUT /{id}/reasignar): solo desde ASIGNADO o EN_CURSO. Nuevo técnico diferente al actual. Envía email al nuevo.
- **Pasar a EN_CURSO** (PUT /{id}/estado): solo desde ASIGNADO.
- **Cerrar** (PUT /{id}/cerrar): solo desde ASIGNADO o EN_CURSO. Resolución obligatoria.
- **Soft-delete** (DELETE /{id}): solo en SOLICITADO y sin técnico.
- **Editar** (PUT /{id}): no permitido si está CERRADO.

### 4.2 Software — Control de Licencias
- Contrato es OBLIGATORIO al crear software.
- `licenciasEnUso <= cantidadLicencias` siempre.
- Al editar, si se asigna hardware/juzgado donde antes no había → `licenciasEnUso + 1`.
- Si se quita asignación → `licenciasEnUso - 1` (mínimo 0).
- Si cambia de una asignación a otra → no cambia.

### 4.3 Contratos — Renovación
- `POST /{id}/renovar`: crea contrato nuevo copiando proveedor, cobertura, diasAlertaVencimiento del original.
- Sobreescribe fechas, monto, observaciones con el DTO.
- Reasigna automáticamente hardware y software del original al nuevo.
- `fechaInicio` de renovación no puede ser anterior a `fechaFin` del original.

### 4.4 Soft-Delete y Restore
Todas las entidades tienen soft-delete con campos: `eliminado`, `fechaEliminacion`, `eliminadoPor`.
- `@SQLRestriction("eliminado = 0")` filtra automáticamente registros eliminados.
- Para restore se usa `findEliminadoById()` con `nativeQuery = true` que bypasea el filtro.
- Endpoint: `PUT /api/{entidad}/{id}/restore`
- Restricciones antes de eliminar:
  - **Hardware:** no si tiene tickets activos.
  - **Juzgado:** no si tiene hardware, software o tickets activos.
  - **Contrato:** no si tiene software activo vinculado.
  - **Usuario técnico:** no si tiene tickets activos asignados.
  - **Circunscripción:** no si tiene juzgados asociados.
  - **Ticket:** solo si está en SOLICITADO sin técnico.
  - **Software:** sin restricciones.

## 5. Seguridad — Matriz de Autorización

| Recurso | Admin | Operario | Técnico |
|---|---|---|---|
| /api/auth/** | Público | Público | Público |
| /api/tickets (GET) | Todos | Todos | Solo sus tickets |
| /api/tickets (CUD) | ✔ | ✔ | ✘ (403) |
| /api/hardware/** | ✔ | ✔ | ✘ (403) |
| /api/software/** | ✔ | ✔ | ✘ (403) |
| /api/contratos/** | ✔ | ✔ | ✘ (403) |
| /api/juzgados/** | ✔ | ✔ | ✘ (403) |
| /api/circunscripciones/** | ✔ | ✔ | ✘ (403) |
| /api/usuarios (GET) | ✔ | Lectura | ✘ (403) |
| /api/usuarios (CUD) | ✔ | ✘ (403) | ✘ (403) |
| /api/roles (GET) | ✔ | ✔ | ✘ (403) |
| /api/audit/** | ✔ | ✘ (403) | ✘ (403) |

- Sin token → 401 Unauthorized
- Token de rol sin permiso → 403 Forbidden
- El filtro "solo sus tickets" para Técnico se maneja en TicketService, no en SecurityConfig.

## 6. Auditoría (AOP)

- `@Auditable(entidad, accion)` en métodos de Service.
- `AuditAspect` intercepta con `@Before` (captura estado anterior) y `@AfterReturning` (registra en audit_log).
- Acciones: `CREATE, UPDATE, DELETE, RESTORE, ASSIGN, CLOSE, ACTIVATE, DEACTIVATE`
- Para CREATE: `valor_anterior = NULL`.
- Para DELETE: `valor_nuevo = NULL`.
- La tabla `audit_log` es **INMUTABLE** — solo INSERT, nunca UPDATE ni DELETE.
- Si falla la auditoría, no interrumpe la operación principal.

## 7. Jobs Programados

| Job | Horario | Función |
|---|---|---|
| ContratoVencimientoJob | 08:00hs | Alerta contratos próximos a vencer + vencidos con hardware activo |
| SoftwareVencimientoJob | 08:15hs | Alerta licencias próximas a vencer (30 días) + vencidas |

Ambos envían email a todos los Admin y Operarios activos via EmailService.

## 8. Patrones del Código

- **Inyección por constructor** (sin @Autowired).
- **Respuestas uniformes:** siempre `ApiResponse<T>` con `success`, `message`, `data`, `timestamp`.
- **Mappers manuales estáticos:** `XxxMapper.toDTO(entity)` — nunca exponer entities en responses.
- **Excepciones tipadas:** `NotFoundException` → 404, `BusinessException` → 400/409.
- **GlobalExceptionHandler** con `@RestControllerAdvice` captura todas las excepciones.
- **@Transactional** en Services que modifican datos. `@Transactional(readOnly = true)` en lecturas.
- **Email asíncrono:** `@Async` en EmailService, no bloquea operaciones.
- **DTOs separados:** `XxxRequestDTO` (entrada con @Valid) y `XxxResponseDTO` (salida).

## 9. Endpoints Completos

### Auth
- `POST /api/auth/login` → LoginResponseDTO (token, tipo, usuarioId, nombreCompleto, email, rol)

### Tickets
- `GET /api/tickets` (filtros: estado, prioridad, juzgadoId, tecnicoId, q)
- `GET /api/tickets/{id}`
- `POST /api/tickets`
- `PUT /api/tickets/{id}`
- `PUT /api/tickets/{id}/asignar` (TicketAsignarRequestDTO)
- `PUT /api/tickets/{id}/reasignar` (TicketAsignarRequestDTO)
- `PUT /api/tickets/{id}/estado` (pasa a EN_CURSO)
- `PUT /api/tickets/{id}/cerrar` (TicketCerrarRequestDTO)
- `DELETE /api/tickets/{id}`
- `PUT /api/tickets/{id}/restore`

### Hardware
- `GET /api/hardware` (filtros: juzgadoId, clase, modelo, ubicacion)
- `GET /api/hardware/{id}`
- `GET /api/hardware/{id}/tickets`
- `POST /api/hardware`
- `PUT /api/hardware/{id}`
- `DELETE /api/hardware/{id}`
- `PUT /api/hardware/{id}/restore`

### Software
- `GET /api/software` (filtros: contratoId, juzgadoId, proveedor)
- `GET /api/software/{id}`
- `GET /api/software/vencimientos?dias=30`
- `POST /api/software`
- `PUT /api/software/{id}`
- `DELETE /api/software/{id}`
- `PUT /api/software/{id}/restore` *(pendiente de implementar)*

### Contratos
- `GET /api/contratos`
- `GET /api/contratos/{id}`
- `GET /api/contratos/proximos-vencer`
- `GET /api/contratos/vencidos`
- `POST /api/contratos`
- `POST /api/contratos/{id}/renovar` (ContratoRenovarRequestDTO)
- `PUT /api/contratos/{id}`
- `DELETE /api/contratos/{id}`
- `PUT /api/contratos/{id}/restore` *(pendiente de implementar)*

### Usuarios
- `GET /api/usuarios`
- `GET /api/usuarios/{id}`
- `GET /api/usuarios/tecnicos-activos`
- `GET /api/usuarios/buscar?q=texto`
- `POST /api/usuarios`
- `PUT /api/usuarios/{id}`
- `PUT /api/usuarios/{id}/activar`
- `PUT /api/usuarios/{id}/desactivar`
- `DELETE /api/usuarios/{id}`
- `PUT /api/usuarios/{id}/restore`

### Juzgados
- `GET /api/juzgados` (filtros: circunscripcionId, ciudad, fuero)
- `GET /api/juzgados/{id}`
- `POST /api/juzgados`
- `PUT /api/juzgados/{id}`
- `DELETE /api/juzgados/{id}`
- `PUT /api/juzgados/{id}/restore` *(pendiente de implementar)*

### Circunscripciones
- `GET /api/circunscripciones`
- `GET /api/circunscripciones/{id}`
- `POST /api/circunscripciones`
- `PUT /api/circunscripciones/{id}`
- `DELETE /api/circunscripciones/{id}`
- `PUT /api/circunscripciones/{id}/restore` *(pendiente de implementar)*

### Roles
- `GET /api/roles` (solo lectura)

### Auditoría
- `GET /api/audit/{entidad}/{registroId}`
- `GET /api/audit/usuario/{usuarioId}`
- `GET /api/audit?desde=...&hasta=...`
- `GET /api/audit/filtro?entidad=...&accion=...&desde=...&hasta=...`

## 10. Validaciones de DTOs

Las validaciones se hacen con Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Email`) en los RequestDTOs. El `GlobalExceptionHandler` captura `MethodArgumentNotValidException` y devuelve un mapa de errores campo → mensaje.

Los campos opcionales (prioridad, hardwareId, referenteNombre, etc.) no tienen `@NotNull` y se manejan con null-checks en el Service.

## 11. Convenciones para Revisión de Código

Al revisar el proyecto, verificar:
1. **Todos los imports coinciden con los paquetes PascalCase** (Entities, no entity).
2. **Queries JPQL usan strings para enums** (`'CERRADO'`, no rutas Java).
3. **Cada Service que tiene `eliminar()` debería tener `restaurar()`** y su Repository `findEliminadoById()`.
4. **Cada método CUD en Service tiene `@Auditable`** con la acción correcta.
5. **Los Controllers no tienen lógica de negocio** — solo delegan al Service.
6. **`@Transactional` en todos los métodos que modifican datos**, `readOnly = true` en lecturas.
7. **No hay `@Autowired`** — solo inyección por constructor.
8. **Endpoints con rutas fijas** (`/vencimientos`, `/proximos-vencer`, `/tecnicos-activos`) deben estar **antes** de `/{id}` en el Controller para evitar conflictos de ruteo.
