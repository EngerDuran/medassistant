package com.example.medassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QueryClassification(
        @JsonPropertyDescription("Tipo de consulta identificada")
        QueryType type,

        @JsonPropertyDescription("Explicación breve de por qué se clasificó de esta manera")
        String reason
) {
}
