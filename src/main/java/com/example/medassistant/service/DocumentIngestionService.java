package com.example.medassistant.service;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DocumentIngestionService {

    private final VectorStore googleVectorStore;
    private final VectorStore ollamaVectorStore;

    public DocumentIngestionService(
           @Qualifier("googleVectorStore") VectorStore googleVectorStore,
           @Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore) {
        this.googleVectorStore = googleVectorStore;
        this.ollamaVectorStore = ollamaVectorStore;
    }
}
