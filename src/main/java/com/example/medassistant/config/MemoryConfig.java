package com.example.medassistant.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del sistema de memoria a corto plazo para el asistente de IA.
 * Permite que el modelo mantenga el contexto de la conversación actual (estado)
 * dentro de una arquitectura REST que por naturaleza es stateless.
 */
@Configuration
public class MemoryConfig {

    /**
     * Define un bean de memoria en RAM utilizando una estrategia de "ventana deslizante".
     *  maxMessages(9) Optimiza el consumo de tokens y los costes de la API. En lugar de enviar todo
     * el historial infinito (lo que saturaría el Context Window del LLM y costaría más dinero),
     * solo conservamos las últimas 9 interacciones. Las más antiguas se descartan automáticamente.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(9)
                .build();
    }
}