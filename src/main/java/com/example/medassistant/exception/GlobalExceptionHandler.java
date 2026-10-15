package com.example.medassistant.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

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


}
