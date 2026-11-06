package co.edu.uniajc.prestamos.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> api(ApiException ex) {
        return ResponseEntity.status(ex.status).body(Map.of("error", ex.code, "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldErrors().stream().findFirst();
        String message = field.map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(Map.of("error", "DATOS_INVALIDOS", "message", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> invalidJson() {
        return ResponseEntity.badRequest().body(Map.of("error", "DATOS_INVALIDOS", "message", "JSON o valor de campo inválido"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, String>> invalidParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "DATOS_INVALIDOS", "message", "Parámetro inválido: " + ex.getName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, String>> missingResource() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "NO_ENCONTRADO", "message", "Recurso no encontrado"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, String>> methodNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Map.of("error", "METODO_NO_PERMITIDO", "message", "Método HTTP no permitido"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Map<String, String>> unsupportedMedia() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(Map.of("error", "FORMATO_NO_ADMITIDO", "message", "Use Content-Type: application/json"));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<Map<String, String>> unacceptableMedia() {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
            .body(Map.of("error", "FORMATO_NO_ACEPTABLE", "message", "Formato de respuesta no disponible"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<Map<String, String>> missingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "DATOS_INVALIDOS", "message", "Falta parámetro: " + ex.getParameterName()));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ResponseEntity<Map<String, String>> concurrentChange() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "CONFLICTO", "message", "El recurso cambió durante la operación; actualice y reintente"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> integrity() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CONFLICTO", "message", "Los datos duplican un valor único o violan una relación"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> denied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "PROHIBIDO", "message", "No tiene permiso para esta operación"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception ex, HttpServletRequest request) {
        // El detalle se registra en el backend y nunca se envía al cliente.
        org.slf4j.LoggerFactory.getLogger(ApiErrorHandler.class).error("Error en {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "ERROR_INTERNO", "message", "No se pudo completar la operación"));
    }
}
