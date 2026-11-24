package com.emprendetech.catalogo.infrastructure.persistence;

import com.emprendetech.catalogo.application.puerto.InventarioRepositorio;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** ADAPTER: operaciones atómicas de inventario mediante UPDATE condicionado (sin sobreventa). */
@Repository
public class InventarioRepositorioAdapter implements InventarioRepositorio {

    private final ProductoJpaRepository repositorio;

    public InventarioRepositorioAdapter(ProductoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean reservar(UUID productoId, int cantidad) {
        return repositorio.descontarStock(productoId, cantidad) == 1;
    }

    @Override
    public boolean liberar(UUID productoId, int cantidad) {
        return repositorio.incrementarStock(productoId, cantidad) == 1;
    }
}
