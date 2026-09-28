package com.example.medassistant.service;

import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.dto.analysis.ConditionSummary;
import com.example.medassistant.dto.analysis.QueryClassification;
import com.example.medassistant.dto.analysis.SymptomAnalysis;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisServiceImpl implements  AnalysisService {

    private final ClientResolver clientResolver;

    private PromptTemplate structuredAnalysisTemplate;

    @Value("classpath:/prompts/structured-analysis.st")
    private Resource structuredAnalysisResource;

    @PostConstruct
    void init() {
        structuredAnalysisTemplate = new PromptTemplate(structuredAnalysisResource);
    }

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

        String message = structuredAnalysisTemplate.render(
                Map.of("sintomas", symptoms)
        );

        return clientResolver.resolve(model)
                .prompt()
                .user(message)
                .call()
                .entity(SymptomAnalysis.class);
    }

    @Override
    public QueryClassification classifyQuery(String query, String model) {
        log.info("Clasificación de consulta - modelo: {}", model);

        return clientResolver.resolve(model)
                .prompt()
                .user("Clasifica la siguiente consulta de un paciente. : " +
                        "determina que tipo de consulta es y explica por qué. \n\n" +
                        "Consulta del paciente: \"" + query + "\"")
                .call()
                .entity(QueryClassification.class);

    }
}
