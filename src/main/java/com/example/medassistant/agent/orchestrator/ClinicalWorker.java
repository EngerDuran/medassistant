package com.example.medassistant.agent.orchestrator;


import com.example.medassistant.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * Worker clínico (Sub-agente especializado).
 * Forma parte del patrón Orchestrator-Workers. Su única responsabilidad es
 * encargarse del razonamiento clínico. En lugar de tener un agente que haga todo mal,
 * el Orquestador le delega los síntomas a este Worker, que usa un prompt hiperespecífico
 * y devuelve su análisis para que el Orquestador ensamble la respuesta final.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClinicalWorker {
    private final ClientResolver clientResolver;

    @Value("classpath:prompts/worker-clinical.st")
    private Resource clinicalResource;

    private PromptTemplate clinicalTemplate;

    @PostConstruct
    public void init() {
        clinicalTemplate = new PromptTemplate(clinicalResource);
    }


    /**
     * Procesa los síntomas reportados por el paciente.
     * Instancia un ChatClient nuevo por cada llamada para garantizar que la memoria
     * y el contexto queden aislados y no se mezclen datos de distintos usuarios.
     */
    public String analyze(String symptoms, String model) {
        String prompt = clinicalTemplate.render(Map.of("symptoms", symptoms));

        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un especialista en razonamiento clínico")
                .user(prompt)
                .call()
                .content();

        log.info("Worker clínico - análisis completado {} ", result);

        return result;
    }
}
