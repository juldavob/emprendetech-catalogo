package com.emprendetech.catalogo.aplicacion.puerto;

import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;

/** Puerto de salida: consulta de productos ACTIVOS con filtros (RF3). */
public interface BusquedaProductosPort {

    ResultadoBusqueda buscar(CriteriosBusqueda criterios);
}
