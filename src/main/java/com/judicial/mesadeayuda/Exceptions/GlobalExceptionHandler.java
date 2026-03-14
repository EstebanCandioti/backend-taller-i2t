package com.judicial.mesadeayuda.Exceptions;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.judicial.mesadeayuda.DTO.Response.ApiResponse;

/**
 * Manejo GLOBAL de excepciones para todos los Controllers.
 * Transforma cada tipo de excepción en un ApiResponse uniforme.
 *
 * Flujo: Controller → Service lanza excepción → GlobalExceptionHandler → ApiResponse
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 404 NOT FOUND - Recurso no encontrado.
     * Captura: NotFoundException lanzada en Services.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 400 BAD REQUEST / 409 CONFLICT - Violación de regla de negocio.
     * Captura: BusinessException lanzada en Services.
     * El código HTTP depende de lo que defina la excepción.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Error de negocio: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 400 BAD REQUEST - Error de validación @Valid.
     * Captura: MethodArgumentNotValidException cuando falla @Valid en DTOs.
     * Devuelve mapa campo → mensaje de error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Error de validación: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError("Error de validación", errors));
    }

    /**
     * 401 UNAUTHORIZED - Credenciales inválidas.
     * Captura: BadCredentialsException de Spring Security en el login.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Intento de login fallido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Credenciales inválidas"));
    }

    /**
     * 403 FORBIDDEN - Sin permisos para la acción.
     * Captura: AccessDeniedException de Spring Security.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene permisos para realizar esta acción"));
    }

    /**
     * 400 BAD REQUEST - Argumentos inválidos genéricos.
     * Captura: IllegalArgumentException lanzada en validaciones manuales.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 409 CONFLICT - Violación de constraint en la BD.
     * Captura: DataIntegrityViolationException (ej: email duplicado, nro_inventario repetido).
     * Actúa como doble seguridad: si la validación del Service no lo atrapa,
     * la BD lo rechaza y este handler devuelve un mensaje amigable.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());

        String mensaje = "Error de integridad de datos: registro duplicado o referencia inválida";

        // Intentar dar un mensaje más específico según el constraint violado
        String detalle = ex.getMostSpecificCause().getMessage();
        if (detalle != null) {
            if (detalle.contains("email")) {
                mensaje = "Ya existe un usuario con ese email";
            } else if (detalle.contains("nro_inventario")) {
                mensaje = "Ya existe un equipo con ese número de inventario";
            } else if (detalle.contains("nombre") && detalle.contains("roles")) {
                mensaje = "Ya existe un rol con ese nombre";
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(mensaje));
    }

    /**
     * 400 BAD REQUEST - JSON mal formado o valor de enum inválido.
     * Captura: HttpMessageNotReadableException (ej: prioridad "URGENTISIMA" no existe).
     * Sin este handler, el cliente recibiría un 500 genérico.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request con body ilegible: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El cuerpo de la solicitud es inválido o tiene un formato incorrecto"));
    }

    /**
     * 500 INTERNAL SERVER ERROR - Error inesperado.
     * Captura: cualquier excepción no manejada anteriormente.
     * NUNCA expone detalles internos al cliente.
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRequestParameters(Exception ex) {
        log.warn("ParÃ¡metros invÃ¡lidos en la request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Los parÃ¡metros de la solicitud son invÃ¡lidos o incompletos"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Error interno no controlado: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor"));
    }
}
