package com.example.medassistant.service;

import reactor.core.publisher.Flux;

public interface AssistantService {
   String chat(String prompt, String model);

   Flux<String> chatStream(String prompt, String model);
   String explainCondition(String condition, String model);
}
