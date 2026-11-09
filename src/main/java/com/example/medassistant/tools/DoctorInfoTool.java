package com.example.medassistant.tools;

import com.example.medassistant.dto.DoctorInfo;
import com.example.medassistant.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI Tool para la resolución autónoma de consultas sobre personal médico.
 * Permite al LLM identificar y consultar datos de contacto, matrícula y ubicación de doctores.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorInfoTool {
    private final DoctorService doctorService;

    @Tool(description = "Buscar información de un médico por nombre o especialidad. " +
    "Usar cuando el usuario pregunte por datos de un doctor, quien atiende una especialidad, " +
    "o información sobre un médico.")
    @McpTool(
            name = "search-doctors",
            description = "Busca médicos por nombre y especialidad"

    )
    public List<DoctorInfo> searchDoctors(
            @ToolParam(description = "Solo el apellido del médico o el nombre de la especialidad, sin títulos como Dr. o Dra")
            @McpToolParam(
                    description = "Apellido o especialidad médica",
                    required = true
            )
            String query) {

        log.info("Tool invocada - searchDoctors: query={}", query);
        return doctorService.searchDoctors(query);
    }
}
