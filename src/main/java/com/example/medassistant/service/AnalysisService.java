package com.example.medassistant.service;

import com.example.medassistant.dto.analysis.ConditionSummary;

public interface AnalysisService {
    ConditionSummary summarizeCondition(String condition, String model);
}
