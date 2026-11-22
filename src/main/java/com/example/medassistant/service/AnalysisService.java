package com.example.medassistant.service;

import com.example.medassistant.dto.analysis.ConditionSummary;
import com.example.medassistant.dto.analysis.QueryClassification;
import com.example.medassistant.dto.analysis.SymptomAnalysis;

import java.util.List;

public interface AnalysisService {
    ConditionSummary summarizeCondition(String condition, String model, long userId);
    List<ConditionSummary> ListRelatedConditions(String symptoms, String model, long userId);
    SymptomAnalysis analyzeSymptoms(String symptoms, String model, long userId);
    QueryClassification classifyQuery(String query, String model, long userId);
}
