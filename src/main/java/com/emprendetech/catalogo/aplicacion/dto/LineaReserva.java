package com.emprendetech.catalogo.aplicacion.dto;

import java.util.UUID;

/** Una línea de reserva de stock: producto y cantidad. */
public record LineaReserva(UUID productoId, int cantidad) {
}
