package com.emprendetech.catalogo.dominio;

import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import com.emprendetech.shared_kernel.Dinero;
import java.time.Instant;
import java.util.UUID;

/** Producto del catálogo: concentra las invariantes (precio > 0, stock >= 0, moderación). */
public final class Producto {

    private final UUID id;
    private final UUID tiendaId;
    private final Instant creadoEn;

    private String nombre;
    private String descripcion;
    private Categoria categoria;
    private Dinero precio;
    private int stock;
    private EstadoProducto estado;
    private String motivoModeracion;
    private Instant actualizadoEn;

    private Producto(UUID id, UUID tiendaId, EstadoProducto estado, String motivoModeracion,
                     Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.tiendaId = tiendaId;
        this.estado = estado;
        this.motivoModeracion = motivoModeracion;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public static Producto crear(UUID tiendaId, String nombre, String descripcion, Categoria categoria,
                                 Dinero precio, int stock, Instant ahora) {
        Reglas.requerido(ahora, "creadoEn");
        Producto p = new Producto(UUID.randomUUID(), Reglas.requerido(tiendaId, "tienda"),
                EstadoProducto.ACTIVO, null, ahora, ahora);
        p.aplicar(nombre, descripcion, categoria, precio, stock);
        return p;
    }

    /** Rehidrata un producto ya persistido (no revalida). */
    public static Producto reconstruir(UUID id, UUID tiendaId, String nombre, String descripcion,
                                       Categoria categoria, Dinero precio, int stock,
                                       EstadoProducto estado, String motivoModeracion,
                                       Instant creadoEn, Instant actualizadoEn) {
        Producto p = new Producto(id, tiendaId, estado, motivoModeracion, creadoEn, actualizadoEn);
        p.nombre = nombre;
        p.descripcion = descripcion;
        p.categoria = categoria;
        p.precio = precio;
        p.stock = stock;
        return p;
    }

    public void actualizar(String nombre, String descripcion, Categoria categoria,
                           Dinero precio, int stock, Instant ahora) {
        aplicar(nombre, descripcion, categoria, precio, stock);
        this.actualizadoEn = Reglas.requerido(ahora, "actualizadoEn");
    }

    /** Moderación: oculta el producto de búsquedas y compras. */
    public void ocultar(String motivo, Instant ahora) {
        if (estado == EstadoProducto.OCULTO) {
            throw new ConflictoException("El producto ya está oculto");
        }
        this.motivoModeracion = Reglas.textoObligatorio(motivo, "motivo", 300);
        this.estado = EstadoProducto.OCULTO;
        this.actualizadoEn = Reglas.requerido(ahora, "actualizadoEn");
    }

    /** Moderación: vuelve a publicar un producto oculto. */
    public void restaurar(Instant ahora) {
        if (estado == EstadoProducto.ACTIVO) {
            throw new ConflictoException("El producto ya está activo");
        }
        this.motivoModeracion = null;
        this.estado = EstadoProducto.ACTIVO;
        this.actualizadoEn = Reglas.requerido(ahora, "actualizadoEn");
    }

    public boolean estaActivo() {
        return estado == EstadoProducto.ACTIVO;
    }

    private void aplicar(String nombre, String descripcion, Categoria categoria, Dinero precio, int stock) {
        if (stock < 0) {
            throw new DatoInvalidoException("El stock no puede ser negativo");
        }
        this.nombre = Reglas.textoObligatorio(nombre, "nombre", 120);
        this.descripcion = Reglas.textoOpcional(descripcion, "descripcion", 2000);
        this.categoria = Reglas.requerido(categoria, "categoria");
        this.precio = Reglas.requerido(precio, "precio");
        this.stock = stock;
    }

    public UUID id() { return id; }
    public UUID tiendaId() { return tiendaId; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public Categoria categoria() { return categoria; }
    public Dinero precio() { return precio; }
    public int stock() { return stock; }
    public EstadoProducto estado() { return estado; }
    public String motivoModeracion() { return motivoModeracion; }
    public Instant creadoEn() { return creadoEn; }
    public Instant actualizadoEn() { return actualizadoEn; }
}
