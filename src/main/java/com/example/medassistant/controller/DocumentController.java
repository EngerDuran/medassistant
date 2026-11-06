package com.example.medassistant.controller;

import com.example.medassistant.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador para la gestión del conocimiento de la IA.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    /**
     * Control de Acceso Basado en Roles.
     * aquí restringimos el endpoint de ingesta. Solo un usuario con privilegios (ej. Médico Jefe / Admin)
     * Se pueden subir pdfs. Esto evita que usuarios malintencionados envenenen la base de datos
     * vectorial con información médica falsa (Data Poisoning).
     */

    @PostMapping("/ingest")
    @PreAuthorize("HasRole('ADMIN'")
    public ResponseEntity<String> ingestDocument(
        @RequestParam("file")MultipartFile file
    ){
        return ResponseEntity.ok(documentIngestionService.ingestNewDocument(file.getResource()));
    }
}
