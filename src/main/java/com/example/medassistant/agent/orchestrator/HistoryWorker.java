package com.example.medassistant.agent.orchestrator;


import com.example.medassistant.dto.PatientInfo;
import com.example.medassistant.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryWorker {

    private final PatientService patientService;

    public String lookup(Long userId) {

        PatientInfo patient = patientService.getPatientInfo(userId);

        if(patient == null) {
            log.info("Worker historial - paciente no encontrado");
            return "No se encontró información del paciente";
        }

        String result = String.format(
                "Paciente: %s %S | Fecha de nacimiento: %s | Alergias: %s | Condiciones: %s",
                patient.firstName(), patient.lastName(),
                patient.dateOfBirth(),
                patient.allergies() != null ? patient.allergies() : "Ninguna registrada",
                patient.conditions() != null ? patient.conditions() : "Ninguna registrada"
        );

        log.info("Worker historial - historial encontrado");
        return result;
    }
}
