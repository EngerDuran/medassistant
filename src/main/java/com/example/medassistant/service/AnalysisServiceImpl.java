package com.example.medassistant.service;

import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.dto.analysis.ConditionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisServiceImpl implements  AnalysisService {

    private final ClientResolver clientResolver;

    /**
     * Solicita al LLM un análisis clínico educativo y mapea directamente
     * la respuesta deserializada a una instancia inmutable de ConditionSummary
     * mediante la API fluida .entity() de Spring AI.
     */

    @Override
    public ConditionSummary summarizeCondition(String condition, String model) {
        log.info("Análisis estruturado de condición:{}, modelo:{}", condition, model);

        return clientResolver.resolve(model)
                .prompt()
                .user("Proporciona un resumen médico educativo sobre: " + condition)
                .call()
                .entity(ConditionSummary.class);
    }
}
