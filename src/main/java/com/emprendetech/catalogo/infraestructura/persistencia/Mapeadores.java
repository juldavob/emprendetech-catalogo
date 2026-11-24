package com.emprendetech.catalogo.infraestructura.persistencia;

import com.emprendetech.catalogo.dominio.Categoria;
import com.emprendetech.catalogo.dominio.EstadoProducto;
import com.emprendetech.catalogo.dominio.EstadoReserva;
import com.emprendetech.catalogo.dominio.Producto;
import com.emprendetech.catalogo.dominio.ReservaStock;
import com.emprendetech.catalogo.dominio.Tienda;
import com.emprendetech.shared_kernel.Dinero;
import com.emprendetech.shared_kernel.Localidad;

/** Traducción entre el modelo de dominio y las entidades JPA. */
final class Mapeadores {

    private Mapeadores() {
    }

    static TiendaJpaEntity aEntidad(Tienda t) {
        TiendaJpaEntity e = new TiendaJpaEntity();
        e.id = t.id();
        e.propietarioId = t.propietarioId();
        e.nombre = t.nombre();
        e.descripcion = t.descripcion();
        e.localidad = t.localidad().name();
        e.creadaEn = t.creadaEn();
        return e;
    }

    static Tienda aDominio(TiendaJpaEntity e) {
        return Tienda.reconstruir(e.id, e.propietarioId, e.nombre, e.descripcion,
                Localidad.valueOf(e.localidad), e.creadaEn);
    }

    static ProductoJpaEntity aEntidad(Producto p) {
        ProductoJpaEntity e = new ProductoJpaEntity();
        e.id = p.id();
        e.tiendaId = p.tiendaId();
        e.nombre = p.nombre();
        e.descripcion = p.descripcion();
        e.categoria = p.categoria().name();
        e.precio = p.precio().monto();
        e.stock = p.stock();
        e.estado = p.estado().name();
        e.motivoModeracion = p.motivoModeracion();
        e.creadoEn = p.creadoEn();
        e.actualizadoEn = p.actualizadoEn();
        return e;
    }

    static Producto aDominio(ProductoJpaEntity e) {
        return Producto.reconstruir(e.id, e.tiendaId, e.nombre, e.descripcion,
                Categoria.valueOf(e.categoria), new Dinero(e.precio), e.stock,
                EstadoProducto.valueOf(e.estado), e.motivoModeracion, e.creadoEn, e.actualizadoEn);
    }

    static ReservaStockJpaEntity aEntidad(ReservaStock r) {
        ReservaStockJpaEntity e = new ReservaStockJpaEntity();
        e.id = r.id();
        e.pedidoId = r.pedidoId();
        e.productoId = r.productoId();
        e.cantidad = r.cantidad();
        e.estado = r.estado().name();
        e.creadaEn = r.creadaEn();
        e.actualizadaEn = r.actualizadaEn();
        return e;
    }

    static ReservaStock aDominio(ReservaStockJpaEntity e) {
        return ReservaStock.reconstruir(e.id, e.pedidoId, e.productoId, e.cantidad,
                EstadoReserva.valueOf(e.estado), e.creadaEn, e.actualizadaEn);
    }
}
