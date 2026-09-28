package com.example.medassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * CHILD DTO (Record Anidado):
 * Representa una entidad clínica subordinada dentro de SymptomAnalysis.
 * Jackson y Spring AI derivan un sub-esquema JSON para los elementos del array,
 * permitiendo respuestas jerárquicas tipadas sin concatenar texto manualmente.
 */

public record PossibleCondition(

        @JsonPropertyDescription("Nombre de la condición médica")
        String name,

        @JsonPropertyDescription("Explicación breve y accesible de la condición")
        String description,

        @JsonPropertyDescription("Nivel de gravedad de esta condición específica")
        Severity severity

) {
}
