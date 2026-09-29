package com.example.medassistant.tools;

import com.example.medassistant.dto.AppointmentInfo;
import com.example.medassistant.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring AI Tool (Function Calling) para la consulta autónoma de disponibilidad clínica.
 * Expone métodos de negocio como herramientas ejecutables por el LLM mediante metadatos semánticos.
 * El motor de Spring AI genera en tiempo de compilación y ejecución la definición JSON Schema
 * para que el modelo decida de forma autónoma invocar esta herramienta ante intenciones de agendamiento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentSearchTool {
    private final AppointmentService appointmentService;

    /**
     * Consulta turnos médicos libres disponibles en base de datos.
     * @param specialty Especialidad médica identificada en el prompt del usuario.
     * @param date      Fecha solicitada en formato estricto ISO-8601 (YYYY-MM-DD).
     * @return Lista estructurada con los turnos disponibles para que el LLM elabore la respuesta final.
     */

    @Tool(description = "Buscar turnos médicos disponibles para una especialidad y fecha, usar " +
            "cuando un usuario pregunte por disponibilidad de turnos o citas médicas.")
    public List<AppointmentInfo> searchAppointments(
           @ToolParam(description = "Especialidad médica, por ejemplo: cardiología, pediatría")
                        String specialty,
           @ToolParam(description = "Fecha de la cita en formato yyyy-MM-dd")
                        String date
    ){
        log.info("Buscando turnos disponibles: specialty={}, date={}", specialty, date);
        return appointmentService.findAvailableAppointments(specialty, LocalDate.parse(date));
    }
}
