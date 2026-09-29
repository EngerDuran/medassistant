package com.example.medassistant.repository;

import com.example.medassistant.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de gestión de agenda médica y turnos clínicos.
 * Diseñado como puente de persistencia para herramientas de agendamiento autónomo impulsadas por IA.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    /**
     * Consulta optimizada para verificar disponibilidad de turnos en lote.
     * Permite al LLM consultar huecos libres filtrando simultáneamente por múltiples
     * facultativos (IN clause), fecha específica y estado de disponibilidad activo.
     */
    List<Appointment> findByDoctorIdInAndDateAndAvailableTrue(
            List<Long> doctorIds, LocalDate date);
}
