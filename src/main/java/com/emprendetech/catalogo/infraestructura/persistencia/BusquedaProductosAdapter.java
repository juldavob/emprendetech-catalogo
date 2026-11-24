package com.emprendetech.catalogo.infraestructura.persistencia;

import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;
import com.emprendetech.catalogo.aplicacion.puerto.BusquedaProductosPort;
import com.emprendetech.catalogo.aplicacion.puerto.ResultadoBusqueda;
import com.emprendetech.catalogo.dominio.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de consulta (RF3, RNF3): arma una consulta SQL parametrizada con filtros por localidad,
 * categoría y precio, y búsqueda de texto completo de PostgreSQL (índice GIN sobre "busqueda").
 * Todos los valores del usuario viajan como parámetros enlazados; el SQL variable es solo constante.
 */
@Repository
public class BusquedaProductosAdapter implements BusquedaProductosPort {

    private static final String ORDEN_POR_RELEVANCIA =
            "ts_rank(p.busqueda, plainto_tsquery('spanish', :texto)) DESC, p.creado_en DESC, p.id";
    private static final String ORDEN_POR_RECIENTES = "p.creado_en DESC, p.id";

    @PersistenceContext
    private EntityManager em;

    @Override
    public ResultadoBusqueda buscar(CriteriosBusqueda criterios) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        StringBuilder desde = new StringBuilder(
                " FROM catalogo.producto p JOIN catalogo.tienda t ON t.id = p.tienda_id"
                        + " WHERE p.estado = 'ACTIVO'");
        if (criterios.texto() != null) {
            desde.append(" AND p.busqueda @@ plainto_tsquery('spanish', :texto)");
            parametros.put("texto", criterios.texto());
        }
        if (criterios.localidad() != null) {
            desde.append(" AND t.localidad = :localidad");
            parametros.put("localidad", criterios.localidad().name());
        }
        if (criterios.categoria() != null) {
            desde.append(" AND p.categoria = :categoria");
            parametros.put("categoria", criterios.categoria());
        }
        if (criterios.precioMin() != null) {
            desde.append(" AND p.precio >= :precioMin");
            parametros.put("precioMin", criterios.precioMin());
        }
        if (criterios.precioMax() != null) {
            desde.append(" AND p.precio <= :precioMax");
            parametros.put("precioMax", criterios.precioMax());
        }

        Query conteo = em.createNativeQuery("SELECT count(*)" + desde);
        parametros.forEach((nombre, valor) -> conteo.setParameter(nombre, valor));
        long total = ((Number) conteo.getSingleResult()).longValue();

        String orden = criterios.texto() != null ? ORDEN_POR_RELEVANCIA : ORDEN_POR_RECIENTES;
        Query consulta = em.createNativeQuery("SELECT p.*" + desde + " ORDER BY " + orden, ProductoJpaEntity.class);
        parametros.forEach((nombre, valor) -> consulta.setParameter(nombre, valor));
        long desplazamiento = (long) criterios.pagina() * criterios.tamano();
        consulta.setFirstResult((int) Math.min(desplazamiento, Integer.MAX_VALUE));
        consulta.setMaxResults(criterios.tamano());

        List<?> filas = consulta.getResultList();
        List<Producto> productos = filas.stream()
                .map(fila -> Mapeadores.aDominio((ProductoJpaEntity) fila))
                .toList();
        return new ResultadoBusqueda(productos, total);
    }
}
