package com.example.medassistant.service;

import com.example.medassistant.config.ClientResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {
    private final ClientResolver clientResolver;

    // Carga de la plantilla externa desde el classpath para desacoplar el prompt del código Java
    @Value("classpath:/prompts/explain-condition.st")
    private Resource explainConditionPrompt;

    @Value("classpath:/prompts/symptom-analysis.st")
    private Resource symptomAnalysisPrompt;

    @Value("classpath:/prompts/diagnosis-cot.st")
    private Resource diagnosisCotResource;

    @Value("classpath:/prompts/consultation.st")
    private Resource consultationResource;

    private PromptTemplate explainConditionTemplate;

    private PromptTemplate symptomAnalysisTemplate;

    private PromptTemplate diagnosisCotTemplate;

    private PromptTemplate consultationTemplate;

    // Inicializa el template de prompt tras completar la inyección de dependencias (@Value)
    @PostConstruct
    void init() {
        consultationTemplate = new PromptTemplate(consultationResource);
        diagnosisCotTemplate = new PromptTemplate(diagnosisCotResource);
        symptomAnalysisTemplate = new PromptTemplate(symptomAnalysisPrompt);
        explainConditionTemplate = new PromptTemplate(explainConditionPrompt);

    }

    /**
     * Utiliza Prompt Templating con variables en Spring AI.
     * Permite inyectar parámetros de forma limpia y segura evitando concatenación manual de cadenas.
     */

    @Override
    public String chat(String prompt, String model) {
        log.info("Chat request - modelo: {} ", model);


        return clientResolver.resolve(model)
                .prompt(prompt).call().content();
    }

    @Override
    public Flux<String> chatStream(String prompt, String model) {
        log.info("Stream request - modelo: {} ", model);

        return clientResolver.resolve(model)
                .prompt(prompt)
                .stream()
                .content();
    }

    //Prompt Templating mediante .param() para inyectar variables de forma segura
    // en lugar de concatenar cadenas manuales, garantizando un tono clínico claro y accesible.
    @Override
    public String explainCondition(String condition, String model) {
        log.info("Explain request - condición: {}, modelo: {}", condition, model);

        String message = explainConditionTemplate.render(Map.of("condition", condition));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    /**
     * Evalúa los síntomas descritos inyectándolos en la plantilla symptom-analysis.st.
     * Estructura el prompt para guiar al LLM hacia un triaje preliminar responsable
     * sin emitir diagnósticos definitivos.
     */
    @Override
    public String analyzeSymptoms(String symptoms, String model) {
        log.info("Análisis de síntomas: {}, modelo: {}", symptoms, model);

        String message = symptomAnalysisTemplate.render(Map.of("sintomas", symptoms));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String diagnoseWithReasoning(String symptoms, String model) {
        log.info("Diagnostico CoT- modelo: {}", model);

        String message = diagnosisCotTemplate.render(Map.of("sintomas", symptoms));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String consult(String query, String model) {
        log.info("Consulta médica - modelo: {} ", model);

        String message = consultationTemplate.render(Map.of("consulta", query));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }
}






