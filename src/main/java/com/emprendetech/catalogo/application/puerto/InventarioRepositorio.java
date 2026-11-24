package com.emprendetech.catalogo.application.puerto;

import java.util.UUID;

/**
 * Puerto de salida: operaciones ATÓMICAS de inventario. Se separan de {@link ProductoRepositorio}
 * para evitar sobreventa bajo alta concurrencia (RNF2): la verificación y el descuento ocurren
 * en una sola sentencia, sin "leer-modificar-escribir".
 */
public interface InventarioRepositorio {

    /** Descuenta stock solo si el producto está activo y hay existencias. @return true si descontó. */
    boolean reservar(UUID productoId, int cantidad);

    /** Devuelve stock. @return false si el producto no existe. */
    boolean liberar(UUID productoId, int cantidad);
}
