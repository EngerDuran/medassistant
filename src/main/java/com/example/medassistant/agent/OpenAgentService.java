package com.example.medassistant.agent;

public interface OpenAgentService {
    String execute(String query, String model, Long userId);
}
