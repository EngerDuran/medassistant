package com.example.medassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Configuración de seguridad para Resource Server con OAuth2 y JWT.
 * Adapta el formato de roles emitido por Keycloak a Spring Security.
 */
@Configuration
public class securityConfig {

    /**
     * Extrae los roles anidados en Keycloak ('realm_access' -> 'roles') y los mapea
     * al formato estándar de Spring Security ('ROLE_<NOMBRE>').
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter(){
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Keycloak agrupa los roles del realm dentro del claim anidado "realm_access"
            var realmAccess = (Map<String, Object>) jwt.getClaim("realm_access");
            if(realmAccess==null) return List.of();

            // Extrae la lista de nombres de roles asignados al usuario
            var roles = (List<String>) realmAccess.get("roles");
            if(roles==null) return List.of();

            // Spring Security exige el prefijo "ROLE_" para verificar autoridades (ej. hasRole("USER"))
            return  roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        return  converter;
    }
}