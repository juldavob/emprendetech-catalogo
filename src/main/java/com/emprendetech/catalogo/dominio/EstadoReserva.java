package com.emprendetech.catalogo.dominio;

public enum EstadoReserva {
    /** Stock apartado al crear el pedido (aún sin pago). */
    RESERVADA,
    /** Pago aprobado: el descuento de stock queda firme. */
    CONFIRMADA,
    /** Pago rechazado o pedido cancelado: el stock se devolvió. */
    LIBERADA
}
