package com.example.medassistant;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class TestChatClient implements CommandLineRunner {

    private final ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception { //Test de prueba para ver si funciona la api-key

    String response = chatClient
            .prompt("¿Qué es la cervicalgia?")
            .call()
            .content();
        System.out.println(response);

    }
}
