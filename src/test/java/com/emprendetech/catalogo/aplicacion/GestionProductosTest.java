package com.emprendetech.catalogo.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendetech.catalogo.aplicacion.dto.AccionModeracion;
import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;
import com.emprendetech.catalogo.aplicacion.dto.DatosProducto;
import com.emprendetech.catalogo.aplicacion.dto.PaginaDto;
import com.emprendetech.catalogo.aplicacion.dto.ProductoDto;
import com.emprendetech.catalogo.aplicacion.puerto.BusquedaProductosPort;
import com.emprendetech.catalogo.aplicacion.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.aplicacion.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.ResultadoBusqueda;
import com.emprendetech.catalogo.aplicacion.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.aplicacion.servicio.BusquedaProductos;
import com.emprendetech.catalogo.aplicacion.servicio.GestionProductos;
import com.emprendetech.catalogo.dominio.Producto;
import com.emprendetech.catalogo.dominio.Tienda;
import com.emprendetech.shared_kernel.Localidad;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import com.emprendetech.shared_kernel.excepcion.NoEncontradoException;
import com.emprendetech.shared_kernel.excepcion.OperacionNoPermitidaException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Casos de uso probados SIN Spring y SIN base de datos: la capa de Aplicación es Java puro y
 * depende solo de puertos, así que bastan adaptadores en memoria.
 */
class GestionProductosTest {

    private static final UUID DUENO = UUID.randomUUID();
    private static final UUID OTRO = UUID.randomUUID();

    private final Map<UUID, Producto> productosDb = new HashMap<>();
    private final Map<UUID, Tienda> tiendasDb = new HashMap<>();
    private final Map<CriteriosBusqueda, PaginaDto<ProductoDto>> memoria = new HashMap<>();
    private final AtomicInteger invalidaciones = new AtomicInteger();
    private final AtomicInteger consultasABd = new AtomicInteger();

    private final ProductoRepositorio productoRepo = new ProductoRepositorio() {
        @Override public Producto guardar(Producto p) { productosDb.put(p.id(), p); return p; }
        @Override public Optional<Producto> buscarPorId(UUID id) { return Optional.ofNullable(productosDb.get(id)); }
        @Override public void eliminar(UUID id) { productosDb.remove(id); }
    };

    private final TiendaRepositorio tiendaRepo = new TiendaRepositorio() {
        @Override public Tienda guardar(Tienda t) { tiendasDb.put(t.id(), t); return t; }
        @Override public Optional<Tienda> buscarPorId(UUID id) { return Optional.ofNullable(tiendasDb.get(id)); }
        @Override public Optional<Tienda> buscarPorPropietario(UUID p) {
            return tiendasDb.values().stream().filter(t -> t.esDe(p)).findFirst();
        }
    };

    private final CacheBusquedaPort cache = new CacheBusquedaPort() {
        @Override public Optional<PaginaDto<ProductoDto>> obtener(CriteriosBusqueda c) { return Optional.ofNullable(memoria.get(c)); }
        @Override public void guardar(CriteriosBusqueda c, PaginaDto<ProductoDto> r) { memoria.put(c, r); }
        @Override public void invalidarTodo() { memoria.clear(); invalidaciones.incrementAndGet(); }
    };

    private final BusquedaProductosPort puertoBusqueda = criterios -> {
        consultasABd.incrementAndGet();
        return new ResultadoBusqueda(List.copyOf(productosDb.values()), productosDb.size());
    };

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    private final TransaccionDirecta tx = new TransaccionDirecta();
    private final GestionProductos servicio = new GestionProductos(productoRepo, tiendaRepo, cache, tx, reloj);
    private final BusquedaProductos busqueda = new BusquedaProductos(puertoBusqueda, cache, tx);

    private static DatosProducto datos() {
        return new DatosProducto("Miel multifloral", "500 g", "alimentos", new BigDecimal("28000"), 50);
    }

    private void tiendaDe(UUID propietario) {
        tiendaRepo.guardar(Tienda.crear(propietario, "Mi tienda", null, Localidad.SUBA, reloj.instant()));
    }

    @Test
    void crearProductoSinTiendaFalla() {
        assertThrows(NoEncontradoException.class, () -> servicio.crear(DUENO, datos()));
    }

    @Test
    void crearProductoLoPersisteConCategoriaNormalizadaEInvalidaLaCache() {
        tiendaDe(DUENO);
        ProductoDto p = servicio.crear(DUENO, datos());
        assertTrue(productosDb.containsKey(p.id()));
        assertEquals("ALIMENTOS", p.categoria());
        assertEquals("ACTIVO", p.estado());
        assertEquals(1, invalidaciones.get());
    }

    @Test
    void categoriaOPrecioInvalidosSonRechazados() {
        tiendaDe(DUENO);
        assertThrows(DatoInvalidoException.class,
                () -> servicio.crear(DUENO, new DatosProducto("X", null, "COHETES", BigDecimal.TEN, 1)));
        assertThrows(DatoInvalidoException.class,
                () -> servicio.crear(DUENO, new DatosProducto("X", null, "HOGAR", BigDecimal.ZERO, 1)));
        assertTrue(productosDb.isEmpty());
    }

    @Test
    void unEmprendedorNoPuedeActualizarNiEliminarProductosAjenos() {
        tiendaDe(DUENO);
        ProductoDto p = servicio.crear(DUENO, datos());
        assertThrows(OperacionNoPermitidaException.class, () -> servicio.actualizar(OTRO, p.id(), datos()));
        assertThrows(OperacionNoPermitidaException.class, () -> servicio.eliminar(OTRO, p.id()));
        assertTrue(productosDb.containsKey(p.id()));
    }

    @Test
    void eliminarProductoPropioLoBorra() {
        tiendaDe(DUENO);
        ProductoDto p = servicio.crear(DUENO, datos());
        servicio.eliminar(DUENO, p.id());
        assertFalse(productosDb.containsKey(p.id()));
    }

    @Test
    void productoOcultoPorModeracionNoEsVisibleParaElPublico() {
        tiendaDe(DUENO);
        ProductoDto p = servicio.crear(DUENO, datos());
        servicio.moderar(p.id(), AccionModeracion.OCULTAR, "Contenido inapropiado");
        assertThrows(NoEncontradoException.class, () -> servicio.obtenerPublico(p.id()));
        assertTrue(servicio.buscarActivo(p.id()).isEmpty());

        servicio.moderar(p.id(), AccionModeracion.RESTAURAR, null);
        assertEquals(p.id(), servicio.obtenerPublico(p.id()).id());
    }

    @Test
    void laBusquedaRepetidaSeSirveDesdeLaCacheHastaQueHayaUnaEscritura() {
        tiendaDe(DUENO);
        CriteriosBusqueda criterios = new CriteriosBusqueda("miel", null, null, null, null, 0, 20);

        busqueda.buscar(criterios);
        busqueda.buscar(criterios);
        assertEquals(1, consultasABd.get(), "la segunda búsqueda debe salir de la caché");

        servicio.crear(DUENO, datos());          // escritura => invalida la caché
        PaginaDto<ProductoDto> pagina = busqueda.buscar(criterios);
        assertEquals(2, consultasABd.get());
        assertEquals(1L, pagina.total());
    }
}
