package com.example.medassistant.mcp;

import com.example.medassistant.dto.DoctorInfo;
import com.example.medassistant.service.DoctorService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Proveedor de Recursos MCP.
 * Expone datos internos del sistema de solo lectura hacia clientes externos
 * ejm Claude Desktop, Cursor, u otros agentes, sin usar endpoints REST tradicionales,
 * permitiendo que la IA externa descubra y consuma nuestro contexto de forma nativa.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DoctorResourceProvider {

    private final DoctorService doctorService;

    /**
     * Define un Resource dinámico mediante URI paramétrica.
     * Cuando el LLM externo pida "doctor://cardiologia", MCP enrutará la petición
     * automáticamente a este método inyectando "cardiologia" como parámetro.
     */

    @McpResource(
            uri = "doctor://{specialty}",
            name = "Doctores por especialidad",
            description = "Lista de doctores disponibles para una especialidad médica"
    )
    public McpSchema.ReadResourceResult getDoctorsBySpecialty(String specialty) {
        List<DoctorInfo> doctors = doctorService.searchDoctors(specialty);

        String content = doctors.isEmpty()
                ? "No se encontraron doctores para la especialidad: " + specialty
                : listDoctors(doctors);

        // Empaquetamos la respuesta estricta en el esquema de MCP para garantizar
        // la compatibilidad con cualquier cliente que hable este protocolo
        return McpSchema.ReadResourceResult.builder(
                List.of(
                        new McpSchema.TextResourceContents(
                                "doctor://" + specialty,
                                "text/plain", content
                        ))).build();
    }


    /**
     * Mapea la lista de dtos a un string plano legible para el LLM.
     */
    private String listDoctors(List<DoctorInfo> doctorInfos) {
        return doctorInfos.stream()
                .map(DoctorInfo::toString)
                .collect(Collectors.joining("\n"));
    }
}
