package com.emprendetech.catalogo.aplicacion.servicio;

import com.emprendetech.catalogo.aplicacion.dto.TiendaDto;
import com.emprendetech.catalogo.aplicacion.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.Transaccion;
import com.emprendetech.catalogo.dominio.Tienda;
import com.emprendetech.shared_kernel.Localidad;
import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import com.emprendetech.shared_kernel.excepcion.NoEncontradoException;
import java.time.Clock;
import java.util.UUID;

/** Casos de uso de la tienda virtual del emprendedor (CU-02). Clase Java pura: se cablea en infraestructura. */
public class GestionTiendas {

    private final TiendaRepositorio tiendas;
    private final Transaccion tx;
    private final Clock reloj;

    public GestionTiendas(TiendaRepositorio tiendas, Transaccion tx, Clock reloj) {
        this.tiendas = tiendas;
        this.tx = tx;
        this.reloj = reloj;
    }

    public TiendaDto crear(UUID propietarioId, String nombre, String descripcion, Localidad localidad) {
        return tx.escribir(() -> {
            if (tiendas.buscarPorPropietario(propietarioId).isPresent()) {
                throw new ConflictoException("El emprendedor ya tiene una tienda registrada");
            }
            Tienda nueva = Tienda.crear(propietarioId, nombre, descripcion, localidad, reloj.instant());
            return TiendaDto.desde(tiendas.guardar(nueva));
        });
    }

    public TiendaDto obtener(UUID id) {
        return tx.leer(() -> tiendas.buscarPorId(id)
                .map(TiendaDto::desde)
                .orElseThrow(() -> new NoEncontradoException("La tienda no existe")));
    }

    public TiendaDto obtenerDelPropietario(UUID propietarioId) {
        return tx.leer(() -> tiendas.buscarPorPropietario(propietarioId)
                .map(TiendaDto::desde)
                .orElseThrow(() -> new NoEncontradoException("Aún no ha creado su tienda")));
    }
}
