package com.emprendetech.catalogo.dominio;

import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import java.time.Instant;
import java.util.UUID;

/**
 * Reserva de stock de un producto para un pedido (SAD §4.1 y §6): se crea al hacer checkout,
 * se confirma cuando Pagos aprueba el pago y se libera si el pago es rechazado o el pedido se cancela.
 */
public final class ReservaStock {

    private final UUID id;
    private final UUID pedidoId;
    private final UUID productoId;
    private final int cantidad;
    private final Instant creadaEn;

    private EstadoReserva estado;
    private Instant actualizadaEn;

    private ReservaStock(UUID id, UUID pedidoId, UUID productoId, int cantidad,
                         EstadoReserva estado, Instant creadaEn, Instant actualizadaEn) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.estado = estado;
        this.creadaEn = creadaEn;
        this.actualizadaEn = actualizadaEn;
    }

    public static ReservaStock crear(UUID pedidoId, UUID productoId, int cantidad, Instant ahora) {
        if (cantidad <= 0) {
            throw new DatoInvalidoException("La cantidad a reservar debe ser mayor que cero");
        }
        return new ReservaStock(UUID.randomUUID(), Reglas.requerido(pedidoId, "pedido"),
                Reglas.requerido(productoId, "producto"), cantidad, EstadoReserva.RESERVADA,
                Reglas.requerido(ahora, "creadaEn"), ahora);
    }

    public static ReservaStock reconstruir(UUID id, UUID pedidoId, UUID productoId, int cantidad,
                                           EstadoReserva estado, Instant creadaEn, Instant actualizadaEn) {
        return new ReservaStock(id, pedidoId, productoId, cantidad, estado, creadaEn, actualizadaEn);
    }

    /** Idempotente: confirmar una reserva ya confirmada no hace nada (el webhook puede reintentarse). */
    public void confirmar(Instant ahora) {
        if (estado == EstadoReserva.LIBERADA) {
            throw new ConflictoException("La reserva del pedido " + pedidoId + " ya fue liberada");
        }
        if (estado == EstadoReserva.RESERVADA) {
            estado = EstadoReserva.CONFIRMADA;
            actualizadaEn = ahora;
        }
    }

    /**
     * Idempotente: liberar una reserva ya liberada no hace nada.
     * @return true si esta llamada cambió el estado (y por tanto hay que devolver el stock).
     */
    public boolean liberar(Instant ahora) {
        if (estado == EstadoReserva.CONFIRMADA) {
            throw new ConflictoException("La reserva del pedido " + pedidoId + " ya fue confirmada");
        }
        if (estado == EstadoReserva.RESERVADA) {
            estado = EstadoReserva.LIBERADA;
            actualizadaEn = ahora;
            return true;
        }
        return false;
    }

    public UUID id() { return id; }
    public UUID pedidoId() { return pedidoId; }
    public UUID productoId() { return productoId; }
    public int cantidad() { return cantidad; }
    public EstadoReserva estado() { return estado; }
    public Instant creadaEn() { return creadaEn; }
    public Instant actualizadaEn() { return actualizadaEn; }
}
