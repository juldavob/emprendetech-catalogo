package com.emprendetech.catalogo.aplicacion.servicio;

import com.emprendetech.catalogo.aplicacion.dto.LineaReserva;
import com.emprendetech.catalogo.aplicacion.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.aplicacion.puerto.InventarioRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.ReservaRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.Transaccion;
import com.emprendetech.catalogo.dominio.Producto;
import com.emprendetech.catalogo.dominio.ReservaStock;
import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import com.emprendetech.shared_kernel.excepcion.NoEncontradoException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reserva de inventario en dos fases (SAD §4.1, §6 y Tabla 3): Pedidos reserva el stock al hacer
 * checkout; si el pago se aprueba se CONFIRMA, y si se rechaza o el pedido se cancela se LIBERA.
 * La reserva de todas las líneas es atómica: si una falla, ninguna queda apartada.
 */
public class GestionInventario {

    private final InventarioRepositorio inventario;
    private final ReservaRepositorio reservas;
    private final ProductoRepositorio productos;
    private final CacheBusquedaPort cache;
    private final Transaccion tx;
    private final Clock reloj;

    public GestionInventario(InventarioRepositorio inventario, ReservaRepositorio reservas,
                             ProductoRepositorio productos, CacheBusquedaPort cache,
                             Transaccion tx, Clock reloj) {
        this.inventario = inventario;
        this.reservas = reservas;
        this.productos = productos;
        this.cache = cache;
        this.tx = tx;
        this.reloj = reloj;
    }

    public void reservar(UUID pedidoId, List<LineaReserva> lineas) {
        validar(pedidoId, lineas);
        tx.ejecutar(() -> {
            if (reservas.existePorPedido(pedidoId)) {
                throw new ConflictoException("Ya existe una reserva para el pedido " + pedidoId);
            }
            Instant ahora = reloj.instant();
            List<ReservaStock> nuevas = new ArrayList<>();
            for (LineaReserva linea : lineas) {
                if (!inventario.descontar(linea.productoId(), linea.cantidad())) {
                    throw motivoDelFallo(linea.productoId()); // la excepción revierte toda la transacción
                }
                nuevas.add(ReservaStock.crear(pedidoId, linea.productoId(), linea.cantidad(), ahora));
            }
            reservas.guardarTodas(nuevas);
        });
        cache.invalidarTodo();
    }

    /** Pago aprobado: el descuento queda firme. Idempotente (la pasarela puede reintentar el webhook). */
    public void confirmar(UUID pedidoId) {
        tx.ejecutar(() -> {
            List<ReservaStock> delPedido = reservasDe(pedidoId);
            Instant ahora = reloj.instant();
            delPedido.forEach(r -> r.confirmar(ahora));
            reservas.guardarTodas(delPedido);
        });
    }

    /** Pago rechazado o pedido cancelado: devuelve el stock apartado. Idempotente. */
    public void liberar(UUID pedidoId) {
        tx.ejecutar(() -> {
            List<ReservaStock> delPedido = reservasDe(pedidoId);
            Instant ahora = reloj.instant();
            for (ReservaStock reserva : delPedido) {
                if (reserva.liberar(ahora)) {
                    inventario.incrementar(reserva.productoId(), reserva.cantidad());
                }
            }
            reservas.guardarTodas(delPedido);
        });
        cache.invalidarTodo();
    }

    private List<ReservaStock> reservasDe(UUID pedidoId) {
        if (pedidoId == null) {
            throw new DatoInvalidoException("El pedido es obligatorio");
        }
        List<ReservaStock> delPedido = reservas.buscarPorPedido(pedidoId);
        if (delPedido.isEmpty()) {
            throw new NoEncontradoException("No hay reserva de stock para el pedido " + pedidoId);
        }
        return delPedido;
    }

    private RuntimeException motivoDelFallo(UUID productoId) {
        Producto producto = productos.buscarPorId(productoId).orElse(null);
        if (producto == null) {
            return new NoEncontradoException("El producto " + productoId + " no existe");
        }
        return new ConflictoException(producto.estaActivo()
                ? "Stock insuficiente para el producto " + productoId
                : "El producto " + productoId + " no está disponible");
    }

    private static void validar(UUID pedidoId, List<LineaReserva> lineas) {
        if (pedidoId == null) {
            throw new DatoInvalidoException("El pedido es obligatorio");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new DatoInvalidoException("Debe indicar al menos un producto a reservar");
        }
        Set<UUID> vistos = new HashSet<>();
        for (LineaReserva linea : lineas) {
            if (linea == null || linea.productoId() == null) {
                throw new DatoInvalidoException("Cada línea debe indicar un producto");
            }
            if (linea.cantidad() <= 0) {
                throw new DatoInvalidoException("La cantidad debe ser mayor que cero");
            }
            if (!vistos.add(linea.productoId())) {
                throw new DatoInvalidoException("El producto " + linea.productoId() + " está repetido en el pedido");
            }
        }
    }
}
