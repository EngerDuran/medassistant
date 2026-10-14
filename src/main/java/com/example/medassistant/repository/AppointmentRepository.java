package com.example.medassistant.repository;

import com.example.medassistant.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repositorio de gestión de agenda médica y turnos clínicos.
 * Diseñado como puente de persistencia para herramientas de agendamiento autónomo impulsadas por IA.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorIdInAndDateAndAvailableTrue(
            List<Long> doctorIds, LocalDate date);

    List<Appointment> findByDoctorIdAndDateAndStartTimeAndAvailableTrue(
            List<Long> doctorIds, LocalDate date, LocalTime startTime);
}
