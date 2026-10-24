package com.example.medassistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;



/**
 * Interceptor personalizado para auditar y medir las consultas médicas.
 * Funciona de forma parecida a un filtro en Spring: envuelve cada petición al modelo
 * para dejar registro en los logs de quién pregunta, qué pregunta y cuánto tarda
 * en responder, manteniendo los controladores y servicios limpios de esta lógica.
 */
@Component
@Slf4j
public class MedicalAuditAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * Se ejecuta en las llamadas normales (síncronas), donde esperamos a que
     * la respuesta esté completa antes de devolverla al usuario.
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // Extraemos el texto que ha enviado el usuario.
        String userMessage = request.prompt().getUserMessage().getText();

        // Obtenemos el ID de la conversación del contexto.
        String conversationId = (String) request.context()
                .getOrDefault(ChatMemory.CONVERSATION_ID, "unknown");

        log.info("AUDIT | conversationId={} | pregunta={}",
                conversationId, userMessage);

        // Medimos el tiempo que tarda en procesarse toda la cadena.
        long start = System.currentTimeMillis();

        //Pasamos la petición al siguiente paso de la cadena y esperamos la respuesta
        ChatClientResponse response = chain.nextCall(request);
        long elapsed = System.currentTimeMillis() - start;

        //Sacamos el texto de la respuesta y lo recortamos a 100 caracteres para no saturar la consola de logs
        String content = response.chatResponse()
                .getResult().getOutput().getText();
        String preview = content != null && content.length() >100
                ? content.substring(0, 100) + "..."
                : content;
        log.info("AUDIT | conversationId={} | tiempo={}ms |respuesta={}",
                conversationId, elapsed, preview);

        return response;
    }

    /**
     * Se ejecuta en las llamadas en tiempo real (streaming con Flux).
     * Aquí registramos la entrada antes de que empiece a fluir la respuesta palabra por palabra.
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {

        String userMessage = request.prompt().getUserMessage().getText();
        String conversationId = (String) request.context()
                .getOrDefault(ChatMemory.CONVERSATION_ID, "unknown");

        log.info("AUDIT STREAM| conversationId={} | pregunta={}",
                conversationId, userMessage);

        // Continuamos el flujo reactivo sin bloquearlo
        return chain.nextStream(request);
    }

    @Override
    public String getName() {
        return "MedicalAuditAdvisor";
    }


    /**
     * Define el orden de ejecución cuando hay varios Advisors encadenados (como el de memoria o el de RAG).
     * Al darle la prioridad más alta (HIGHEST_PRECEDENCE), nos aseguramos de que este
     * sea el primero en ejecutarse al entrar la petición y el último al salir,
     * midiendo así el tiempo total real de todo el proceso.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
