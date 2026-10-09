package com.example.medassistant.config;

import com.example.medassistant.model.Appointment;
import com.example.medassistant.model.Doctor;
import com.example.medassistant.model.Patient;
import com.example.medassistant.repository.AppointmentRepository;
import com.example.medassistant.repository.DoctorRepository;
import com.example.medassistant.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Componente de inicialización y seeding del entorno de desarrollo.
 *
 * Implementa {CommandLineRunner} para poblar la base de datos con un dataset
 * clínico de prueba (doctores, turnos dinámicos y pacientes con antecedentes médicos),
 * garantizando un estado determinista para pruebas de Tool Calling y RAG.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    @Override
    public void run(String... args) {
        loadDoctors();
        loadAppointments();
        loadPatients();
        log.info("Datos de prueba cargados: {} doctores, {} turnos, {} pacientes",
                doctorRepository.count(),
                appointmentRepository.count(),
                patientRepository.count());
    }

    private void loadDoctors() {
        doctorRepository.saveAll(List.of(
                Doctor.builder().firstName("Ana").lastName("Martínez").specialty("Cardiología")
                        .licenseNumber("MP-12345").phone("555-0101").office("Consultorio 101").build(),
                Doctor.builder().firstName("Carlos").lastName("López").specialty("Dermatología")
                        .licenseNumber("MP-23456").phone("555-0102").office("Consultorio 205").build(),
                Doctor.builder().firstName("Laura").lastName("Gómez").specialty("Pediatría")
                        .licenseNumber("MP-34567").phone("555-0103").office("Consultorio 302").build(),
                Doctor.builder().firstName("Martín").lastName("Ruiz").specialty("Traumatología")
                        .licenseNumber("MP-45678").phone("555-0104").office("Consultorio 110").build(),
                Doctor.builder().firstName("Sofía").lastName("Perez").specialty("Endocrinología")
                        .licenseNumber("MP-56789").phone("555-0105").office("Consultorio 408").build()
        ));
    }

    private void loadAppointments() {
        var today = LocalDate.now();
        var doctors = doctorRepository.findAll();

        LocalTime[] times = {
                LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(11, 0), LocalTime.of(15, 0)
        };

        int count = 0;
        for (int day = 1; day <= 5; day++) {
            for (var time : times) {
                var doctor = doctors.get(count % doctors.size());
                appointmentRepository.save(
                        Appointment.builder()
                                .doctorId(doctor.getId())
                                .date(today.plusDays(day))
                                .startTime(time)
                                .available(count % 3 != 2)
                                .build()
                );
                count++;
            }
        }
    }

    private void loadPatients() {
        patientRepository.saveAll(List.of(
                Patient.builder().firstName("Enger").lastName("Ejemplo")
                        .dateOfBirth(LocalDate.of(1985, 3, 15))
                        .allergies("Penicilina")
                        .conditions("Hipertensión leve controlada con medicación").build(),
                Patient.builder().firstName("María").lastName("Test")
                        .dateOfBirth(LocalDate.of(1990, 7, 22))
                        .allergies("Ninguna conocida")
                        .conditions("Diabetes tipo 2 diagnosticada en 2019").build(),
                Patient.builder().firstName("Juan").lastName("Demo")
                        .dateOfBirth(LocalDate.of(1978, 11, 8))
                        .allergies("Ibuprofeno, Mariscos")
                        .conditions("Sin condiciones preexistentes").build()
        ));
    }
}