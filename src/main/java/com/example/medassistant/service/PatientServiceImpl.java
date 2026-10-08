package com.example.medassistant.service;

import com.example.medassistant.dto.PatientInfo;
import com.example.medassistant.model.Patient;
import com.example.medassistant.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * Implementación del servicio de dominio para la gestión del expediente clínico del paciente.
 * Actúa como puente entre la capa de persistencia relacional y las herramientas de IA,
 * proyectando las entidades {Patient} hacia representaciones inmutables {PatientInfo}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements  PatientService {

    private final PatientRepository patientRepository;

    /**
     * Recupera y transforma el expediente del paciente por su identificador primario.
     * @param patientId Identificador único del paciente en base de datos.
     * @return DTO inmutable con la información médica o null si el registro no existe.
     */

    @Override
    public PatientInfo getPatientInfo(Long patientId) {


        log.info("Consultando historial: patientId={}", patientId);

        return patientRepository.findById(patientId)
                .map(this::toPatientInfo)
                .orElse(null);
    }

    private  PatientInfo toPatientInfo(Patient patient) {
        return new  PatientInfo(
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth().toString(),
                patient.getAllergies(),
                patient.getConditions());
    }
}
