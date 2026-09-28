package com.example.medassistant.dto.analysis;


/**
 * Enums de Control:
 * Restringen los valores permitidos en el JSON Schema generado a una lista discreta de constantes.
 * Garantizan que el LLM no invente términos arbitrarios y permiten validación estricta en deserialización.
 */

public enum Severity {
    MILD,
    MODERATE,
    SEVERE
}
