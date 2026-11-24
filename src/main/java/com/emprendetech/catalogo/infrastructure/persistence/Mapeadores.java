package com.emprendetech.catalogo.infrastructure.persistence;

import com.emprendetech.catalogo.domain.Categoria;
import com.emprendetech.catalogo.domain.EstadoProducto;
import com.emprendetech.catalogo.domain.Producto;
import com.emprendetech.catalogo.domain.Tienda;
import com.emprendetech.shared.Dinero;
import com.emprendetech.shared.Localidad;

/** Traducción entre el modelo de dominio y las entidades JPA (núcleo del patrón Adapter). */
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
}
