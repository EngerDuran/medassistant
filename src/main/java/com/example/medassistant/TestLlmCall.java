package com.example.medassistant;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
@RequiredArgsConstructor
public class TestLlmCall implements CommandLineRunner {

    private final ChatModel chatModel;

    @Override
    public void run(String... args) throws Exception { //Test de prueba para ver si funciona la api-key

        ChatResponse chatResponse = chatModel.call(new Prompt("Explicame en dos párrafos la cervicalgia"));

        String content = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();
        System.out.println("=== RESPUESTA ===");
        System.out.println(content);
        System.out.println("=== FIN RESPUESTA ===");

        System.out.println("\n=== METADATA ===\n");

        System.out.println("Modelo: " +
                chatResponse.getMetadata().getModel());

        System.out.println("Tokens de entrada: " +
                chatResponse.getMetadata().getUsage().getPromptTokens());

        System.out.println("Tokens de salida: " +
                chatResponse.getMetadata().getUsage().getCompletionTokens());

        System.out.println("Tokens totales: " +
                chatResponse.getMetadata().getUsage().getTotalTokens());

        System.out.println("Finish reason: " +
                chatResponse.getResult().getMetadata().getFinishReason());

    }
}
