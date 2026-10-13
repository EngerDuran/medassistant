package com.example.medassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    /**
     * Cliente HTTP moderno (reemplazo de RestTemplate).
     * Configurado con la URL base de la FDA para facilitar las llamadas.
     */
    @Bean
    public RestClient openFdaClient() {
        return RestClient.builder()
                .baseUrl("https://api.fda.gov/")
                .build();
    }
}
