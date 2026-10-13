package com.example.medassistant.dto;

/**
 * Record DTO para mapear únicamente los datos clave del prospecto.
 * Filtra el ruido del JSON original de la FDA para enviar respuestas limpias al LLM.
 */
public record DrugInfo(
        String brandName,
        String genericName,
        String purpose,
        String warning,
        String dosageAndAdministration
) {
}
