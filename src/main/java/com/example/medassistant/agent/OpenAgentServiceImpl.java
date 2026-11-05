package com.example.medassistant.agent;

import com.example.medassistant.config.ClientResolver;
import com.example.medassistant.tools.AppointmentSearchTool;
import com.example.medassistant.tools.DoctorInfoTool;
import com.example.medassistant.tools.DrugInfoTool;
import com.example.medassistant.tools.PatientInfoTool;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;


/**
 * Agente Autónomo, Open Agent.
 * A diferencia del 'Routing' o el 'Orchestrator' donde controlamos el flujo por código,
 * aquí le damos total libertad al LLM. Le entregamos varias herramientas Tools y
 * él decide autónomamente cuáles ejecutar, en qué orden, y con qué parámetros,
 * iterando hasta obtener la respuesta final para el usuario.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAgentServiceImpl implements  OpenAgentService {

    private final ClientResolver clientResolver;
    private final PatientInfoTool patientInfoTool;
    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final DrugInfoTool drugInfoTool;

    @Value("classpath:prompts/open-agent.st")
    private Resource agentResource;

    private PromptTemplate agentTemplate;

    @PostConstruct
    void init() {
        agentTemplate = new PromptTemplate(agentResource);
    }

    @Override
    public String execute(String query, String model, Long userId) {

        log.info("Agente abierto - query: {}", query);

        // Grounding temporal: Inyectamos la fecha del sistema en el prompt.
        // Es vital para que el LLM no alucine con las fechas cuando el paciente
        // pide un turno para mañana o la próxima semana.
        String systemPrompt = agentTemplate.render(Map.of(
                "fechaActual", LocalDate.now().toString()
        ));

        // Instanciamos un ChatClient limpio por petición (Prototype)
        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system(systemPrompt)
                .user(query)
                // Habilitamos el Tool Calling pasándole las funciones Java que puede invocar
                .tools(patientInfoTool, appointmentSearchTool, doctorInfoTool, drugInfoTool)

                // SEGURIDAD CRÍTICA
                // No le pedimos al LLM que deduzca el ID del usuario, ni se lo pasamos en el texto.
                // Lo inyectamos directamente en el contexto de ejecución de las herramientas.
                // Esto previene ataques de Prompt Injection donde un atacante intente acceder
                // a datos de otro paciente. Las @Tool leerán el userId de aquí.
                .toolContext(Map.of("userId", userId, "role", "PATIENT"))
                .call()
                .content();

        log.info("Agente abierto - respuesta generada");

        return result;
    }
}
