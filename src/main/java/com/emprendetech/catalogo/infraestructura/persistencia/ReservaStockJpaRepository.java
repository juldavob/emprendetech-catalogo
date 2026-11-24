package com.emprendetech.catalogo.infraestructura.persistencia;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaStockJpaRepository extends JpaRepository<ReservaStockJpaEntity, UUID> {

    List<ReservaStockJpaEntity> findByPedidoId(UUID pedidoId);

    boolean existsByPedidoId(UUID pedidoId);
}
