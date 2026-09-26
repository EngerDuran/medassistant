package com.example.medassistant.dto.analysis;

/**
 * Representa la salida estructurada para el resumen de una condición médica.
 * Spring AI utiliza este Record para derivar automáticamente el JSON Schema
 * que condiciona la respuesta del modelo de lenguaje.
 */
public record ConditionSummary(
        String conditionName,
        String description,
        String commonSymptoms,
        String whenToSeeDoctor
) {
}
