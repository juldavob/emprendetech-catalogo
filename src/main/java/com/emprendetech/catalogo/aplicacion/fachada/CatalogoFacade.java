package com.emprendetech.catalogo.aplicacion.fachada;

import com.emprendetech.catalogo.aplicacion.dto.LineaReserva;
import com.emprendetech.catalogo.aplicacion.servicio.GestionInventario;
import com.emprendetech.catalogo.aplicacion.servicio.GestionProductos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FACHADA PÚBLICA del módulo Catálogo (SAD §5.1 y Tabla 4): la única puerta por la que Pedidos
 * consulta productos y reserva, confirma o libera stock. Ningún otro módulo toca el dominio ni la
 * infraestructura del Catálogo. Todas las operaciones son idempotentes o fallan sin efectos parciales.
 */
public class CatalogoFacade {

    private final GestionProductos productos;
    private final GestionInventario inventario;

    public CatalogoFacade(GestionProductos productos, GestionInventario inventario) {
        this.productos = productos;
        this.inventario = inventario;
    }

    /** Verifica disponibilidad: devuelve el producto ACTIVO o vacío si no existe o está oculto. */
    public Optional<ProductoVista> consultarProducto(UUID productoId) {
        return productos.buscarActivo(productoId)
                .map(p -> new ProductoVista(p.id(), p.tiendaId(), p.nombre(), p.precio(), p.stock()));
    }

    /**
     * Reserva (descuenta) el stock de todos los ítems del pedido, o de ninguno. Lanza ConflictoException si
     * algún producto no tiene existencias suficientes. El pedidoId permite confirmar o liberar después.
     */
    public void reservarStock(UUID pedidoId, List<ItemReserva> items) {
        List<LineaReserva> lineas = items == null ? null
                : items.stream().map(i -> new LineaReserva(i.productoId(), i.cantidad())).toList();
        inventario.reservar(pedidoId, lineas);
    }

    /** Evento PagoAprobado: confirma la reserva del pedido (el descuento queda firme). */
    public void confirmarStock(UUID pedidoId) {
        inventario.confirmar(pedidoId);
    }

    /** Pago rechazado o pedido cancelado: devuelve el stock reservado. */
    public void liberarStock(UUID pedidoId) {
        inventario.liberar(pedidoId);
    }
}
