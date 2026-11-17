package com.example.medassistant.dto;

public record IngestionResult(
        String filename,
        int fragmentsCount
) {
}
