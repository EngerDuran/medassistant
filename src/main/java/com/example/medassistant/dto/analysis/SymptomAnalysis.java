package com.example.medassistant.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record SymptomAnalysis(

        @JsonPropertyDescription("Lista de sintomas identificados en la consulta del paciente")
        List<String> identifiedSymptoms,

        @JsonPropertyDescription("Posibles condiciones médicas basadas en los sintomas, ordenadas de más a menos probales")
        List<PossibleCondition> possibleConditions,

        @JsonPropertyDescription("Nivel de urgencia del paciente")
        Urgency urgencyLevel,

        @JsonPropertyDescription("Recomendación para el paciente, incluyendo si debe buscar atención médica")
        String recommendation
        ) {


}
