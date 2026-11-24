package com.emprendetech.catalogo.dominio;

import com.emprendetech.shared_kernel.Localidad;
import java.time.Instant;
import java.util.UUID;

/** Tienda virtual de un emprendedor. Regla: un emprendedor tiene una sola tienda. */
public final class Tienda {

    private final UUID id;
    private final UUID propietarioId;
    private final String nombre;
    private final String descripcion;
    private final Localidad localidad;
    private final Instant creadaEn;

    private Tienda(UUID id, UUID propietarioId, String nombre, String descripcion,
                   Localidad localidad, Instant creadaEn) {
        this.id = id;
        this.propietarioId = propietarioId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.localidad = localidad;
        this.creadaEn = creadaEn;
    }

    public static Tienda crear(UUID propietarioId, String nombre, String descripcion,
                               Localidad localidad, Instant ahora) {
        return new Tienda(
                UUID.randomUUID(),
                Reglas.requerido(propietarioId, "propietario"),
                Reglas.textoObligatorio(nombre, "nombre", 120),
                Reglas.textoOpcional(descripcion, "descripcion", 500),
                Reglas.requerido(localidad, "localidad"),
                Reglas.requerido(ahora, "creadaEn"));
    }

    /** Rehidrata una tienda ya persistida (no revalida). */
    public static Tienda reconstruir(UUID id, UUID propietarioId, String nombre, String descripcion,
                                     Localidad localidad, Instant creadaEn) {
        return new Tienda(id, propietarioId, nombre, descripcion, localidad, creadaEn);
    }

    public boolean esDe(UUID usuarioId) {
        return propietarioId.equals(usuarioId);
    }

    public UUID id() { return id; }
    public UUID propietarioId() { return propietarioId; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public Localidad localidad() { return localidad; }
    public Instant creadaEn() { return creadaEn; }
}
