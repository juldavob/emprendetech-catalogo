package com.emprendetech.catalogo.aplicacion.servicio;

import com.emprendetech.catalogo.aplicacion.dto.AccionModeracion;
import com.emprendetech.catalogo.aplicacion.dto.DatosProducto;
import com.emprendetech.catalogo.aplicacion.dto.ProductoDto;
import com.emprendetech.catalogo.aplicacion.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.aplicacion.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.Transaccion;
import com.emprendetech.catalogo.dominio.Categoria;
import com.emprendetech.catalogo.dominio.Producto;
import com.emprendetech.catalogo.dominio.Tienda;
import com.emprendetech.shared_kernel.Dinero;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import com.emprendetech.shared_kernel.excepcion.NoEncontradoException;
import com.emprendetech.shared_kernel.excepcion.OperacionNoPermitidaException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de productos: CRUD por emprendedor (RF2, CU-02) y moderación (CU-10).
 * La caché se invalida DESPUÉS de confirmar la transacción, para no repoblarla con datos viejos.
 */
public class GestionProductos {

    private final ProductoRepositorio productos;
    private final TiendaRepositorio tiendas;
    private final CacheBusquedaPort cache;
    private final Transaccion tx;
    private final Clock reloj;

    public GestionProductos(ProductoRepositorio productos, TiendaRepositorio tiendas,
                            CacheBusquedaPort cache, Transaccion tx, Clock reloj) {
        this.productos = productos;
        this.tiendas = tiendas;
        this.cache = cache;
        this.tx = tx;
        this.reloj = reloj;
    }

    public ProductoDto crear(UUID propietarioId, DatosProducto datos) {
        ProductoDto creado = tx.escribir(() -> {
            Tienda tienda = tiendas.buscarPorPropietario(propietarioId)
                    .orElseThrow(() -> new NoEncontradoException("Debe crear su tienda antes de publicar productos"));
            Producto nuevo = Producto.crear(tienda.id(), datos.nombre(), datos.descripcion(),
                    Categoria.desde(datos.categoria()), dinero(datos.precio()), datos.stock(), reloj.instant());
            return ProductoDto.desde(productos.guardar(nuevo));
        });
        cache.invalidarTodo();
        return creado;
    }

    public ProductoDto actualizar(UUID usuarioId, UUID productoId, DatosProducto datos) {
        ProductoDto actualizado = tx.escribir(() -> {
            Producto producto = cargarPropio(usuarioId, productoId);
            producto.actualizar(datos.nombre(), datos.descripcion(), Categoria.desde(datos.categoria()),
                    dinero(datos.precio()), datos.stock(), reloj.instant());
            return ProductoDto.desde(productos.guardar(producto));
        });
        cache.invalidarTodo();
        return actualizado;
    }

    public void eliminar(UUID usuarioId, UUID productoId) {
        tx.ejecutar(() -> productos.eliminar(cargarPropio(usuarioId, productoId).id()));
        cache.invalidarTodo();
    }

    /** Moderación (rol ADMIN): oculta o restaura un producto. */
    public ProductoDto moderar(UUID productoId, AccionModeracion accion, String motivo) {
        ProductoDto moderado = tx.escribir(() -> {
            Producto producto = productos.buscarPorId(productoId)
                    .orElseThrow(() -> new NoEncontradoException("El producto no existe"));
            if (accion == AccionModeracion.OCULTAR) {
                producto.ocultar(motivo, reloj.instant());
            } else {
                producto.restaurar(reloj.instant());
            }
            return ProductoDto.desde(productos.guardar(producto));
        });
        cache.invalidarTodo();
        return moderado;
    }

    /** Producto visible para el público; vacío si no existe o está oculto por moderación. */
    public Optional<ProductoDto> buscarActivo(UUID productoId) {
        return tx.leer(() -> productos.buscarPorId(productoId)
                .filter(Producto::estaActivo)
                .map(ProductoDto::desde));
    }

    public ProductoDto obtenerPublico(UUID productoId) {
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
