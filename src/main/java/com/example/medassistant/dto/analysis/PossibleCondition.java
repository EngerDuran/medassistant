package com.example.medassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record PossibleCondition(

        @JsonPropertyDescription("Nombre de la condición médica")
        String name,

        @JsonPropertyDescription("Explicación breve y accesible de la condición")
        String description,

        @JsonPropertyDescription("Nivel de gravedad de esta condición específica")
        Severity severity

) {
}
