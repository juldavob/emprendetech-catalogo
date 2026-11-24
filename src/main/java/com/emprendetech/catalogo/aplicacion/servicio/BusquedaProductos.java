package com.emprendetech.catalogo.aplicacion.servicio;

import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;
import com.emprendetech.catalogo.aplicacion.dto.PaginaDto;
import com.emprendetech.catalogo.aplicacion.dto.ProductoDto;
import com.emprendetech.catalogo.aplicacion.puerto.BusquedaProductosPort;
import com.emprendetech.catalogo.aplicacion.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.aplicacion.puerto.ResultadoBusqueda;
import com.emprendetech.catalogo.aplicacion.puerto.Transaccion;
import java.util.Optional;

/**
 * CU-03 (RF3): búsqueda filtrada. Flujo del SAD §4.1: primero Redis; ante cache miss, búsqueda
 * indexada de texto completo en el esquema de Catálogo. Los aciertos de caché no abren transacción.
 */
public class BusquedaProductos {

    private final BusquedaProductosPort busqueda;
    private final CacheBusquedaPort cache;
    private final Transaccion tx;

    public BusquedaProductos(BusquedaProductosPort busqueda, CacheBusquedaPort cache, Transaccion tx) {
        this.busqueda = busqueda;
        this.cache = cache;
        this.tx = tx;
    }

    public PaginaDto<ProductoDto> buscar(CriteriosBusqueda criterios) {
        Optional<PaginaDto<ProductoDto>> enCache = cache.obtener(criterios);
        if (enCache.isPresent()) {
            return enCache.get();
        }
        PaginaDto<ProductoDto> pagina = tx.leer(() -> {
            ResultadoBusqueda resultado = busqueda.buscar(criterios);
            return PaginaDto.de(resultado.productos().stream().map(ProductoDto::desde).toList(),
                    criterios.pagina(), criterios.tamano(), resultado.total());
        });
        cache.guardar(criterios, pagina);
        return pagina;
    }
}
