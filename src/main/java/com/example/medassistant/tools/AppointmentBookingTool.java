package com.example.medassistant.tools;

import com.example.medassistant.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tool de escritura que permite al asistente agendar citas en la base de datos.
 * Incluye validación de seguridad por rol antes de ejecutar cambios reales.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentBookingTool {

    private final AppointmentService appointmentService;

    @Tool(description = "Reservar un turno médico para un paciente. " +
            "Solo personal administrativo puede ejecutar esta acción. " +
            "Pedir confirmación antes de reservar.")
    public String bookAppointment(
            @ToolParam(description = "Especialidad médica") String specialty,
            // Se piden como String porque los LLMs formatean mejor texto plano que objetos de fecha
            @ToolParam(description = "fecha del turno en formato yyyy-MM-dd") String date,
            @ToolParam(description = "Hora del turno en formato HH:mm") String time,
            @ToolParam(description = "ID numérico del paciente") Long patientId,
            ToolContext context
    ){
        // Extraemos el rol real desde el contexto de seguridad (inyectado desde el JWT)
        String role = context.getContext().get("role").toString();

        log.info("Tool invocada - bookAppointment: specialty={}, date={}, time={}, patientId={}, role={}",
                specialty, date, time, patientId, role);

        // Doble barrera de seguridad: aunque el prompt dice que es solo para ADMIN,
        // lo bloqueamos por código para evitar que un paciente engañe a la IA.
        if (!"ADMIN".equals(role)) {
            log.warn("Acceso denegado: rol {} intentó reservar turno", role);
            return "Acceso denegado. Solo personal administrativo puede reservar turnos";
        }

        // Convertimos el texto de la IA a objetos LocalDate/LocalTime de Java y ejecutamos la reserva
        return appointmentService.bookAppointment(
                specialty,
                LocalDate.parse(date),
                LocalTime.parse(time),
                patientId
        );
    }
}