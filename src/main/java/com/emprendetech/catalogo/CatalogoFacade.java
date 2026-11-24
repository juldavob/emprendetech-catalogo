package com.emprendetech.catalogo;

import com.emprendetech.catalogo.application.servicio.GestionInventario;
import com.emprendetech.catalogo.application.servicio.GestionProductos;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * FACHADA PÚBLICA del módulo Catálogo (SAD §2.1 y §2.3, regla 1). Es la única puerta de entrada
 * para otros módulos: ninguno importa el dominio ni la infraestructura del Catálogo.
 * Spring Modulith considera API pública solo los tipos del paquete raíz del módulo.
 */
@Service
public class CatalogoFacade {

    private final GestionProductos productos;
    private final GestionInventario inventario;

    public CatalogoFacade(GestionProductos productos, GestionInventario inventario) {
        this.productos = productos;
        this.inventario = inventario;
    }

    /** Consulta un producto ACTIVO; vacío si no existe o está oculto por moderación. */
    public Optional<ProductoVista> consultarProducto(UUID productoId) {
        return productos.buscarActivo(productoId)
                .map(p -> new ProductoVista(p.id(), p.tiendaId(), p.nombre(), p.precio().monto(), p.stock()));
    }

    /** Descuenta stock de forma atómica. Falla si no hay existencias suficientes. */
    public void reservarStock(UUID productoId, int cantidad) {
        inventario.reservar(productoId, cantidad);
    }

    /** Devuelve stock (p. ej., cuando un pago es rechazado). */
    public void liberarStock(UUID productoId, int cantidad) {
        inventario.liberar(productoId, cantidad);
    }
}
