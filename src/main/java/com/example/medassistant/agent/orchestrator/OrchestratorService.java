package com.example.medassistant.agent.orchestrator;

import reactor.core.publisher.Flux;

public interface OrchestratorService {

    String orchestrate(String symptoms, String model, Long userId);
    Flux<String> orchestrateWithProgress(String symptoms, String model, Long userID);
}
