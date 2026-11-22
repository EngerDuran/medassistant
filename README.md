# MedAssistant

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.1-6DB33F?logo=spring&logoColor=white)

Asistente médico conversacional construido con Spring Boot y Spring AI: chat con memoria, RAG sobre documentos clínicos, tool calling, agentes autónomos, orquestación de workers y soporte MCP (cliente y servidor).

> **Aviso**: proyecto educativo. La información que genera esta aplicación no sustituye en ningún caso el consejo, diagnóstico o tratamiento de un profesional médico.

## Índice

- [Stack](#stack)
- [Funcionalidades](#funcionalidades)
- [Arquitectura](#arquitectura)
- [Cómo funcionan los agentes](#cómo-funcionan-los-agentes)
- [Flujo RAG](#flujo-rag)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Perfiles](#perfiles)
- [Cómo ejecutarlo en local](#cómo-ejecutarlo-en-local)
- [Endpoints](#endpoints)
- [Ejemplos de uso con curl](#ejemplos-de-uso-con-curl)
- [Decisiones técnicas](#decisiones-técnicas)
- [Despliegue en AWS](#despliegue-en-aws)
- [Qué aprendí](#qué-aprendí)
- [Próximos pasos](#próximos-pasos)
- [Autor](#autor)

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
- **RAG sobre documentos médicos**: ingesta automática de 3 PDFs de `src/main/resources/docs` (farmacología del ibuprofeno, guía de hipertensión, protocolo cardiovascular) y recuperación con `QuestionAnswerAdvisor` (topK=3, umbral de similitud 0.7) contra ChromaDB en local o pgvector en AWS. Ver [Flujo RAG](#flujo-rag).
- **Ingesta de documentos en caliente**: `POST /api/v1/documents/ingest` permite subir nuevos PDFs al conocimiento RAG sin reiniciar.
- **Tools (tool calling)**:
  - `getPatientInfo`: historial clínico, con control de acceso en `ToolContext` (el `userId` inyectado por el backend debe coincidir con el `patientId` solicitado).
  - `searchAppointments`: búsqueda de turnos disponibles por especialidad y fecha.
  - `bookAppointment`: reserva de turnos con doble barrera de seguridad por rol (`ADMIN`).
  - `searchDoctors`: búsqueda de médicos por apellido o especialidad.
  - `getDrugInfo`: información de medicamentos consultando la API pública OpenFDA (textos recortados para no saturar el contexto del LLM).
- **Agentes**: cuatro patrones implementados — chain de turnos con Human-in-the-Loop, router por tipo de consulta, Orchestrator-Workers y Open Agent. Ver [Cómo funcionan los agentes](#cómo-funcionan-los-agentes).
- **MCP**:
  - **Cliente**: conexiones stdio configuradas (filesystem, GitHub, engram) y endpoint de demo `POST /api/v1/mcp/demo` (no disponible en el perfil `aws`) para conversar con las tools expuestas por servidores MCP externos.
  - **Servidor**: expone las tools `search-doctors` y `search-appointments`, el resource `doctor://{specialty}` y el prompt `symptom-consultation` a clientes externos (Claude Desktop, Cursor, etc.). Configurado como `medassistant-mcp`, protocolo `STATELESS`.
- **Seguridad OAuth2 con Keycloak**: API stateless con JWT; los roles de `realm_access.roles` se traducen a authorities de Spring (`ROLE_PATIENT`, `ROLE_ADMIN`). Todo `/api/v1/**` requiere rol `PATIENT` o `ADMIN`; `/mcp/**` es público.
- **Advisor de auditoría**: `MedicalAuditAdvisor` intercepta cada llamada (síncrona y streaming) y registra en logs la conversación, la pregunta, el tiempo de respuesta y un preview de la respuesta, sin ensuciar controladores ni servicios.

## Arquitectura

```mermaid
flowchart LR
    UI[Cliente / Frontend]
    KC[Keycloak]
    MCPEXT[Clientes MCP externos<br/>Claude Desktop · Cursor]
    MCPSTD[Servidores MCP stdio<br/>filesystem · GitHub · engram]

    subgraph App[Spring Boot · medassistant]
        SEC[Seguridad JWT<br/>OAuth2 Resource Server]
        CTRL[Controllers REST<br/>/api/v1/**]
        SVC[Servicios y agentes<br/>chain · routing · orchestrator · open agent]
        CC[ChatClient<br/>Gemini / Ollama]
        AD[Advisors<br/>memoria JDBC · RAG · auditoría]
        TOOLS[Tools<br/>paciente · turnos · médicos · fármacos]
        MCPSRV[Servidor MCP<br/>/mcp/**]
        MCPCLI[Cliente MCP stdio]
        REPO[Repositorios JPA]
    end

    LLM[(Gemini / Ollama)]
    PG[(PostgreSQL)]
    VEC[(ChromaDB / pgvector)]

    UI -->|JWT Bearer| SEC
    KC -.->|emite y firma tokens| SEC
    SEC --> CTRL --> SVC --> CC
    CC --- AD
    CC -->|tool calling| TOOLS
    TOOLS -->|usan servicios de negocio| SVC
    SVC --> REPO --> PG
    AD -->|"búsqueda vectorial (RAG)"| VEC
    CC -->|HTTP| LLM
    MCPEXT -->|MCP| MCPSRV --> SVC
    SVC --> MCPCLI -->|stdio| MCPSTD
```

En capas: controladores REST → servicios → agentes y tools → `ChatClient` de Spring AI, con la configuración de proveedores resuelta por perfiles (interfaz `ClientResolver` con implementaciones para local y AWS). La persistencia combina PostgreSQL para datos relacionales (pacientes, médicos, turnos, memoria de chat) y un vector store para RAG: ChromaDB en local o pgvector sobre RDS en AWS (en este último caso, pgvector vive en el mismo PostgreSQL). Los prompts viven como plantillas StringTemplate en `src/main/resources/prompts`, desacoplados del código Java, y la misma lógica de negocio se reutiliza para dos mundos: tool calling interno de Spring AI (`@Tool`) y exposición externa vía MCP (`@McpTool`, `@McpResource`, `@McpPrompt`).

## Cómo funcionan los agentes

**1. Cadena de citas (Chain + Human-in-the-Loop)** — `AppointmentChainServiceImpl`
Pipeline secuencial de 3 pasos con plantillas dedicadas (`appointment-extraction.st`, `appointment-confirmation.st`): extrae especialidad y fecha del texto libre (`AppointmentRequest`), busca huecos disponibles (`AppointmentService.findAvailableAppointments`) y selecciona el mejor turno (`AppointmentSelection`). No escribe todavía en BD: guarda la selección por usuario en `PendingBookingStore` (un `ConcurrentHashMap`). Al confirmar, revalida disponibilidad contra la BD para evitar el doble booking y persiste; también puede cancelarse el turno pendiente.

**2. Router por tipo de consulta** — `RoutingWorkFlowServiceImpl`
`AnalysisService.classifyQuery` clasifica la intención (`QueryClassification`: `SYMPTOM_REPORT`, `GENERAL_QUESTION`, `EMERGENCY`, `PRESCRIPTION_REQUEST`, `OFF_TOPIC`) y un `switch` expression delega en el sub-agente correspondiente, cada uno con su plantilla (`routing-symptom.st`, `routing-general.st`, `routing-emergency.st`, `routing-prescription.st`) y su system prompt. Todas las rutas de LLM pasan por `MedicalAuditAdvisor` y mantienen memoria por `userId`; la ruta `OFF_TOPIC` responde texto fijo sin llamar al modelo (ahorro de tokens y coste).

**3. Orchestrator-Workers** — `OrchestratorServiceImpl` + `ClinicalWorker`, `HistoryWorker`, `BibliographyWorker`
Fan-out a tres especialistas: razonamiento clínico (LLM + `worker-clinical.st`), historial del paciente (consulta a BD, sin LLM) y bibliografía médica (RAG con búsqueda vectorial propia + `worker-bibliography.st`). Fan-in: una síntesis final (`orchestrator-synthesis.st`) integra los tres informes en una única respuesta. La variante `orchestrateWithProgress` emite eventos de progreso por SSE y aísla el trabajo bloqueante en `Schedulers.boundedElastic()` para no bloquear el hilo del servidor.

**4. Open Agent (agente autónomo)** — `OpenAgentServiceImpl`
Entrega cuatro tools al LLM (`PatientInfoTool`, `AppointmentSearchTool`, `DoctorInfoTool`, `DrugInfoTool`) y le deja decidir cuáles invocar, en qué orden y con qué parámetros. Inyecta la fecha actual en el prompt (`open-agent.st`) para evitar alucinaciones con fechas relativas y pasa `toolContext` con `userId` y `role=PATIENT`; las tools leen la identidad de ahí, nunca del texto del usuario (mitigación de prompt injection).

## Flujo RAG

**1. Ingesta** — al arrancar (`@PostConstruct`), `DocumentIngestionServiceImpl` (perfil distinto de `aws`) o `DocumentIngestionServiceAwsImpl` (perfil `aws`) leen los 3 PDFs de `classpath:docs` con `TikaDocumentReader` y los trocean con `TokenTextSplitter` (chunks de ~800 tokens, máximo 1000 chunks). La ingesta es idempotente: hace una búsqueda previa en el vector store y omite si ya hay documentos. También existe ingesta en caliente vía `POST /api/v1/documents/ingest`.

**2. Embeddings y almacenamiento** — los chunks se convierten en vectores con el `EmbeddingModel` del proveedor configurado y se almacenan en ChromaDB (`ChromaVectorStore`, colecciones separadas `medassistant_google` y `medassistant_ollama` porque los espacios vectoriales de cada proveedor son incompatibles) o en pgvector (`PgVectorStore`, 3072 dimensiones, tabla `medassistant_google`) en AWS.

**3. Recuperación** — `QuestionAnswerAdvisor` (topK=3, umbral de similitud 0.7) inyecta contexto clínico de forma transparente en cada chat (`AssistantConfig` / `AssistantConfigAws`). Además, `BibliographyWorker` realiza su propia recuperación durante la orquestación (`searchDocuments`: topK=3, umbral 0.3) con cláusula fail-fast si no hay documentos relevantes.

## Estructura del proyecto

```
src/main/java/com/example/medassistant
├── MedassistantApplication.java   # arranque de Spring Boot
├── agent/                         # patrones de agentes (chain, routing, open agent)
│   └── orchestrator/              # orquestador y workers (Orchestrator-Workers)
├── config/                        # seguridad, ChatClients, memoria, auditoría, seeding
├── controller/                    # API REST (/api/v1/**)
├── dto/                           # records de entrada/salida
│   ├── agent/                     # DTOs de la cadena de citas
│   └── analysis/                  # DTOs del análisis estructurado
├── exception/                     # manejo global de errores
├── mcp/                           # servidor MCP: resources y prompts expuestos
├── model/                         # entidades JPA (Patient, Doctor, Appointment)
├── repository/                    # repositorios Spring Data JPA
├── service/                       # lógica de negocio y servicios de IA
├── tools/                         # herramientas de tool calling (@Tool / @McpTool)
└── util/                          # utilidades (extracción de roles del JWT)
```

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

## Endpoints

El cuerpo de los POST es `{ "prompt": "...", "model": "gemini" | "ollama" }` (`ChatRequest`); el `userId` se extrae siempre del token, nunca del body. Reglas de seguridad según `SecurityConfig`: `/api/v1/**` requiere JWT válido con rol `PATIENT` o `ADMIN`; `/mcp/**` es público.

| Método | Ruta | Descripción | Rol / Autenticación |
|---|---|---|---|
| POST | `/api/v1/chat` | Chat síncrono con memoria. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/chat/stream` | Chat en streaming SSE. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/chat/explain` | Explica una condición médica con lenguaje accesible. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/chat/symptoms` | Orienta sobre síntomas descritos. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/chat/diagnose` | Razonamiento clínico con chain-of-thought. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/chat/consult` | Consulta médica completa. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/analysis/condition` | Resumen estructurado de una condición (`ConditionSummary`). | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/analysis/conditions` | Lista de condiciones relacionadas con una consulta. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/analysis/symptoms` | Análisis estructurado de síntomas (`SymptomAnalysis`). | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/analysis/classify` | Clasifica la consulta por tipo (`QueryClassification`). | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/documents/ingest` | Sube un PDF (multipart) e ingesta sus chunks en el vector store. | Restringido a `ADMIN` vía `@PreAuthorize("hasRole('ADMIN')")` — TODO: pendiente de verificar en ejecución |
| POST | `/api/v1/agent/chain/appointment` | Reserva de turno paso a paso (extracción → búsqueda → selección). | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/agent/chain/appointment/confirm` | Confirma el turno pendiente (Human-in-the-Loop, revalida disponibilidad). | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/agent/chain/appointment/cancel` | Descarta el turno pendiente. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/agent/routing` | Router Workflow: clasifica y delega al sub-agente adecuado. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/agent/orchestrator` | Orchestrator-Workers: análisis clínico + historial + bibliografía + síntesis. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/agent/orchestrator/progress` | Igual que el anterior con progreso en vivo por SSE. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/agent/open` | Agente autónomo con tool calling libre. | JWT · `PATIENT` o `ADMIN` |
| POST | `/api/v1/mcp/demo` | Demo de conversación usando las tools de servidores MCP externos (solo perfil distinto de `aws`). | JWT · `PATIENT` o `ADMIN` |
| — | `/mcp/**` | Servidor MCP (tools, resources y prompts expuestos a clientes externos). | Público (`permitAll`) |

## Ejemplos de uso con curl

```bash
export TOKEN="<JWT emitido por Keycloak>"   # nunca un token real en el repositorio

# 1) Chat síncrono con memoria
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "¿Qué cuidados debo seguir si tengo hipertensión?", "model": "gemini"}'

# 2) Orchestrator con progreso en vivo (SSE; -N desactiva el buffering)
curl -N -X POST http://localhost:8080/api/v1/agent/orchestrator/progress \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Dolor torácico opresivo y fatiga al esfuerzo", "model": "gemini"}'
```

## Decisiones técnicas

- **Perfiles como estrategia de proveedor**: la interfaz `ClientResolver` abstrae la selección de modelo y vector store; `ClientResolverDefault` (gemini/ollama) y `ClientResolverAws` (solo gemini) permiten la misma lógica de negocio en local y en AWS cambiando solo el perfil activo.
- **Identidad extraída del JWT, nunca del body**: los controladores reciben el `userId` con `@AuthenticationPrincipal`; `SecurityUtils` extrae el rol de negocio. Las tools reciben `userId` y `role` vía `ToolContext` inyectado por el backend, lo que evita que un prompt malicioso suplante a otro paciente o escale privilegios.
- **Doble barrera en acciones sensibles**: `bookAppointment` indica en su descripción que es solo para admin y además lo valida por código contra el rol del `ToolContext`.
- **MCP `STATELESS`**: el servidor MCP (`medassistant-mcp`, v1.0.0) no mantiene sesión entre peticiones, coherente con un transporte WebMVC y una API sin estado.
- **Conversión de roles Keycloak → Spring**: `realm_access.roles` se traduce a `ROLE_<NOMBRE>` con pattern matching defensivo para evitar `ClassCastException` al deserializar el JSON de roles.
- **Un `ChatClient` nuevo por petición**: los agentes usan `ChatClient.create(...)` para no compartir estado entre usuarios; los beans de `AssistantConfig` solo definen valores por defecto (system prompt, tools, advisors).
- **Colecciones vectoriales separadas por proveedor de embeddings**: Gemini y Ollama generan espacios vectoriales incompatibles entre sí; mezclarlos en una misma colección rompería las búsquedas por similitud.
- **Prompts externalizados** en plantillas StringTemplate (`src/main/resources/prompts`), editables sin recompilar.
- **Memoria acotada**: ventana de 9 mensajes sobre repositorio JDBC, para limitar tokens y coste manteniendo contexto.
- **SSE para latencia percibida**: chat y orquestación emiten tokens/eventos en tiempo real, con el trabajo bloqueante aislado en `Schedulers.boundedElastic()`.
- **Fail-fast en la selección de modelo**: un valor no soportado lanza `IllegalArgumentException` de inmediato; las consultas off-topic se responden sin llamar al LLM.
- **Contenedor de producción**: Dockerfile multi-stage (build con Maven/JDK 21, runtime solo JRE), usuario no-root y `-XX:MaxRAMPercentage=75.0` para instancias ajustadas de RAM.

## Despliegue en AWS

Verificable en el repositorio:

- **Dockerfile multi-stage**: compilación con `maven:3.9-eclipse-temurin-21` (cacheo de dependencias con `dependency:go-offline`) y runtime ligero `eclipse-temurin:21-jre-jammy`, ejecutando como usuario no-root (`spring`), con TZ `Europe/Madrid`, `SPRING_PROFILES_ACTIVE=aws` por defecto, puerto 8080 y `-XX:MaxRAMPercentage=75.0` (pensado para instancias EC2 ajustadas de RAM).
- **Perfil `aws`**: RDS PostgreSQL parametrizado por variables `RDS_*`; el vector store pasa de ChromaDB a **pgvector** (`PgVectorStore`, 3072 dimensiones, tabla `medassistant_google`); `schema-aws.sql` crea la extensión `vector`; el cliente MCP stdio queda deshabilitado.
- **`docker-compose.aws-test.yml`**: permite probar el perfil `aws` en local levantando PostgreSQL con pgvector (puerto 5434) en lugar de RDS.

TODO (no verificable solo con el código de este repositorio): topología exacta de despliegue (EC2/ECS, balanceador, grupos de seguridad), aprovisionamiento del RDS, pipeline de CI/CD y publicación de imágenes.

## Qué aprendí

> TODO (autor): completar con 3–5 aprendizajes reales de este proyecto.

## Próximos pasos

> TODO (autor): completar con los próximos pasos previstos.

## Autor

**Enger Durán**
