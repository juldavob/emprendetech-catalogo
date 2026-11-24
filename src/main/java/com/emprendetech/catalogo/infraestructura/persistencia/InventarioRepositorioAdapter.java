package com.emprendetech.catalogo.infraestructura.persistencia;

import com.emprendetech.catalogo.aplicacion.puerto.InventarioRepositorio;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Operaciones atómicas de inventario mediante UPDATE condicionado (sin sobreventa). */
@Repository
public class InventarioRepositorioAdapter implements InventarioRepositorio {

    private final ProductoJpaRepository repositorio;

    public InventarioRepositorioAdapter(ProductoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean descontar(UUID productoId, int cantidad) {
        return repositorio.descontarStock(productoId, cantidad) == 1;
    }

    @Override
    public void incrementar(UUID productoId, int cantidad) {
        repositorio.incrementarStock(productoId, cantidad);
    }
}
