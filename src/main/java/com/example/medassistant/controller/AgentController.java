package com.example.medassistant.controller;

import com.example.medassistant.agent.AppointmentChainService;
import com.example.medassistant.agent.OpenAgentService;
import com.example.medassistant.agent.RoutingWorkFlowService;
import com.example.medassistant.agent.orchestrator.OrchestratorService;
import com.example.medassistant.dto.ChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AppointmentChainService appointmentChainService;
    private final RoutingWorkFlowService routingWorkFlowService;
    private final OrchestratorService orchestratorService;
    private final OpenAgentService openAgentService;


    /**
     * Seguridad por diseño: El userId se extrae criptográficamente del token JWT
     * (@AuthenticationPrincipal), ignorando cualquier ID que pudiera mandar el frontend
     * o la IA. Esto previene vulnerabilidades.
     */
    @PostMapping("/chain/appointment")
    public ResponseEntity<String> bookAppointmentChain(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {

        Long userId = jwt.getClaim("userId");

        return ResponseEntity.ok(appointmentChainService
                .bookAppointmentChain(request.prompt(), request.model(), userId));
    }

    @PostMapping("/routing")
    public  ResponseEntity<String> routeQuery(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(
                routingWorkFlowService.routeQuery(request.prompt(), request.model(), userId));
    }

    @PostMapping("/orchestrator")
    public  ResponseEntity<String> orchestrate(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(
                orchestratorService.orchestrate(request.prompt(), request.model(), userId));
    }

    @PostMapping("/chain/appointment/confirm")
    public ResponseEntity<String> confirmBooking(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(appointmentChainService.confirmBooking(userId));
    }

    @PostMapping("/chain/appointment/cancel")
    public ResponseEntity<String> cancelBooking(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(appointmentChainService.cancelBooking(userId));
    }

    @PostMapping(value = "/orchestrator/progress", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> orchestrateWithProgress(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return orchestratorService.orchestrateWithProgress(request.prompt(), request.model(), userId);
    }

    /**
     * Endpoint del Agente Autónomo.
     * La identidad del usuario se garantiza criptográficamente desencriptando
     * el token JWT (AuthenticationPrincipal), protegiendo la privacidad médica.
     */
    @PostMapping("/open")
    public  ResponseEntity<String> openAgent(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(
                openAgentService.execute(request.prompt(), request.model(), userId));
    }

}
