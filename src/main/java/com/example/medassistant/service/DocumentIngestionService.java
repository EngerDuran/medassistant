package com.example.medassistant.service;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface DocumentIngestionService {

    String ingestNewDocument(Resource resource);
}
