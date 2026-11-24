package com.emprendetech.api;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Capa de API REST transversal: aplicación sin estado (RNF2), control de acceso por rol (RNF6).
 * Lectura del catálogo pública; escritura solo EMPRENDEDOR; moderación solo ADMIN.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private static final String CATALOGO = "/api/v1/catalogo/**";

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http,
            @Value("${emprendetech.seguridad.jwt-secret:}") String secreto) throws Exception {
        byte[] llave = secreto.getBytes(StandardCharsets.UTF_8);
        if (llave.length < 32) {
            throw new IllegalStateException(
                    "emprendetech.seguridad.jwt-secret debe tener al menos 32 caracteres (variable JWT_SECRET)");
        }
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/dev/tokens").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/tiendas/mia").hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.GET, CATALOGO).permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/catalogo/productos/*/moderacion").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, CATALOGO).hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.PUT, CATALOGO).hasRole("EMPRENDEDOR")
                        .requestMatchers(HttpMethod.DELETE, CATALOGO).hasRole("EMPRENDEDOR")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtFiltro(llave), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
