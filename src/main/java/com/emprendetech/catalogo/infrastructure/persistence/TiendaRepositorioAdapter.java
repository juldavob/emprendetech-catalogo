package com.emprendetech.catalogo.infrastructure.persistence;

import com.emprendetech.catalogo.application.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.domain.Tienda;
import com.emprendetech.catalogo.domain.excepcion.ConflictoException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** ADAPTER: implementa el puerto {@link TiendaRepositorio} sobre Spring Data JPA. */
@Repository
public class TiendaRepositorioAdapter implements TiendaRepositorio {

    private final TiendaJpaRepository repositorio;

    public TiendaRepositorioAdapter(TiendaJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Tienda guardar(Tienda tienda) {
        try {
            return Mapeadores.aDominio(repositorio.saveAndFlush(Mapeadores.aEntidad(tienda)));
        } catch (DataIntegrityViolationException e) {
            // Carrera entre dos solicitudes del mismo emprendedor: lo resuelve la restricción UNIQUE.
            throw new ConflictoException("El emprendedor ya tiene una tienda registrada");
        }
    }

    @Override
    public Optional<Tienda> buscarPorId(UUID id) {
        return repositorio.findById(id).map(Mapeadores::aDominio);
    }

    @Override
    public Optional<Tienda> buscarPorPropietario(UUID propietarioId) {
        return repositorio.findByPropietarioId(propietarioId).map(Mapeadores::aDominio);
    }
}
