package com.example.medassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("aws")
public class ClientResolverAws implements ClientResolver {

    private final ChatClient geminiClient;
    private final ChatModel geminiModel;
    private final VectorStore googleVectorStore;

    public ClientResolverAws(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("googleGenAiChatModel") ChatModel geminiModel,
            @Qualifier("googleVectorStore") VectorStore googleVectorStore
    ) {
        this.geminiClient = geminiClient;
        this.geminiModel = geminiModel;
        this.googleVectorStore = googleVectorStore;
    }


    @Override
    public ChatClient resolve(String model) {
        assertGemini(model);
        return geminiClient;
    }

    @Override
    public ChatModel resolveModel(String model) {
        assertGemini(model);
        return geminiModel;
    }

    @Override
    public VectorStore resolveVectorStore(String model) {
        assertGemini(model);
        return  googleVectorStore;
    }

    private void assertGemini(String model) {
        String normalized = model == null ? "gemini" : model.strip().toLowerCase();

        if(!"gemini".equals(normalized)) {
            throw new IllegalArgumentException(
                    "Modelo no soportado en producción: '%s'. En AWS solo está disponible: gemini".formatted(model)
            );
        }
    }



}
