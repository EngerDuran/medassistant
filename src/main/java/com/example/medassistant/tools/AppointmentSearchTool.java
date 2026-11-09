package com.example.medassistant.tools;

import com.example.medassistant.dto.AppointmentInfo;
import com.example.medassistant.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Herramienta de búsqueda de citas médicas.
 * Este componente es clave en la arquitectura porque sirve a dos mundos simultáneamente:
 * Uso interno Spring AI: Usado por nuestro OpenAgent local mediante @Tool.
 * Uso externo MCP: Expuesto a agentes externos (Claude, Cursor) mediante @McpTool.
 * De esta forma reutilizamos la lógica de negocio sin duplicar código.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentSearchTool {
    private final AppointmentService appointmentService;

    /**
     * Consulta turnos médicos libres disponibles en la base de datos.
     * El motor de Spring intercepta estas anotaciones para generar los JSON Schemas
     * que los LLMs necesitan para saber cómo y cuándo invocar esta función.
     */

    @Tool(description = "Buscar turnos médicos disponibles para una especialidad y fecha, usar " +
            "cuando un usuario pregunte por disponibilidad de turnos o citas médicas.")
    @McpTool(name = "search-appointments",
            description = "Busca turnos disponibles par auna especialidad en una fecha")
    public List<AppointmentInfo> searchAppointments(
           @ToolParam(description = "Especialidad médica, por ejemplo: cardiología, pediatría, dermatología")
                   @McpToolParam(description = "Especialidad médica", required = true)
                        String specialty,
           @ToolParam(description = "Fecha de la cita en formato yyyy-MM-dd")
                   @McpToolParam(description = "fecha en formato yyyy-MM-DD", required = true)
                        String date
    ){
        log.info("Buscando turnos disponibles: specialty={}, date={}", specialty, date);

        return appointmentService.findAvailableAppointments(specialty, LocalDate.parse(date));
    }
}
