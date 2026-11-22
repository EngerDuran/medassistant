package com.example.medassistant.service;

import com.example.medassistant.dto.IngestionResult;
import org.springframework.core.io.Resource;

import java.io.IOException;

public interface DocumentIngestionService {

    IngestionResult ingestNewDocument(Resource resource);
}
