package com.emprendetech.catalogo.aplicacion.fachada;

import java.math.BigDecimal;
import java.util.UUID;

/** Vista inmutable de un producto para otros módulos (p. ej., Pedidos). No expone el dominio. */
public record ProductoVista(UUID id, UUID tiendaId, String nombre, BigDecimal precio, int stock) {
}
