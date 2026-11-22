package com.example.medassistant.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.stringtemplate.v4.ST;

public interface ClientResolver {
    ChatClient resolve(String model);

    ChatModel resolveModel(String model);

    VectorStore resolveVectorStore(String model);
}
