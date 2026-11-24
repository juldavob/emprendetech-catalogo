package com.emprendetech.catalogo.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Reserva de stock por pedido y producto (esquema "catalogo"; sin llaves foráneas a otros módulos). */
@Entity
@Table(name = "reserva_stock", schema = "catalogo")
public class ReservaStockJpaEntity {

    @Id
    UUID id;

    @Column(name = "pedido_id", nullable = false)
    UUID pedidoId;

    @Column(name = "producto_id", nullable = false)
    UUID productoId;

    @Column(nullable = false)
    int cantidad;

    @Column(nullable = false, length = 20)
    String estado;

    @Column(name = "creada_en", nullable = false)
    Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    Instant actualizadaEn;

    protected ReservaStockJpaEntity() {
        // requerido por JPA
    }
}
