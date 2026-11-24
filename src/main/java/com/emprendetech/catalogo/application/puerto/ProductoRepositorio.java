package com.emprendetech.catalogo.application.puerto;

import com.emprendetech.catalogo.domain.Producto;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de productos. */
public interface ProductoRepositorio {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(UUID id);

    void eliminar(UUID id);
}
