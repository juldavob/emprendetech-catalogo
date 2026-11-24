package com.emprendetech.catalogo.aplicacion.dto;

import com.emprendetech.catalogo.dominio.Producto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/** DTO de salida de la capa de Aplicación. Es serializable porque también se guarda en la caché. */
public record ProductoDto(UUID id, UUID tiendaId, String nombre, String descripcion, String categoria,
                          BigDecimal precio, int stock, String estado, String motivoModeracion)
        implements Serializable {

    public static ProductoDto desde(Producto p) {
        return new ProductoDto(p.id(), p.tiendaId(), p.nombre(), p.descripcion(), p.categoria().name(),
                p.precio().monto(), p.stock(), p.estado().name(), p.motivoModeracion());
    }
}
