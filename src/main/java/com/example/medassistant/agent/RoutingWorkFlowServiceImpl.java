package com.example.medassistant.agent;

import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.config.MedicalAuditAdvisor;
import com.example.medassistant.dto.analysis.QueryClassification;
import com.example.medassistant.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Servicio central del Workflow de Enrutamiento (Router Workflow).
 * En vez de usar un único prompt gigante para todo, este patrón usa un LLM
 * más rápido/barato (AnalysisService) para clasificar la intención del usuario.
 * Una vez sabemos de qué va la consulta (urgencia, síntomas, receta, offtopic),
 * dirigimos la petición a un sub-agente (LLM) configurado con el System Prompt
 * específico y las herramientas exactas que necesita para ese caso.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingWorkFlowServiceImpl implements  RoutingWorkFlowService {

    private final ClientResolver clientResolver;
    private final AnalysisService analysisService;
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    // Mantener los prompts fuera del código Java permite editarlos sin tener que recompilar el proyecto.
    @Value("classpath:prompts/routing-emergency.st")
    private Resource emergencyResource;

    @Value("classpath:prompts/routing-symptom.st")
    private Resource symptomResource;

    @Value("classpath:prompts/routing-general.st")
    private Resource generalResource;

    @Value("classpath:prompts/routing-prescription.st")
    private Resource prescriptionResource;

    private PromptTemplate emergencyTemplate;
    private PromptTemplate symptomTemplate;
    private PromptTemplate generalTemplate;
    private PromptTemplate prescriptionTemplate;

    @PostConstruct
    void init() {
        emergencyTemplate = new PromptTemplate(emergencyResource);
        symptomTemplate = new PromptTemplate(symptomResource);
        generalTemplate = new PromptTemplate(generalResource);
        prescriptionTemplate = new PromptTemplate(prescriptionResource);
    }

    /**
     * Punto de entrada del Router Workflow.
     * Evalúa, clasifica y delega al sub-agente correspondiente.
     */
    @Override
    public String routeQuery(String query, String model, Long userId) {
        // 1. Fase de Evaluación: El modelo clasifica el mensaje entrante.
        QueryClassification classification = analysisService.classifyQuery(query, model, userId);

        log.info("Router - tipo: {} - razón: {}", classification.type(), classification.reason());

        // 2. Fase de Enrutamiento: Switch Expression para delegar la tarea.
        return switch (classification.type()) {
            case SYMPTOM_REPORT -> handleSymptomReport(query,model,userId);
            case GENERAL_QUESTION -> handleGeneralQuestion(query,model,userId);
            case EMERGENCY -> handleEmergency(query, model, userId);
            case PRESCRIPTION_REQUEST -> handlePrescriptionRequest(query, model, userId);
            case OFF_TOPIC -> handleOffTopic(userId);
        };
    }

    // --- Sub-Agentes ---

    private String handleEmergency(String query, String model, Long userId) {
        log.info("Ruta: EMERGENCY");
        String message = emergencyTemplate.render(Map.of("consulta", query));

        // Por cada petición se instancia un ChatClient limpio usando create().
        // Esto evita compartir estado entre usuarios.
        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un especialista en urgencias médicas, responde de forma breve y directa.")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId))
                )
                .call()
                .content();
    }

    private String handleSymptomReport(String query, String model, Long userId) {
        log.info("Ruta: SYMPTOM_REPORT");
        String message = symptomTemplate.render(Map.of("consulta", query));
        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un especialista en análisis de síntomas")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handleGeneralQuestion(String query, String model, Long userId) {
        log.info("Ruta: GENERAL_QUESTION");
        String message = generalTemplate.render(Map.of("consulta", query));
        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un médico educador, responde con detalle y claridad")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handlePrescriptionRequest(String query, String model, Long userId) {
        log.info("Ruta: PRESCRIPTION_:REQUEST");
        String message = prescriptionTemplate.render(Map.of("consulta", query));
        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un farmacólogo clínico informas sobre medicamentos sin prescribir")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId))
                )
                .call()
                .content();
    }

    /**
     * Vía de escape determinística.
     * Si la consulta es OFF_TOPIC (ej: pedir recetas de cocina a un bot médico),
     * devolvemos texto quemado en código (hardcoded) para ahorrar tokens y costes del LLM.
     */
    private String handleOffTopic(Long userId) {
        log.info("Ruta: OFF_TOPIC - userId: {} - respuesta determinística sin llamada al modelo", userId);
        return "lo siento, solo puedo ayudarte con consultas relacionadas con salud y medicina." +
                "si tienes una consulta médica, no dudes en escribirme.";
    }
    

}
