package com.emprendetech.catalogo.infraestructura.persistencia;

import com.emprendetech.catalogo.aplicacion.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.dominio.Producto;
import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Adaptador de persistencia (SAD Tabla 4): implementa el puerto {@link ProductoRepositorio} con Spring Data JPA. */
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
        try {
            repositorio.deleteById(id);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("El producto tiene reservas de stock asociadas y no puede eliminarse");
        }
    }
}
