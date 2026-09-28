package com.example.medassistant.controller;

import com.example.medassistant.dto.ChatRequest;
import com.example.medassistant.dto.analysis.ConditionSummary;
import com.example.medassistant.dto.analysis.QueryClassification;
import com.example.medassistant.dto.analysis.SymptomAnalysis;
import com.example.medassistant.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/condition")
    public ResponseEntity<ConditionSummary> analyzeCondition(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(analysisService.summarizeCondition(request.prompt(), request.model()));

    }

    @PostMapping("/conditions")
    public ResponseEntity<List<ConditionSummary>> listConditions(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(analysisService.ListRelatedConditions(request.prompt(), request.model()));
    }

    @PostMapping("/symptoms")
    public ResponseEntity<SymptomAnalysis> analyzeSymptoms(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(analysisService.analyzeSymptoms(request.prompt(), request.model()));
    }

    @PostMapping("/classify")
    public ResponseEntity<QueryClassification> classifyQuery(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(analysisService.classifyQuery(request.prompt(), request.model()));
    }

}
