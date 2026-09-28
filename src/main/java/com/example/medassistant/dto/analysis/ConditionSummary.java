package com.example.medassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Representa la salida estructurada para el resumen de una condición médica.
 * Spring AI utiliza este Record para derivar automáticamente el JSON Schema
 * que condiciona la respuesta del modelo de lenguaje.
 */
public record ConditionSummary(
        // @JsonPropertyDescription: Proporciona contexto semántico en el JSON Schema
        // para orientar al LLM sobre el contenido esperado en cada campo.

        @JsonPropertyDescription("Nombre de la condición médica")
        String conditionName,

        @JsonPropertyDescription("Descripción accesible para pacientes sin jergas médicas innecesarias.")
        String description,

        @JsonPropertyDescription("Síntomas más frecuentes asociadoas a esta condición")
        String commonSymptoms,

        @JsonPropertyDescription("Nivel de gravedad general de la condición")
        Severity severity,

        @JsonPropertyDescription("Indicaciones claras de cuándo buscar atención médica profesional")
        String whenToSeeDoctor
) {
}
