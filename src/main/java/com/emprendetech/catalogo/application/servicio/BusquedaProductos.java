package com.emprendetech.catalogo.application.servicio;

import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.Pagina;
import com.emprendetech.catalogo.application.puerto.BusquedaProductosPort;
import com.emprendetech.catalogo.application.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.domain.Producto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso RF3: búsqueda y filtro de productos, con caché de consultas frecuentes (RNF3). */
@Service
@Transactional(readOnly = true)
public class BusquedaProductos {

    private final BusquedaProductosPort busqueda;
    private final CacheBusquedaPort cache;

    public BusquedaProductos(BusquedaProductosPort busqueda, CacheBusquedaPort cache) {
        this.busqueda = busqueda;
        this.cache = cache;
    }

    public Pagina<Producto> buscar(CriteriosBusqueda criterios) {
        return cache.obtener(criterios).orElseGet(() -> {
            Pagina<Producto> resultado = busqueda.buscar(criterios);
            cache.guardar(criterios, resultado);
            return resultado;
        });
    }
}
