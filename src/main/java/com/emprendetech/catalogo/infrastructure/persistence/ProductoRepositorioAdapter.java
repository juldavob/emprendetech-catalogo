package com.emprendetech.catalogo.infrastructure.persistence;

import com.emprendetech.catalogo.application.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.domain.Producto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** ADAPTER: implementa el puerto {@link ProductoRepositorio} sobre Spring Data JPA. */
@Repository
public class ProductoRepositorioAdapter implements ProductoRepositorio {

    private final ProductoJpaRepository repositorio;

    public ProductoRepositorioAdapter(ProductoJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Producto guardar(Producto producto) {
        return Mapeadores.aDominio(repositorio.saveAndFlush(Mapeadores.aEntidad(producto)));
    }

    @Override
    public Optional<Producto> buscarPorId(UUID id) {
        return repositorio.findById(id).map(Mapeadores::aDominio);
    }

    @Override
    public void eliminar(UUID id) {
        repositorio.deleteById(id);
        repositorio.flush();
    }
}
