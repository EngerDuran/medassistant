package com.example.medassistant.agent.orchestrator;

import com.example.medassistant.config.ClientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Servicio central del patrón Orchestrator-Workers.
 * Actúa como un "director de orquesta". En lugar de pedirle a un solo LLM que
 * haga todo (lo cual genera alucinaciones y respuestas pobres), este servicio
 * divide el problema, delega tareas específicas a los Workers y luego consolida
 * los resultados en una única respuesta estructurada y coherente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService {

    private final ClientResolver clientResolver;

    // Inyectamos a los "empleados" especialistas
    private final ClinicalWorker clinicalWorker;
    private final HistoryWorker historyWorker;
    private final BibliographyWorker bibliographyWorker;

    @Value("classpath:prompts/orchestrator-synthesis.st")
    private Resource synthesisResource;

    private PromptTemplate synthesisTemplate;

    @PostConstruct
    void init() {
        this.synthesisTemplate = new PromptTemplate(synthesisResource);
    }

    /**
     * Punto de entrada principal. Coordina el flujo de trabajo completo.
     */
    @Override
    public String orchestrate(String symptoms, String model, Long userId) {
        log.info("Orquestador - iniciando análisis integral");

        // Delegación de tareas (Fan-out)
        // Pedimos a cada Worker que haga su trabajo.
        // (Nota para entrevistas: ahora mismo es secuencial, pero en un entorno
        // de altísimo tráfico se podrían ejecutar de forma asíncrona/paralela con CompletableFuture).
        String clinicalAnalysis = clinicalWorker.analyze(symptoms, model);
        String patientHistory = historyWorker.lookup(userId);
        String bibliographyResearch = bibliographyWorker.research(symptoms, model);

        log.info("Orquestador - los tres workers completaron su trabajo");

        //Consolidación (Fan-in)
        return synthesize(clinicalAnalysis, patientHistory, bibliographyResearch, symptoms, model);
    }

    /**
     * Toma los tres informes independientes y le pide a un LLM final que redacte
     * un diagnóstico unificado usando exclusivamente esa información.
     */
    private String synthesize(String clinicalAnalysis, String patientHistory,
                              String bibliographyResearch, String symptoms, String model) {

        // Inyectamos el trabajo de los Workers en el prompt final
        String prompt = synthesisTemplate.render(Map.of(
                "analisisClinico", clinicalAnalysis,
                "historial", patientHistory,
                "bibliografia", bibliographyResearch,
                "sintomas", symptoms
        ));

        // Instanciamos un nuevo ChatClient para evitar contaminación de contexto
        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un médico jefe que integra informes de múltiples departamentos para dar un diagnóstico final.")
                .user(prompt)
                .call()
                .content();

        log.info("Orquestador - síntesis completada");
        return result;
    }
}