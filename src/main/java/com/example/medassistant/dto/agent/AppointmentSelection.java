package com.example.medassistant.dto.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Representa la selección temporal de un turno médico.
 * Se implementa como un record para garantizar la inmutabilidad de los datos.
 * Las anotaciones @JsonPropertyDescription actúan como metadatos que Spring AI
 * envía al LLM para que sepa exactamente qué formato y tipo de dato debe generar en el JSON.
 */
public record AppointmentSelection(
        @JsonPropertyDescription("Especialidad médica del turno seleccionado")
        String specialty,

        @JsonPropertyDescription("Fecha del turno seleccionado en formato yyyy-MM-dd")
        String date,

        @JsonPropertyDescription("Hora del turno seleccionado en formato HH:mm")
        String time,

        @JsonPropertyDescription("Nombre del médico del turno seleccionado")
        String doctorName,

        @JsonPropertyDescription("Mensaje breve informando al paciente sobre el turno encontrado")
        String message
) {
}