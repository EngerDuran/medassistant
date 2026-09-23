package com.example.medassistant.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Prompt cannot be empty")
        String prompt,
        String model
) {
}
