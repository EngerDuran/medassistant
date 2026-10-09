package com.example.medassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Desactivamos CSRF al ser una API stateless basada en tokens (sin cookies)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/**").hasRole("PATIENT")
                        .anyRequest().authenticated()
                )
                // Delegamos la validación a Keycloak y adaptamos los roles al formato de Spring
                .oauth2ResourceServer(auth2 ->
                        auth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    /**
     * Actúa como un traductor entre Keycloak y Spring Security.
     * Keycloak envía los roles dentro de un objeto JSON anidado ("realm_access" -> "roles"),
     * pero Spring Security requiere que estén en una lista plana y con el prefijo "ROLE_".
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter(){
        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt ->{
            // Extraemos el mapa principal de forma segura
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) return List.of();

            // Usamos pattern matching (instanceof) para prevenir ClassCastException
            // al deserializar el JSON de los roles
            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof List<?> roles)) return List.of();

            // Filtramos asegurando que cada rol sea un String antes de mapearlo
            // al formato que exige Spring Security (ROLE_<NOMBRE>)
            return roles.stream()
                    .filter(role -> role instanceof String)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        return converter;
    }
}