package com.example.medassistant.agent;


import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.dto.AppointmentInfo;
import com.example.medassistant.dto.agent.AppointmentRequest;
import com.example.medassistant.dto.agent.AppointmentSelection;
import com.example.medassistant.service.AppointmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentChainServiceImpl  implements  AppointmentChainService {

    private final ClientResolver clientResolver;
    private final AppointmentService appointmentService;
    private final PendingBookingStore pendingBookingStore;

    @Value("classpath:/prompts/appointment-extraction.st")
    private Resource extractionResource;

    @Value("classpath:/prompts/appointment-confirmation.st")
    private Resource confirmationResource;

    private PromptTemplate extractionTemplate;
    private PromptTemplate confirmationTemplate;

    @PostConstruct
    public void init() {
        extractionTemplate = new PromptTemplate(extractionResource);
        confirmationTemplate = new PromptTemplate(confirmationResource);
    }

    @Override
    public String bookAppointmentChain(String userRequest, String model, Long userId) {
        AppointmentRequest extracted = interpret(userRequest, model);
        List<AppointmentInfo> available = search(extracted);

        return select(userRequest, available, model, userId);
    }

    /**
     * Patrón Human-in-the-Loop: Confirmación explícita.
     * Recupera el estado temporal de la reserva y efectúa la escritura final en base de datos.
     */
    @Override
    public String confirmBooking(Long userId) {
        // Recuperamos y eliminamos el turno de la memoria temporal (caché/Map)
        AppointmentSelection selection =  pendingBookingStore.remove(userId);

        if (selection == null) {
            return "No hay un turno seleccionado para confirmar";
        }

        // Control de Concurrencia:
        // Evita el problema del doble booking por si otro paciente reservó
        // este mismo hueco mientras el usuario actual se lo estaba pensando.
        if (!isStillAvailable(selection)) {
            log.info("Turno ya no está disponible para userId={}: {} {} {}",
                    userId, selection.specialty(), selection.date(), selection.time());
            return "El turno seleccionado ya no está disponible. Por favor, elige otro turno.";
        }

        return book(selection, userId);
    }

    /**
     * Helper privado que delega la persistencia final a la base de datos.
     */
    private String book(AppointmentSelection selection, Long userId) {

        String result = appointmentService.bookAppointment(
                selection.specialty(),
                LocalDate.parse(selection.date()),
                LocalTime.parse(selection.time()),
                userId
        );

        log.info("Resultado de la reserva: {}", result);
        return result;
    }
    /**
     * Verifica contra la base de datos en tiempo real si el hueco sigue libre.
     */
    private boolean isStillAvailable(AppointmentSelection selection) {
        List<AppointmentInfo> available = appointmentService.findAvailableAppointments(
                selection.specialty(), LocalDate.parse(selection.date()));

        return available.stream().anyMatch( a -> a.time().equals(selection.time()));
    }

    /**
     * Descarta la reserva temporal liberando la memoria.
     */
    @Override
    public String cancelBooking(Long userId) {

        AppointmentSelection selection =  pendingBookingStore.remove(userId);

        if (selection == null) {
            return "No hay un turno seleccionado para cancelar";
        }
        log.info("Turno cancelado por userId={}: {} {} {}",
                userId, selection.specialty(), selection.date(), selection.time());

        return "Turno descartado, puedes buscar otro cuando quieras.";
    }

    private AppointmentRequest interpret(String userRequest, String model){

        String prompt = extractionTemplate.render(Map.of("pedido", userRequest));

        AppointmentRequest extracted = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un extractor de datos, extraes exactamente lo que dice el texto, sin interpretar ni cambiar nada")
                .user(prompt)
                .call()
                .entity(AppointmentRequest.class);

        log.info("Chain paso 1 - extraído: specialty={}, date={}", extracted.specialty(), extracted.date());
        return  extracted;
    }

    private List<AppointmentInfo> search(AppointmentRequest extracted) {

        List<AppointmentInfo> available = appointmentService.findAvailableAppointments(
                extracted.specialty(), LocalDate.parse(extracted.date())
        );

        log.info("Chain paso 2 - turnos encontrados: {}", available.size());
        return  available;
    }

    private String select(String userRequest, List<AppointmentInfo> available,
                          String model, Long userId) {
        if (available.isEmpty()) {
            return "No se encontraron turnos disponibles para esa especialidad, puedes probar con otra fecha.";
        }

        AppointmentSelection selection = selectAppointment(userRequest, available, model);

        pendingBookingStore.store(userId, selection);

        return selection.message();

    }

    private AppointmentSelection selectAppointment(String userRequest, List<AppointmentInfo> available, String model) {
        String prompt = extractionTemplate.render(Map.of(
                "pedido", userRequest,
                "turnos", available.toString()
                ));

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un asistente de gestión de turnos médicos. " +
                        "Selecciona el mejor turno y completa todos los campos.")
                .user(prompt)
                .call()
                .entity(AppointmentSelection.class);
    }
}
