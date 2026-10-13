package com.example.medassistant.service;

import com.example.medassistant.dto.DrugInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Servicio que conecta con la API oficial de la FDA.
 * Recupera los prospectos médicos y "limpia" los textos para no saturar
 * la ventana de contexto del LLM con datos innecesarios.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DrugInfoServiceImpl implements DrugInfoService {

    private final RestClient openFdaClient;

    // Límite para recortar textos legales gigantes y ahorrar tokens en la IA
    private static final int MAX_FIELD_LENGTH = 500;

    @Override
    public DrugInfo getDrugInfo(String drugName) {
        log.info("Consultando OpenFDA: drugName={}", drugName);

        var response = fetchFromOpenFda(drugName);
        return mapToDrugInfo(response, drugName);
    }

    /**
     * Extrae de forma segura el primer elemento de una lista dentro del JSON.
     * Si el texto supera los 500 caracteres, lo recorta para no saturar a la IA.
     */
    private String extractFirst(Map<?, ?> map, String key) {
        if (!(map.get(key) instanceof List<?> list) || list.isEmpty()){
            return "No disponible";
        }

        var value = list.getFirst().toString();

        return value.length() > MAX_FIELD_LENGTH
                ? value.substring(0, MAX_FIELD_LENGTH) + "..."
                : value;
    }

    /**
     * Ejecuta la petición HTTP usando el cliente RestClient (Spring Boot 3).
     */
    private Map<String, Object> fetchFromOpenFda(String drugName) {
        return openFdaClient.get()
                // Se inyecta la variable drugName en el placeholder {name}
                .uri("/drug/label.json?search=openfda.generic_name:{name}&limit=1", drugName)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * Navega por el árbol del JSON de la FDA (que suele ser muy anidado)
     * usando pattern matching para evitar ClassCastException o NullPointer.
     */
    private DrugInfo mapToDrugInfo(Map<String, Object> response, String drugName) {
        // Validamos que exista el nodo "results" y no esté vacío
        if(!(response.get("results") instanceof List<?> results) || results.isEmpty()){
            return null;
        }

        // Sacamos el primer medicamento de la lista
        if (!(results.getFirst() instanceof Map<?, ?> result)){
            return null;
        }

        // Extraemos el bloque de información general
        var openfda = result.get("openfda") instanceof Map<?, ?> m ? m : Map.of();

        return new DrugInfo(
                extractFirst(openfda, "brand_name"),
                extractFirst(openfda, "generic_name"),
                extractFirst(openfda, "purpose"),
                extractFirst(openfda, "warning"),
                extractFirst(openfda, "dosage_and_administration")
        );
    }
}


