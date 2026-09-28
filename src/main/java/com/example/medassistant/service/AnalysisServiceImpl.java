package com.example.medassistant.service;

import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.dto.analysis.ConditionSummary;
import com.example.medassistant.dto.analysis.SymptomAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * Identifica y clasifica las condiciones clínicas más probables basadas en síntomas.
     * Utiliza ParameterizedTypeReference para preservar el tipo genérico List<ConditionSummary>
     * en runtime y permitir a Spring AI deserializar
     * un array JSON directamente en una colección fuertemente tipada.
     */
    @Override
    public List<ConditionSummary> ListRelatedConditions(String symptoms, String model) {
        log.info("Listado de condiciones relacionadas - modelo: {} ", model);
        return clientResolver.resolve(model)
                .prompt()
                .user("Pasa en lista las 3 condiciones médicas más probables " +
                                "para estos síntomas: " + symptoms
                                )
                .call()
                .entity(new ParameterizedTypeReference<>() {});
    }

    @Override
    public SymptomAnalysis analyzeSymptoms(String symptoms, String model) {
        log.info("Análisis de síntomas - modelo: {}", model);

        return clientResolver.resolve(model)
                .prompt()
                .user("Analiza los siguientes síntomas de un paciente y " +
                        "proporciona un análisis médico educativo completo: " + symptoms)
                .call()
                .entity(SymptomAnalysis.class);


    }
}
