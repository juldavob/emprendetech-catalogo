package com.emprendetech.catalogo.infraestructura.persistencia;

import com.emprendetech.catalogo.aplicacion.puerto.ReservaRepositorio;
import com.emprendetech.catalogo.dominio.ReservaStock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ReservaRepositorioAdapter implements ReservaRepositorio {

    private final ReservaStockJpaRepository repositorio;

    public ReservaRepositorioAdapter(ReservaStockJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardarTodas(List<ReservaStock> reservas) {
        repositorio.saveAllAndFlush(reservas.stream().map(Mapeadores::aEntidad).toList());
    }

    @Override
    public List<ReservaStock> buscarPorPedido(UUID pedidoId) {
        return repositorio.findByPedidoId(pedidoId).stream().map(Mapeadores::aDominio).toList();
    }

    @Override
    public boolean existePorPedido(UUID pedidoId) {
        return repositorio.existsByPedidoId(pedidoId);
    }
}
