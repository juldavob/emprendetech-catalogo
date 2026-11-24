package com.emprendetech.catalogo.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogoConfig {

    /** Reloj inyectable: los casos de uso no llaman a Instant.now(), lo que facilita las pruebas. */
    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
