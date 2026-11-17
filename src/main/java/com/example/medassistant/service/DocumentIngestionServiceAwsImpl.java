package com.example.medassistant.service;

import com.example.medassistant.dto.IngestionResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("aws")
@Slf4j
public class DocumentIngestionServiceAwsImpl implements DocumentIngestionService{

    private final VectorStore googleVectorStore;

    @Value("classpath:docs/farmacologia-ibuprofeno.pdf")
    private Resource ibuprofenoDoc;

    @Value("classpath:docs/guia-hipertension-medassistant.pdf")
    private Resource hipertensionDoc;

    @Value("classpath:docs/protocolo-atencion-cardiovascular.pdf")
    private Resource cardiovascularDoc;

    @PostConstruct
    public void ingest() {

        List<Document> existing = googleVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("documento médico")
                        .topK(1)
                        .similarityThreshold(0.0)
                        .build());

        if (!existing.isEmpty()) {
            log.info("Documentos ya presentes en PgVector - omitiendo ingestión");
            return;
        }

        log.info("Iniciando ingestión de documentos médicos...");
        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(readDocument(ibuprofenoDoc));
        allDocuments.addAll(readDocument(hipertensionDoc));
        allDocuments.addAll(readDocument(cardiovascularDoc));

        // Dividimos el texto en bloques (chunks) de unos 800 tokens.
        // Hacemos esto porque no es eficiente pasarle un PDF entero al modelo de IA
        // al cortarlo en fragmentos pequeños, las búsquedas son más rápidas, precisas y además ahorramos tokens.
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMaxNumChunks(1000)
                .build();

        List<Document> chunks = splitter.apply(allDocuments);

        log.info("Documentos divididos en {} fragmentos", chunks.size());

        // Al hacer .add(), Spring AI convierte cada fragmento de texto en números = embeddings
        // por debajo y los guarda en su respectiva colección de PgVector.
        log.info("Ingiriendo en collection google...");
        googleVectorStore.add(chunks);
    }

    // Inyectamos el vector VectorStores usando @Qualifier para que Spring
    // sepa distinguir.
    public DocumentIngestionServiceAwsImpl(
            @Qualifier("googleVectorStore") VectorStore googleVectorStore) {
        this.googleVectorStore = googleVectorStore;
    }

    @Override
    public IngestionResult ingestNewDocument(Resource resource) {
        List<Document> documents = readDocument(resource);

        // Troceamos el PDF en fragmentos de 800 tokens para no saturar
        // la ventana de contexto de Gemini/Ollama durante las búsquedas vectoriales.
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMaxNumChunks(1000)
                .build();

        List<Document> chunks = splitter.apply(documents);

        log.info("Nuevo documento '{}: {} fragemntos",
                resource.getFilename(), chunks.size());

        // Guardamos los embeddings tanto en la BD local como en la nube
        googleVectorStore.add(chunks);

        return new IngestionResult(resource.getFilename(),chunks.size());
    }

    private List<Document> readDocument(Resource resource) {
        log.info("Leyendo Documento: {}", resource.getFilename());
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }

}
