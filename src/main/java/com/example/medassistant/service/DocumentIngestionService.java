package com.example.medassistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio encargado de cargar los documentos médicos en Chroma.
 * Se ocupa del proceso previo al RAG: lee los PDFs de la carpeta resources, los divide
 * en partes más pequeñas y los guarda como vectores para que la IA pueda consultarlos después.
 */
@Service
@Slf4j
public class DocumentIngestionService {

    private final VectorStore googleVectorStore;
    private final VectorStore ollamaVectorStore;

    @Value("classpath:docs/farmacologia-ibuprofeno.pdf")
    private Resource ibuprofenoDoc;

    @Value("classpath:docs/guia-hipertension-medassistant.pdf")
    private Resource hipertensionDoc;

    @Value("classpath:docs/protocolo-atencion-cardiovascular.pdf")
    private Resource cardiovascularDoc;

    // Inyectamos los dos VectorStores por separado usando @Qualifier para que Spring
    // sepa distinguir entre la colección de Google y la de Ollama.
    public DocumentIngestionService(
            @Qualifier("googleVectorStore") VectorStore googleVectorStore,
            @Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore) {
        this.googleVectorStore = googleVectorStore;
        this.ollamaVectorStore = ollamaVectorStore;
    }

    /**
     * Se ejecuta automáticamente una sola vez al arrancar la aplicación gracias a @PostConstruct.
     * Automatiza la lectura, división y guardado de los documentos en ChromaDB.
     */
    @PostConstruct
    public void ingest() {
        log.info("Iniciando ingestión de documentos médicos...");
        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(readDocument(ibuprofenoDoc));
        allDocuments.addAll(readDocument(hipertensionDoc));
        allDocuments.addAll(readDocument(cardiovascularDoc));

        // Dividimos el texto en bloques (chunks) de unos 800 tokens.
        // Hacemos esto porque no es eficiente pasarle un PDF entero al modelo de IA;
        // al cortarlo en fragmentos pequeños, las búsquedas son más rápidas, precisas y además ahorramos tokens.
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMaxNumChunks(1000)
                .build();

        List<Document> chunks = splitter.apply(allDocuments);

        log.info("Documentos divididos en {} fragmentos", chunks.size());

        // Al hacer .add(), Spring AI convierte cada fragmento de texto en números (embeddings)
        // por debajo y los guarda en su respectiva colección de ChromaDB.
        log.info("Ingiriendo en collection google...");
        googleVectorStore.add(chunks);

        log.info("Ingiriendo en collection ollama...");
        ollamaVectorStore.add(chunks);
    }

    /**
     * Método auxiliar que usa Apache Tika para extraer el texto plano de cada archivo PDF.
     */
    private List<Document> readDocument(Resource resource) {
        log.info("Leyendo Documento: {}", resource.getFilename());
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }
}
