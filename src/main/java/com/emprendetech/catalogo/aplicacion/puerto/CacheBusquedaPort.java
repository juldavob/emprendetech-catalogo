package com.emprendetech.catalogo.aplicacion.puerto;

import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;
import com.emprendetech.catalogo.aplicacion.dto.PaginaDto;
import com.emprendetech.catalogo.aplicacion.dto.ProductoDto;
import java.util.Optional;

/** Puerto de salida: caché de consultas del catálogo (RNF2, RNF3). Un fallo de la caché nunca debe romper la búsqueda. */
public interface CacheBusquedaPort {

    Optional<PaginaDto<ProductoDto>> obtener(CriteriosBusqueda criterios);

    void guardar(CriteriosBusqueda criterios, PaginaDto<ProductoDto> resultado);

    void invalidarTodo();
}
