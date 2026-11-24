package com.emprendetech.catalogo.application.modelo;

import com.emprendetech.catalogo.domain.Categoria;
import java.math.BigDecimal;

/** Datos de entrada para crear o actualizar un producto. */
public record DatosProducto(String nombre, String descripcion, Categoria categoria,
                            BigDecimal precio, int stock) {
}
