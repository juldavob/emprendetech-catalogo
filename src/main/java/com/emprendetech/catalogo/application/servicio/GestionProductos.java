package com.emprendetech.catalogo.application.servicio;

import com.emprendetech.catalogo.application.modelo.AccionModeracion;
import com.emprendetech.catalogo.application.modelo.DatosProducto;
import com.emprendetech.catalogo.application.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.application.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.application.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.domain.Producto;
import com.emprendetech.catalogo.domain.Tienda;
import com.emprendetech.catalogo.domain.excepcion.DatoInvalidoException;
import com.emprendetech.catalogo.domain.excepcion.NoEncontradoException;
import com.emprendetech.catalogo.domain.excepcion.OperacionNoPermitidaException;
import com.emprendetech.shared.Dinero;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso: RF2 (CRUD de productos por emprendedor) y moderación del administrador. */
@Service
@Transactional
public class GestionProductos {

    private final ProductoRepositorio productos;
    private final TiendaRepositorio tiendas;
    private final CacheBusquedaPort cache;
    private final Clock reloj;

    public GestionProductos(ProductoRepositorio productos, TiendaRepositorio tiendas,
                            CacheBusquedaPort cache, Clock reloj) {
        this.productos = productos;
        this.tiendas = tiendas;
        this.cache = cache;
        this.reloj = reloj;
    }

    public Producto crear(UUID propietarioId, DatosProducto datos) {
        Tienda tienda = tiendas.buscarPorPropietario(propietarioId)
                .orElseThrow(() -> new NoEncontradoException("Debe crear su tienda antes de publicar productos"));
        Producto nuevo = Producto.crear(tienda.id(), datos.nombre(), datos.descripcion(), datos.categoria(),
                dinero(datos.precio()), datos.stock(), reloj.instant());
        Producto guardado = productos.guardar(nuevo);
        cache.invalidarTodo();
        return guardado;
    }

    public Producto actualizar(UUID usuarioId, UUID productoId, DatosProducto datos) {
        Producto producto = cargarPropio(usuarioId, productoId);
        producto.actualizar(datos.nombre(), datos.descripcion(), datos.categoria(),
                dinero(datos.precio()), datos.stock(), reloj.instant());
        Producto guardado = productos.guardar(producto);
        cache.invalidarTodo();
        return guardado;
    }

    public void eliminar(UUID usuarioId, UUID productoId) {
        Producto producto = cargarPropio(usuarioId, productoId);
        productos.eliminar(producto.id());
        cache.invalidarTodo();
    }

    /** Moderación (rol ADMIN): oculta o restaura un producto. */
    public Producto moderar(UUID productoId, AccionModeracion accion, String motivo) {
        Producto producto = productos.buscarPorId(productoId)
                .orElseThrow(() -> new NoEncontradoException("El producto no existe"));
        if (accion == AccionModeracion.OCULTAR) {
            producto.ocultar(motivo, reloj.instant());
        } else {
            producto.restaurar(reloj.instant());
        }
        Producto guardado = productos.guardar(producto);
        cache.invalidarTodo();
        return guardado;
    }

    @Transactional(readOnly = true)
    public Optional<Producto> buscarActivo(UUID productoId) {
        return productos.buscarPorId(productoId).filter(Producto::estaActivo);
    }

    @Transactional(readOnly = true)
    public Producto obtenerPublico(UUID productoId) {
        return buscarActivo(productoId)
                .orElseThrow(() -> new NoEncontradoException("El producto no existe"));
    }

    private Producto cargarPropio(UUID usuarioId, UUID productoId) {
        Producto producto = productos.buscarPorId(productoId)
                .orElseThrow(() -> new NoEncontradoException("El producto no existe"));
        Tienda tienda = tiendas.buscarPorId(producto.tiendaId())
                .orElseThrow(() -> new NoEncontradoException("La tienda del producto no existe"));
        if (!tienda.esDe(usuarioId)) {
            throw new OperacionNoPermitidaException("El producto pertenece a otro emprendedor");
        }
        return producto;
    }

    private static Dinero dinero(BigDecimal precio) {
        try {
            return new Dinero(precio);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DatoInvalidoException("Precio inválido: " + e.getMessage());
        }
    }
}
