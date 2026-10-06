package com.example.medassistant.dto;

/**
 * DTO inmutable con los datos de contacto y colegiatura de un facultativo.
 * Desacopla la entidad Doctor exponiendo solo los datos relevantes para el LLM.
 */

public record DoctorInfo(
        String firstName,
        String lastName,
        String speciality,
        String licenseNumber,
        String phone,
        String office
) {
}