package com.example.medassistant.tools;

import com.example.medassistant.dto.PatientInfo;
import com.example.medassistant.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI Tool protegida mediante contexto de servidor (ToolContext).
 * Implementa control de acceso a nivel de herramienta: combina los argumentos extraídos
 * por el LLM con metadatos de sesión/seguridad inyectados por el backend, mitigando ataques
 * de elevación de privilegios o consulta de historiales clínicos no autorizados.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PatientInfoTool {
    private final PatientService patientService;

    @Tool(description = "Consultar el historial clínico de un paciente por su ID. " +
    "Usar cuando el usuario pregunte por su historial médico, alergias o condiciones.")
    public PatientInfo getPatientInfo(
            @ToolParam(description = "ID numérico del paciente") Long patientId,
            ToolContext context
    ){
        log.info("Tool invocada - getPatientInfo: patientId={}", patientId);

        long userId = (long) context.getContext().get("userId");
        log.info("ToolContext - userId={}", userId);

        if(!patientId.equals(userId)) {
            log.warn("Acceso denegado: userId={} intentó acceder a patientId={}",userId,patientId);
            return null;
        }


        return  patientService.getPatientInfo(patientId);
    }
}
