package com.example.medassistant.service;

import com.example.medassistant.dto.AppointmentInfo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Contrato para la gestión de citas médicas (lectura de agenda y reserva de turnos).
 */
public interface AppointmentService {

     // Consulta los huecos libres filtrando por especialidad y día
     List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date);

     // Registra la reserva de un turno asignándolo al ID del paciente autenticado
     String bookAppointment(String specialty, LocalDate date, LocalTime time, Long patientId);

}