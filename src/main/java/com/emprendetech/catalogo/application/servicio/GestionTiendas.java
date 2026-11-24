package com.emprendetech.catalogo.application.servicio;

import com.emprendetech.catalogo.application.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.domain.Tienda;
import com.emprendetech.catalogo.domain.excepcion.ConflictoException;
import com.emprendetech.catalogo.domain.excepcion.NoEncontradoException;
import com.emprendetech.shared.Localidad;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso: tienda virtual del emprendedor. */
@Service
@Transactional
public class GestionTiendas {

    private final TiendaRepositorio tiendas;
    private final Clock reloj;

    public GestionTiendas(TiendaRepositorio tiendas, Clock reloj) {
        this.tiendas = tiendas;
        this.reloj = reloj;
    }

    public Tienda crear(UUID propietarioId, String nombre, String descripcion, Localidad localidad) {
        if (tiendas.buscarPorPropietario(propietarioId).isPresent()) {
            throw new ConflictoException("El emprendedor ya tiene una tienda registrada");
        }
        return tiendas.guardar(Tienda.crear(propietarioId, nombre, descripcion, localidad, reloj.instant()));
    }

    @Transactional(readOnly = true)
    public Tienda obtener(UUID id) {
        return tiendas.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("La tienda no existe"));
    }

    @Transactional(readOnly = true)
    public Tienda obtenerDelPropietario(UUID propietarioId) {
        return tiendas.buscarPorPropietario(propietarioId)
                .orElseThrow(() -> new NoEncontradoException("Aún no ha creado su tienda"));
    }
}
