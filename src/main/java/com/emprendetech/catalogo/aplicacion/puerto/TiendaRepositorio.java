package com.emprendetech.catalogo.aplicacion.puerto;

import com.emprendetech.catalogo.dominio.Tienda;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de tiendas (lo implementa un adaptador de infraestructura). */
public interface TiendaRepositorio {

    Tienda guardar(Tienda tienda);

    Optional<Tienda> buscarPorId(UUID id);

    Optional<Tienda> buscarPorPropietario(UUID propietarioId);
}
