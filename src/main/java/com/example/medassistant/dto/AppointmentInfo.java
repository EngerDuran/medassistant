package com.example.medassistant.dto;

/**
 * Data Transfer Object que modela un turno médico disponible.
 * Diseñado específicamente como contrato de salida simplificado para Function Calling:
 * aplana las entidades relacionales (Doctor y Appointment) en una estructura ligera,
 * desacoplando el esquema de la base de datos de la interfaz expuesta al modelo de lenguaje.
 */
public record AppointmentInfo(
        String doctorName,
        String specialty,
        String date,
        String time
) {
}
