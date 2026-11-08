package com.example.medassistant.controller;

import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.dto.ChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mcp")
@RequiredArgsConstructor
public class McpDemoController {

    private final ClientResolver clientResolver;
    private final ToolCallbackProvider mcpTools;

    @PostMapping("/demo")
    public ResponseEntity<String> demo(@Valid @RequestBody ChatRequest request) {
        String response = ChatClient.create(clientResolver.resolveModel(request.model()))
                .prompt()
                .user(request.prompt())
                .tools(mcpTools)
                .call()
                .content();
        return ResponseEntity.ok(response);
    }
}
