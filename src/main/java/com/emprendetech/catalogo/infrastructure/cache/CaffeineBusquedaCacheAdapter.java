package com.emprendetech.catalogo.infrastructure.cache;

import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.Pagina;
import com.emprendetech.catalogo.application.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.domain.Producto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ADAPTER del puerto {@link CacheBusquedaPort} con Caffeine (en memoria, por instancia).
 * En producción (varias instancias detrás del balanceador) se reemplaza por un adaptador Redis
 * sin modificar el dominio ni los casos de uso.
 */
@Component
public class CaffeineBusquedaCacheAdapter implements CacheBusquedaPort {

    private final Cache<CriteriosBusqueda, Pagina<Producto>> cache;

    public CaffeineBusquedaCacheAdapter(
            @Value("${emprendetech.catalogo.cache.ttl-segundos:60}") long ttlSegundos,
            @Value("${emprendetech.catalogo.cache.max-entradas:1000}") long maxEntradas) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSegundos))
                .maximumSize(maxEntradas)
                .build();
    }

    @Override
    public Optional<Pagina<Producto>> obtener(CriteriosBusqueda criterios) {
        return Optional.ofNullable(cache.getIfPresent(criterios));
    }

    @Override
    public void guardar(CriteriosBusqueda criterios, Pagina<Producto> resultado) {
        cache.put(criterios, resultado);
    }

    @Override
    public void invalidarTodo() {
        cache.invalidateAll();
    }
}
