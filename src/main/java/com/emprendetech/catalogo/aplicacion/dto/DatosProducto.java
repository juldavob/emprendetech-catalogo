package com.emprendetech.catalogo.aplicacion.dto;

import java.math.BigDecimal;

/** Datos de entrada para crear o actualizar un producto (la categoría llega como texto y se valida en el caso de uso). */
public record DatosProducto(String nombre, String descripcion, String categoria, BigDecimal precio, int stock) {
}
