package com.example.medassistant.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.nio.file.AccessDeniedException;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;


/**
 * Controlador global de excepciones.
 * Si la API externa se cae o tarda en responder, capturamos el error aquí
 * para devolver un error limpio (HTTP 503) en lugar de romper toda la aplicación.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<String> handleRestClientException(RestClientException e) {

        log.warn("Error consultando API externa: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("El servicio externo no está disponible en este momento");

    }


    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<String> handleDateTimeParseException(DateTimeParseException e) {
        log.warn("Formato de fecha inválido: {}", e.getParsedString());
        return ResponseEntity.badRequest()
                .body("Formato de fecha inválido. Use el formato yyyy-MM-dd");
    }


    /**
     * Intercepta las excepciones de validación como pedir un modelo que no existe
     * y las transforma en un error HTTP 400 (Bad Request) limpio para el frontend,
     * evitando que el servidor devuelva un StackTrace completo HTTP 500 que exponga
     * detalles internos de la arquitectura.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public  ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Argumento inválido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public  ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Acceso denegado: {}", e.getMessage());
        return ResponseEntity.status(403)
                .body("No tienes permisos para realizar esta accion.");
    }


}
