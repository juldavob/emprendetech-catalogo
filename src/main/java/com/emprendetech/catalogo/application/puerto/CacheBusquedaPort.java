package com.emprendetech.catalogo.application.puerto;

import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.Pagina;
import com.emprendetech.catalogo.domain.Producto;
import java.util.Optional;

/** Puerto de salida: caché de consultas frecuentes del catálogo (RNF2 y RNF3). */
public interface CacheBusquedaPort {

    Optional<Pagina<Producto>> obtener(CriteriosBusqueda criterios);

    void guardar(CriteriosBusqueda criterios, Pagina<Producto> resultado);

    void invalidarTodo();
}
