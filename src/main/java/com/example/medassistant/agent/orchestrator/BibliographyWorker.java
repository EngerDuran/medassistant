package com.example.medassistant.agent.orchestrator;

import com.example.medassistant.config.ClientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Worker encargado de la investigación bibliográfica (Motor RAG).
 * En el patrón Orchestrator-Workers, este es el ratón de biblioteca.
 * Recibe los síntomas, busca en nuestra base de datos vectorial ChromaDB
 * documentación médica real ingerida previamente, y obliga al LLM a fundamentar
 * su respuesta estrictamente en esos textos para evitar alucinaciones.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BibliographyWorker {

    private final ClientResolver clientResolver;

    @Value("classpath:prompts/worker-bibliography.st")
    private Resource bibliographyResource;

    private PromptTemplate bibliographyTemplate;

    @PostConstruct
    void init() {
        bibliographyTemplate = new PromptTemplate(bibliographyResource);
    }

    /**
     * Inyección de contexto y llamada al LLM.
     */

    private String interpret(List<Document> documents, String symptoms, String model) {
        // Usamos la API de Streams de Java para transformar la lista de Documentos en un solo bloque de texto.
        // Separamos cada documento con un doble salto de línea y guiones para estructurar el prompt de forma clara
        String documentContent = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = bibliographyTemplate.render(Map.of(
                "documentos", documentContent,
                "sintomas", symptoms
        ));

        // Instanciamos un ChatClient limpio para procesar los documentos sin arrastrar contexto basura
        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Eres un especialista en bibliografía médica")
                .user(prompt)
                .call()
                .content();

        log.info("Worker bibliografía - interpretación completada");
        return result;
    }

    /**
     Búsqueda de similitud semántica (Vector Search).
     * Convierte la consulta a un vector y busca coincidencias matemáticas en la BD.
     */
    private List<Document> searchDocuments(String symptoms, String model) {
        List<Document> documents = clientResolver.resolveVectorStore(model)
                .similaritySearch(
                        SearchRequest.builder()
                                .query(symptoms)
                                .topK(3) // Limitamos a los 3 fragmentos más relevantes para no saturar la ventana de contexto del LLM
                                .similarityThreshold(0.3) // Exigimos un 30% mínimo de coincidencia semántica para asegurar calidad
                                .build());

        log.info("Worker bibliografía - documentos encontrados: {}", documents.size());
        return documents;
    }

    /**
     * Punto de entrada principal del Worker.
     */
    public String research(String symptoms, String model){
        List<Document> documents = searchDocuments(symptoms, model);

        // Patrón Fail-Fast (Cláusula de guarda).
        // Si ChromaDB no devuelve información útil, cortamos la ejecución en seco.
        // Esto evita llamar a la API de Gemini sin contexto, ahorrando tokens, dinero y latencia.
        if (documents.isEmpty()) {
            return "No se encontraron documentos médicos relevantes para estos sintomas.";
        }

        return interpret(documents, symptoms, model);
    }

}
