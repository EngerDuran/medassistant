package com.example.medassistant.agent;


import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.dto.agent.AppointmentRequest;
import com.example.medassistant.service.AppointmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentChainServiceImpl  implements  AppointmentChainService {

    private final ClientResolver clientResolver;
    private final AppointmentService appointmentService;

    @Value("classpath:/prompts/appointment-extraction.st")
    private Resource extractionResource;

    @Value("classpath:/prompts/appointment-confirmation.st")
    private Resource confirmationResource;

    private PromptTemplate extractionTemplate;
    private PromptTemplate confirmationTemplate;

    @PostConstruct
    public void init() {
        extractionTemplate = new PromptTemplate(extractionResource);
        confirmationTemplate = new PromptTemplate(confirmationResource);
    }

    @Override
    public String bookAppointmentChain(String userRequest, String model, Long userId) {
        AppointmentRequest extracted = interpret(userRequest, model);
        return "";
    }

    private AppointmentRequest interpret(String userRequest, String model){

        String prompt = extractionTemplate.render(Map.of("pedido", userRequest));

        AppointmentRequest extracted = clientResolver.resolve(model)
                .prompt()
                .user(prompt)
                .call()
                .entity(AppointmentRequest.class);

        log.info("Chain paso 1 - extraído: specialty={}, date={}", extracted.specialty(), extracted.date());
        return  extracted;
    }
}
