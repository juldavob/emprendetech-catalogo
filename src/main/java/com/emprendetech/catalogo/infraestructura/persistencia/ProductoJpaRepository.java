package com.emprendetech.catalogo.infraestructura.persistencia;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoJpaRepository extends JpaRepository<ProductoJpaEntity, UUID> {

    /** Descuento atómico: solo actúa si el producto está activo y hay existencias suficientes. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductoJpaEntity p SET p.stock = p.stock - :cantidad "
            + "WHERE p.id = :id AND p.estado = 'ACTIVO' AND p.stock >= :cantidad")
    int descontarStock(@Param("id") UUID id, @Param("cantidad") int cantidad);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductoJpaEntity p SET p.stock = p.stock + :cantidad WHERE p.id = :id")
    int incrementarStock(@Param("id") UUID id, @Param("cantidad") int cantidad);
}
