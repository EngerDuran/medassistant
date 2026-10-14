package com.example.medassistant.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;


/**
 * Utilidad para extraer información limpia de la sesión de Spring Security.
 * Evita repetir la lógica de transformación de roles en los controladores o servicios.
 */
@Component
public class SecurityUtils {

    /**
     * Obtiene el rol principal del usuario autenticado sin el prefijo 'ROLE_'.
     * Se usa para pasarlo al contexto de la IA (ToolContext) y validar permisos.
     */
    public String extractRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                // 1. Convertimos cada objeto de permiso a su texto (ej. "ROLE_PATIENT")
                .map(GrantedAuthority::getAuthority)
                // 2. Ignoramos permisos internos de Keycloak y filtramos solo los de negocio
                .filter(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_PATIENT"))
                .findFirst()
                // 3. Limpiamos el prefijo de Spring para dejar solo "ADMIN" o "PATIENT"
                .map(a -> a.replace("ROLE_", ""))
                .orElse("UNKNOWN");
    }
}
