package com.emprendetech.catalogo.aplicacion.dto;

import com.emprendetech.catalogo.dominio.Tienda;
import com.emprendetech.shared_kernel.Localidad;
import java.util.UUID;

public record TiendaDto(UUID id, UUID propietarioId, String nombre, String descripcion, Localidad localidad) {

    public static TiendaDto desde(Tienda t) {
        return new TiendaDto(t.id(), t.propietarioId(), t.nombre(), t.descripcion(), t.localidad());
    }
}
