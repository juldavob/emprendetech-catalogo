package com.emprendetech.catalogo.aplicacion.puerto;

import com.emprendetech.catalogo.dominio.Producto;
import java.util.List;

/** Resultado crudo de una consulta paginada: la página de productos y el total que cumple los filtros. */
public record ResultadoBusqueda(List<Producto> productos, long total) {
}
