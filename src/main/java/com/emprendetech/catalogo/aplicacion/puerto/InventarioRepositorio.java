package com.emprendetech.catalogo.aplicacion.puerto;

import java.util.UUID;

/**
 * Puerto de salida: operaciones ATÓMICAS de inventario (una sola sentencia, sin leer-modificar-escribir),
 * para evitar sobreventa bajo alta concurrencia (RNF2).
 */
public interface InventarioRepositorio {

    /** Descuenta stock solo si el producto está activo y hay existencias. @return true si descontó. */
    boolean descontar(UUID productoId, int cantidad);

    /** Devuelve stock al producto. */
    void incrementar(UUID productoId, int cantidad);
}
