package com.example.medassistant.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * Proveedor de Prompts, MCP Prompts.
 * En el ecosistema MCP, los Prompts son plantillas reutilizables.
 * A diferencia de los Resources que exponen datos o las Tools que ejecutan acciones,
 * los Prompts permiten estandarizar cómo los clientes externos formulan preguntas al sistema,
 * funcionando como atajos o comandos predefinidos.
 */
@Component
@Slf4j
public class SymptomConsultationPromptProvider {

    /**
     * Expone dinámicamente la plantilla al servidor MCP
     * El cliente externo descubrirá este prompt y sabrá que necesita solicitar
     * un argumento en este caso symptom al usuario para poder construir el mensaje.
     */

    @McpPrompt(
            name = "symptom-consultation",
            description = "Genera un mensaje estructurado para consultar sobre síntomas médicos"
    )
    public McpSchema.GetPromptResult symptomConsultation(
            @McpArg(
                    name = "symptom",
                    description = "Síntoma principal del paciente",
                    required = true
            ) String symptom
    ){
        // Usamos Text Blocks de Java para estructurar el prompt, que se vea limpio y legible.
        // Incluye un "System Prompt" implícito al final para mitigar riesgos médicos, esto es Responsabilidad Legal.
        String message = """
                Necesito orientación educativa sobre el siguiente síntoma: %s.
                
                Por favor incluye:
                - Posibles causas comunes
                -Cuando consultar a un profesional
                - Especialidad médica recomendada
                
                Recuerda que esta información es sólo educativa y no reemplaza
                una consulta médica presencial.
                """.formatted(symptom);

        // Empaquetamos el resultado en el esquema estándar de MCP
        return new McpSchema.GetPromptResult(
                "Consulta sobre síntomas ",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(message)))
        );
    }
}
