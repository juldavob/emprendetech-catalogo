package com.emprendetech.catalogo.aplicacion.puerto;

import com.emprendetech.catalogo.dominio.Producto;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de productos. */
public interface ProductoRepositorio {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(UUID id);

    void eliminar(UUID id);
}
