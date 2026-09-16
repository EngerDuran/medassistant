package com.example.medassistant;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TestLlmCall implements CommandLineRunner {

    private final ChatModel chatModel;

    @Override
    public void run(String... args) throws Exception { //Test de prueba para ver si funciona la api-key
        String response = chatModel.call("Que es la fiebre?");
        System.out.println(response);

    }
}
