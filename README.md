# MedAssistant

Asistente médico conversacional construido con Spring Boot y Spring AI: chat con memoria, RAG sobre documentos clínicos, tool calling, agentes autónomos, orquestación de workers y soporte MCP (cliente y servidor).

> **Aviso**: proyecto educativo. La información que genera esta aplicación no sustituye en ningún caso el consejo, diagnóstico o tratamiento de un profesional médico.

## Stack

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje base |
| Spring Boot | 4.1.1 | Framework principal |
| Spring AI | 2.0.1 (BOM) | Chat, RAG, tools, MCP |
| Spring Security (OAuth2 Resource Server) | — | JWT con Keycloak |
| Spring Data JPA | — | Persistencia relacional |
| PostgreSQL | 17 (Docker) | Base de datos de la app |
| ChromaDB | imagen `chromadb/chroma` | Vector store en local |
| pgvector (`pgvector/pgvector:pg17`) | pg17 | Vector store en AWS |
| Keycloak | 26.2.5 (Docker) | Servidor de identidad |
| Google GenAI | `gemini-3.1-flash-lite`, `gemini-embedding-001` | LLM y embeddings (perfil `gemini`/`aws`) |
| Ollama | `llama3.2:3b`, `nomic-embed-text` | LLM y embeddings locales (perfil `ollama`) |
| Apache Tika (`spring-ai-tika-document-reader`) | — | Extracción de texto de PDFs |
| Spring AI MCP Client / Server (WebMVC) | — | Integración MCP |
| Lombok | — | Reducción de boilerplate |

## Funcionalidades

- **Chat con memoria**: memoria de conversación con ventana deslizante (`MessageWindowChatMemory`, máximo 9 mensajes) persistida vía repositorio JDBC. Respuestas síncronas (`POST /api/v1/chat`) y en streaming por SSE (`POST /api/v1/chat/stream`).
- **RAG sobre documentos médicos**: al arrancar, ingesta automática de 3 PDFs de `src/main/resources/docs` (farmacología del ibuprofeno, guía de hipertensión, protocolo cardiovascular) con Tika + `TokenTextSplitter` (chunks de ~800 tokens). Recuperación con `QuestionAnswerAdvisor` (topK=3, umbral de similitud 0.7) contra ChromaDB en local (colecciones separadas por proveedor de embeddings) o pgvector en AWS.
- **Ingesta de documentos en caliente**: `POST /api/v1/documents/ingest` permite subir nuevos PDFs al conocimiento RAG sin reiniciar (destinado a rol ADMIN para evitar data poisoning en la base vectorial).
- **Tools (tool calling)**:
  - `getPatientInfo`: historial clínico, con control de acceso en `ToolContext` (el `userId` inyectado por el backend debe coincidir con el `patientId` solicitado).
  - `searchAppointments`: búsqueda de turnos disponibles por especialidad y fecha.
  - `bookAppointment`: reserva de turnos con doble barrera de seguridad por rol (`ADMIN`).
  - `searchDoctors`: búsqueda de médicos por apellido o especialidad.
  - `getDrugInfo`: información de medicamentos consultando la API pública OpenFDA (textos recortados para no saturar el contexto del LLM).
- **Agentes**:
  - **Chain de turnos**: pipeline extracción → búsqueda → selección con patrón Human-in-the-Loop (confirmación explícita del turno, revalidación de disponibilidad contra la BD y cancelación).
  - **Router Workflow**: un clasificador de intención enruta a sub-agentes especializados (urgencia, síntomas, consulta general, prescripción) con prompts dedicados; las consultas off-topic se responden de forma determinística.
  - **Orchestrator-Workers**: tres workers especializados (clínico, historial del paciente, bibliografía/RAG) + síntesis final, con endpoint SSE de progreso en tiempo real.
  - **Open Agent**: el LLM decide autónomamente qué tools invocar, en qué orden y con qué parámetros, con la identidad del usuario inyectada en el `ToolContext` (anti prompt-injection).
- **MCP**:
  - **Cliente**: conexiones stdio configuradas (filesystem, GitHub, engram) y endpoint de demo `POST /api/v1/mcp/demo` (no disponible en el perfil `aws`) para conversar con las tools expuestas por servidores MCP externos.
  - **Servidor**: expone la tools `search-doctors` y `search-appointments`, el resource `doctor://{specialty}` y el prompt `symptom-consultation` a clientes externos (Claude Desktop, Cursor, etc.). Configurado como `medassistant-mcp`, protocolo `STATELESS`.
- **Seguridad OAuth2 con Keycloak**: API stateless con JWT; los roles de `realm_access.roles` se traducen a authorities de Spring (`ROLE_PATIENT`, `ROLE_ADMIN`). Todo `/api/v1/**` requiere rol `PATIENT` o `ADMIN`; `/mcp/**` es público.
- **Advisor de auditoría**: `MedicalAuditAdvisor` intercepta cada llamada (síncrona y streaming) y registra en logs la conversación, la pregunta, el tiempo de respuesta y un preview de la respuesta, sin ensuciar controladores ni servicios.

## Arquitectura

Aplicación en capas: controladores REST → servicios → agentes y tools → `ChatClient` de Spring AI, con la configuración de proveedores resuelta por perfiles (interfaz `ClientResolver` con implementaciones para local y AWS). La persistencia combina PostgreSQL para datos relacionales (pacientes, médicos, turnos, memoria de chat) y un vector store para RAG: ChromaDB en local (colección por proveedor de embeddings para no mezclar espacios vectoriales) o pgvector sobre RDS en AWS. Los prompts viven como plantillas StringTemplate en `src/main/resources/prompts`, desacoplados del código Java. La misma lógica de negocio se reutiliza para dos mundos: tool calling interno de Spring AI (`@Tool`) y exposición externa vía MCP (`@McpTool`, `@McpResource`, `@McpPrompt`).

## Perfiles

| Perfil | Qué hace |
|---|---|
| Base (sin perfil) | Configura Postgres local, ChromaDB (`localhost:8000`), Keycloak local y las conexiones MCP stdio (filesystem, GitHub, engram). |
| `gemini` | Activa Google GenAI como proveedor de chat y embeddings (requiere `GOOGLE_AI_API_KEY`). |
| `ollama` | Activa Ollama local (`localhost:11434`) con `llama3.2:3b` y `nomic-embed-text`. |
| `aws` | Producción: RDS PostgreSQL + pgvector (sustituye Chroma), MCP cliente deshabilitado, autoconfiguración de Ollama/Chroma excluida, esquema de BD vía `schema-aws.sql` (crea la extensión `vector`) y `ddl-auto: update`. Solo admite el modelo `gemini`. |

## Cómo ejecutarlo en local

1. **Levanta la infraestructura**:

   ```bash
   docker compose up -d
   ```

   Levanta PostgreSQL de la app (5432), PostgreSQL de Keycloak (5433), Keycloak (8090) y ChromaDB (8000). Las credenciales del compose (`postgres/postgres`, `keycloak/keycloak`, `admin/admin`) son **solo para desarrollo**.

2. **Prepara Keycloak**: el repositorio incluye `medassistant-realm.json` con la definición del realm (roles `PATIENT` y `ADMIN`). TODO: los pasos de importación no están automatizados en `docker-compose.yml`.

3. **Opcional (perfil `ollama`)**: instala y arranca Ollama con los modelos configurados; no viene incluido en el compose.

4. **Variables de entorno necesarias** (solo nombres; nunca valores):
   - `GOOGLE_AI_API_KEY` — perfil `gemini` y `aws`.
   - `GITHUB_TOKEN` — conexión MCP de GitHub en el entorno local.
   - Perfil `aws`: `RDS_HOST`, `RDS_PORT`, `RDS_DB`, `RDS_USERNAME`, `RDS_PASSWORD`, `KEYCLOAK_ISSUER_URI`.

5. **Arranca la aplicación**:

   ```bash
   SPRING_PROFILES_ACTIVE=gemini ./mvnw spring-boot:run
   ```

   (o `ollama` en lugar de `gemini`; también válido `-Dspring-boot.run.profiles=gemini`).

   Nota: las conexiones MCP stdio lanzan los binarios locales configurados en `application.yaml`; si no tienes instalados los servidores MCP (filesystem, GitHub, engram), ajusta o deshabilita esas conexiones para ese entorno.

## Endpoints principales

Todas las rutas `/api/v1/**` requieren JWT válido con rol `PATIENT` o `ADMIN`. El cuerpo de los POST es `{ "prompt": "...", "model": "gemini" | "ollama" }` (`ChatRequest`); el `userId` se extrae siempre del token, nunca del body.

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/v1/chat` | Chat síncrono con memoria. |
| POST | `/api/v1/chat/stream` | Chat en streaming SSE. |
| POST | `/api/v1/chat/explain` | Explica una condición médica con lenguaje accesible. |
| POST | `/api/v1/chat/symptoms` | Orienta sobre síntomas descritos. |
| POST | `/api/v1/chat/diagnose` | Razonamiento clínico con chain-of-thought. |
| POST | `/api/v1/chat/consult` | Consulta médica completa. |
| POST | `/api/v1/analysis/condition` | Resumen estructurado de una condición (`ConditionSummary`). |
| POST | `/api/v1/analysis/conditions` | Lista de condiciones relacionadas con una consulta. |
| POST | `/api/v1/analysis/symptoms` | Análisis estructurado de síntomas (`SymptomAnalysis`). |
| POST | `/api/v1/analysis/classify` | Clasifica la consulta por tipo (`QueryClassification`). |
| POST | `/api/v1/documents/ingest` | Sube un PDF (multipart) e ingesta sus chunks en el vector store. Destinado a rol `ADMIN`. |
| POST | `/api/v1/agent/chain/appointment` | Reserva de turno paso a paso (extracción → búsqueda → selección). |
| POST | `/api/v1/agent/chain/appointment/confirm` | Confirma el turno pendiente (Human-in-the-Loop, revalida disponibilidad). |
| POST | `/api/v1/agent/chain/appointment/cancel` | Descarta el turno pendiente. |
| POST | `/api/v1/agent/routing` | Router Workflow: clasifica y delega al sub-agente adecuado. |
| POST | `/api/v1/agent/orchestrator` | Orchestrator-Workers: análisis clínico + historial + bibliografía + síntesis. |
| POST | `/api/v1/agent/orchestrator/progress` | Igual que el anterior con progreso en vivo por SSE. |
| POST | `/api/v1/agent/open` | Agente autónomo con tool calling libre. |
| POST | `/api/v1/mcp/demo` | Demo de conversación usando las tools de servidores MCP externos (solo perfil distinto de `aws`). |
| — | `/mcp/**` | Servidor MCP (tools, resources y prompts expuestos a clientes externos). Público. |

## Despliegue en AWS

Verificable en el repositorio:

- **Dockerfile multi-stage**: compilación con `maven:3.9-eclipse-temurin-21` (cacheo de dependencias con `dependency:go-offline`) y runtime ligero `eclipse-temurin:21-jre-jammy`, ejecutando como usuario no-root (`spring`), con TZ `Europe/Madrid`, `SPRING_PROFILES_ACTIVE=aws` por defecto, puerto 8080 y `-XX:MaxRAMPercentage=75.0` (pensado para instancias EC2 ajustadas de RAM).
- **Perfil `aws`**: RDS PostgreSQL parametrizado por variables `RDS_*`; el vector store pasa de ChromaDB a **pgvector** (`PgVectorStore`, 3072 dimensiones, tabla `medassistant_google`); `schema-aws.sql` crea la extensión `vector`; el cliente MCP stdio queda deshabilitado.
- **`docker-compose.aws-test.yml`**: permite probar el perfil `aws` en local levantando PostgreSQL con pgvector (puerto 5434) en lugar de RDS.

TODO (no verificable solo con el código de este repositorio): topología exacta de despliegue (EC2/ECS, balanceador, grupos de seguridad), aprovisionamiento del RDS, pipeline de CI/CD y publicación de imágenes.

## Autor

**Enger Durán**
