package com.emprendetech.catalogo.application.puerto;

import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.Pagina;
import com.emprendetech.catalogo.domain.Producto;

/** Puerto de salida: consulta de productos ACTIVOS con filtros (RF3). */
public interface BusquedaProductosPort {

    Pagina<Producto> buscar(CriteriosBusqueda criterios);
}
