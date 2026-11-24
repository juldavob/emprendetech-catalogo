package com.emprendetech.catalogo.infraestructura.persistencia;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaJpaRepository extends JpaRepository<TiendaJpaEntity, UUID> {

    Optional<TiendaJpaEntity> findByPropietarioId(UUID propietarioId);
}
