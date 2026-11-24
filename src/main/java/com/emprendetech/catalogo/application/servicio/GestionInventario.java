package com.emprendetech.catalogo.application.servicio;

import com.emprendetech.catalogo.application.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.application.puerto.InventarioRepositorio;
import com.emprendetech.catalogo.application.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.domain.Producto;
import com.emprendetech.catalogo.domain.excepcion.ConflictoException;
import com.emprendetech.catalogo.domain.excepcion.DatoInvalidoException;
import com.emprendetech.catalogo.domain.excepcion.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de inventario que consume el módulo Pedidos a través de la fachada
 * (SAD §2.5: "Pedidos consulta a Catálogo y reserva el stock", y "si el pago es rechazado,
 * Catálogo libera el stock").
 */
@Service
@Transactional
public class GestionInventario {

    private final InventarioRepositorio inventario;
    private final ProductoRepositorio productos;
    private final CacheBusquedaPort cache;

    public GestionInventario(InventarioRepositorio inventario, ProductoRepositorio productos,
                             CacheBusquedaPort cache) {
        this.inventario = inventario;
        this.productos = productos;
        this.cache = cache;
    }

    public void reservar(UUID productoId, int cantidad) {
        validarCantidad(cantidad);
        if (!inventario.reservar(productoId, cantidad)) {
            Producto producto = productos.buscarPorId(productoId)
                    .orElseThrow(() -> new NoEncontradoException("El producto no existe"));
            throw new ConflictoException(producto.estaActivo()
                    ? "Stock insuficiente para el producto " + productoId
                    : "El producto no está disponible");
        }
        cache.invalidarTodo();
    }

    public void liberar(UUID productoId, int cantidad) {
        validarCantidad(cantidad);
        if (!inventario.liberar(productoId, cantidad)) {
            throw new NoEncontradoException("El producto no existe");
        }
        cache.invalidarTodo();
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new DatoInvalidoException("La cantidad debe ser mayor que cero");
        }
    }
}
