package com.emprendetech.catalogo.infrastructure.persistence;

import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.OrdenProductos;
import com.emprendetech.catalogo.application.modelo.Pagina;
import com.emprendetech.catalogo.application.puerto.BusquedaProductosPort;
import com.emprendetech.catalogo.domain.Producto;
import com.emprendetech.catalogo.infrastructure.persistence.orden.EstrategiaOrdenamiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * ADAPTER del puerto {@link BusquedaProductosPort}: traduce los criterios a una consulta SQL
 * parametrizada (RF3) con búsqueda de texto completo de PostgreSQL (RNF3). Todos los valores del
 * usuario viajan como parámetros; el único SQL variable proviene de las {@link EstrategiaOrdenamiento}.
 */
@Repository
public class BusquedaProductosAdapter implements BusquedaProductosPort {

    @PersistenceContext
    private EntityManager em;

    private final Map<OrdenProductos, EstrategiaOrdenamiento> estrategias = new EnumMap<>(OrdenProductos.class);

    public BusquedaProductosAdapter(List<EstrategiaOrdenamiento> disponibles) {
        disponibles.forEach(e -> estrategias.put(e.orden(), e));
    }

    @Override
    public Pagina<Producto> buscar(CriteriosBusqueda criterios) {
        EstrategiaOrdenamiento estrategia = estrategias.get(criterios.orden());
        if (estrategia == null) {
            throw new IllegalStateException("No hay estrategia de ordenamiento para " + criterios.orden());
        }

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
            parametros.put("categoria", criterios.categoria().name());
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

        Query consulta = em.createNativeQuery(
                "SELECT p.*" + desde + " ORDER BY " + estrategia.clausulaOrderBy(),
                ProductoJpaEntity.class);
        parametros.forEach((nombre, valor) -> consulta.setParameter(nombre, valor));
        long desplazamiento = (long) criterios.pagina() * criterios.tamano();
        consulta.setFirstResult((int) Math.min(desplazamiento, Integer.MAX_VALUE));
        consulta.setMaxResults(criterios.tamano());

        List<?> filas = consulta.getResultList();
        List<Producto> productos = filas.stream()
                .map(fila -> Mapeadores.aDominio((ProductoJpaEntity) fila))
                .toList();
        return new Pagina<>(productos, criterios.pagina(), criterios.tamano(), total);
    }
}
