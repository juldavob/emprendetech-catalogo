package com.emprendetech.catalogo.aplicacion.puerto;

import com.emprendetech.catalogo.dominio.ReservaStock;
import java.util.List;
import java.util.UUID;

/** Puerto de salida: persistencia de las reservas de stock (esquema propio de Catálogo). */
public interface ReservaRepositorio {

    void guardarTodas(List<ReservaStock> reservas);

    List<ReservaStock> buscarPorPedido(UUID pedidoId);

    boolean existePorPedido(UUID pedidoId);
}
