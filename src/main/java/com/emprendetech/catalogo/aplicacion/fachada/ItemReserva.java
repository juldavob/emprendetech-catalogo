package com.emprendetech.catalogo.aplicacion.fachada;

import java.util.UUID;

/** Ítem de un pedido cuyo stock se desea reservar. */
public record ItemReserva(UUID productoId, int cantidad) {
}
