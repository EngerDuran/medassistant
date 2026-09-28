package com.example.medassistant.service;

import com.example.medassistant.dto.analysis.ConditionSummary;
import com.example.medassistant.dto.analysis.SymptomAnalysis;

import java.util.List;

public interface AnalysisService {
    ConditionSummary summarizeCondition(String condition, String model);
    List<ConditionSummary> ListRelatedConditions(String symptoms, String model);
    SymptomAnalysis analyzeSymptoms(String symptoms, String model);
}
