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
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

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

    /**
     * Orquestación con feedback en tiempo real (Server-Sent Events / SSE).
     * Problema: La llamada combinada a 3 Workers (IA + BD) y la síntesis final tarda bastante.
     * Si usamos un endpoint síncrono tradicional, el cliente se queda congelado esperando.
     * Solución: Envolvemos el flujo en un Flux reactivo. A medida que avanza el proceso,
     * emitimos eventos de texto (sink.next) para que la UI pueda mostrar los pasos en vivo,
     * y al final enviamos el diagnóstico completo.
     */
    @Override
    public Flux<String> orchestrateWithProgress(String symptoms, String model, Long userId) {

        // Flux.create nos permite construir un stream de datos a mano.
        // El sink actúa como un embudo por el que vamos empujando los mensajes hacia el cliente.
    return Flux.<String>create(sink -> {
        try {
            //Notificamos al frontend e iniciamos la tarea
            sink.next("[PROGRESO] Analizando síntomas clínicamente...");
            String clinicalAnalysis = clinicalWorker.analyze(symptoms, model);

            sink.next("[PROGRESO] Buscando historial del paciente...");
            String patientHistory = historyWorker.lookup(userId);

            //Consulta a la base de datos vectorial (ChromaDB)
            sink.next("[PROGRESO] Buscando bibliografía médica...");
            String bibliographyResearch = bibliographyWorker.research(symptoms, model);
            //Integración final
            sink.next("[PROGRESO] Integrando resultados de los tres especialistas...");
            String result = synthesize(clinicalAnalysis, patientHistory, bibliographyResearch, symptoms, model);

            // 5. Entregamos el diagnóstico final y cerramos el stream limpiamente
            sink.next(result);
            sink.complete();
        } catch (Exception e) {
            log.error("Orquestador (progreso) - error: {}", e.getMessage());
            // Si la IA falla (ej. error 503 por servidores saturados) pasamos la excepción
            // al stream para que la conexión se cierre con error y el Controller la maneje.
           sink.error(e);
        }
    })
            // DETALLE CRÍTICO DE ARQUITECTURA:
            // Como nuestros Workers ejecutan código bloqueante (llamadas HTTP clásicas o consultas a BD),
            // si no hacemos esto bloquearíamos el hilo principal del servidor.
            // boundedElastic() envuelve todo este bloque y lo ejecuta en un pool de hilos
            // diseñado específicamente para aislar tareas lentas o bloqueantes.



            .subscribeOn(Schedulers.boundedElastic());
    }
}