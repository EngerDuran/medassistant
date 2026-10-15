package com.example.medassistant.service;

import com.example.medassistant.dto.AppointmentInfo;
import com.example.medassistant.model.Appointment;
import com.example.medassistant.model.Doctor;
import com.example.medassistant.repository.AppointmentRepository;
import com.example.medassistant.repository.DoctorRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de dominio responsable de la orquestación y consulta de turnos médicos.
 * Actúa como capa de negocio intermedia: recupera datos relacionales, ejecuta agregaciones en memoria
 * para evitar el problema de consultas N+1 y mapea las entidades JPA hacia DTOs inmutables optimizados
 * para su consumo por agentes de IA y APIs REST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Recupera todos los turnos disponibles para una especialidad y fecha determinadas.
     * @param specialty Especialidad médica a filtrar (ej. 'Cardiología').
     * @param date      Fecha seleccionada para la disponibilidad.
     * @return Lista aplanada de DTOs {AppointmentInfo} listos para serialización.
     */

    @Override
    public List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date) {
        log.info("Buscando turnos disponibles: specialty={}, date={}", specialty, date);

        var doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);

        if(doctors.isEmpty()) {
            log.info("No se encontraron médicos con la especialidad: {}", specialty);
                    return List.of();
        }

        // Optimización de rendimiento: creación de diccionario en memoria
        var doctorNames = buildDoctorNameMap(doctors);
        var doctorIds = new ArrayList<>(doctorNames.keySet());

        // Batch lookup en base de datos utilizando la cláusula SQL 'IN'
        var appointments = appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(doctorIds, date);

        return toAppointmentInfoList(appointments, doctorNames, specialty);
    }


    /**
     * Reserva un turno médico y actualiza su estado en la base de datos.
     * Se usa @Transactional para asegurar que, si ocurre algún error al guardar,
     * no queden datos corruptos ni turnos bloqueados a medias.
     */
    @Transactional
    @Override
    public String bookAppointment(String specialty, LocalDate date, LocalTime time, Long patientId) {

        log.info("Reservando turno: specialty={}, date={}, time={}, patientId={}",
                specialty, date, time, patientId);

        // Verificamos que existan médicos para esa especialidad
        var doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        if(doctors.isEmpty()) {
            return "No se encontró la especialidad: " + specialty;
        }

        // Extraemos solo los IDs de los médicos para buscar en sus agendas
        var doctorIds = doctors.stream().map(Doctor::getId).toList();

        // Buscamos si alguno tiene libre ese día a esa hora exacta
        var appointment = findAvailableAppointment(doctorIds, date, time);
        if(appointment == null) {
            return "El turno no está disponible.";
        }

        // Bloqueamos el turno, asignamos el paciente y guardamos los cambios
        appointment.setAvailable(false);
        appointment.setPatientId(patientId);

        appointmentRepository.save(appointment);

        return "Turno reservado exitosamente.";
    }

    @Transactional(readOnly = true)
    private Appointment findAvailableAppointment(List<Long> doctorIds, LocalDate date, LocalTime time){
        return appointmentRepository.findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(doctorIds, date, time)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Genera un mapa asociativo de ID de doctor a Nombre Completo para acelerar la resolución en memoria.
     */
    private Map<Long, String> buildDoctorNameMap(List<Doctor> doctors) {
        return doctors.stream()
                .collect(Collectors.toMap(
                        Doctor::getId,
                        doctor -> doctor.getFirstName() + " " + doctor.getLastName()
                ));
    }

    /**
     * Transforma la colección de entidades JPA Appointment a DTOs de salida limpios.
     */

    private List<AppointmentInfo> toAppointmentInfoList(
            List<Appointment> appointments,
            Map<Long, String> doctorNames,
            String specialty
    ){
        return appointments.stream()
                .map(a -> new AppointmentInfo(
                        doctorNames.get(a.getDoctorId()),
                        specialty,
                        a.getDate().toString(),
                        a.getStartTime().toString()
                )).toList();
    }
}
