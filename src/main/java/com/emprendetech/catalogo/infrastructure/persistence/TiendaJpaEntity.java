package com.emprendetech.catalogo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Estructura de persistencia (no es el modelo de dominio). Esquema propio del módulo: "catalogo". */
@Entity
@Table(name = "tienda", schema = "catalogo")
public class TiendaJpaEntity {

    @Id
    UUID id;

    @Column(name = "propietario_id", nullable = false)
    UUID propietarioId;

    @Column(nullable = false, length = 120)
    String nombre;

    @Column(length = 500)
    String descripcion;

    @Column(nullable = false, length = 40)
    String localidad;

    @Column(name = "creada_en", nullable = false)
    Instant creadaEn;

    protected TiendaJpaEntity() {
        // requerido por JPA
    }
}
