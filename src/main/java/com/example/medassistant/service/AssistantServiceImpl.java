package com.example.medassistant.service;
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
public class AssistantServiceImpl implements AssistantService {
    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;

    // Carga de la plantilla externa desde el classpath para desacoplar el prompt del código Java
    @Value("classpath:/prompts/explain-condition.st")
    private Resource explainConditionPrompt;

    private PromptTemplate explainConditionTemplate;

    // Inicializa el template de prompt tras completar la inyección de dependencias (@Value)
    @PostConstruct
    void init() {
        explainConditionTemplate = new PromptTemplate(explainConditionPrompt);
    }

    // Inyección de dependencias con @Qualifier: resuelve la ambigüedad indicando
    // a Spring qué bean concreto inyectar cuando existen varias implementaciones de ChatClient.
    public AssistantServiceImpl(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient

    ) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Utiliza Prompt Templating con variables en Spring AI.
     * Permite inyectar parámetros de forma limpia y segura evitando concatenación manual de cadenas.
     */

    @Override
    public String chat(String prompt, String model) {
        log.info("Chat request - modelo: {} ", model);


        return resolveCliente(model)
                .prompt(prompt).call().content();
    }

    @Override
    public Flux<String> chatStream(String prompt, String model) {
        log.info("Stream request - modelo: {} ", model);

        return resolveCliente(model)
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

        return resolveCliente(model)
                .prompt(message)
                .call()
                .content();
    }

    // usa el cliente de IA adecuado según el parámetro recibido en la petición,
    // usando Gemini por defecto.
    private ChatClient resolveCliente(String model){
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }
}






