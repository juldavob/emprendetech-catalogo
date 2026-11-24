package com.emprendetech.catalogo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Estructura de persistencia del producto. La columna generada "busqueda" (tsvector) no se mapea:
 * la mantiene PostgreSQL y solo se usa en consultas nativas de búsqueda.
 */
@Entity
@Table(name = "producto", schema = "catalogo")
public class ProductoJpaEntity {

    @Id
    UUID id;

    @Column(name = "tienda_id", nullable = false)
    UUID tiendaId;

    @Column(nullable = false, length = 120)
    String nombre;

    @Column(length = 2000)
    String descripcion;

    @Column(nullable = false, length = 30)
    String categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal precio;

    @Column(nullable = false)
    int stock;

    @Column(nullable = false, length = 20)
    String estado;

    @Column(name = "motivo_moderacion", length = 300)
    String motivoModeracion;

    @Column(name = "creado_en", nullable = false)
    Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    Instant actualizadoEn;

    protected ProductoJpaEntity() {
        // requerido por JPA
    }
}
