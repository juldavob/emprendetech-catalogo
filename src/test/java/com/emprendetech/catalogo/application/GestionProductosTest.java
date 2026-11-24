package com.emprendetech.catalogo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendetech.catalogo.application.modelo.AccionModeracion;
import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.DatosProducto;
import com.emprendetech.catalogo.application.modelo.Pagina;
import com.emprendetech.catalogo.application.puerto.BusquedaProductosPort;
import com.emprendetech.catalogo.application.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.application.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.application.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.application.servicio.BusquedaProductos;
import com.emprendetech.catalogo.application.servicio.GestionProductos;
import com.emprendetech.catalogo.domain.Categoria;
import com.emprendetech.catalogo.domain.Producto;
import com.emprendetech.catalogo.domain.Tienda;
import com.emprendetech.catalogo.domain.excepcion.NoEncontradoException;
import com.emprendetech.catalogo.domain.excepcion.OperacionNoPermitidaException;
import com.emprendetech.shared.Localidad;
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
 * Prueba de los casos de uso SIN Spring y SIN base de datos: gracias a los puertos de Clean
 * Architecture basta con adaptadores en memoria.
 */
class GestionProductosTest {

    private static final UUID DUENO = UUID.randomUUID();
    private static final UUID OTRO = UUID.randomUUID();

    private final Map<UUID, Producto> productosDb = new HashMap<>();
    private final Map<UUID, Tienda> tiendasDb = new HashMap<>();
    private final AtomicInteger invalidaciones = new AtomicInteger();
    private final AtomicInteger consultasABd = new AtomicInteger();
    private final Map<CriteriosBusqueda, Pagina<Producto>> memoria = new HashMap<>();

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
        @Override public Optional<Pagina<Producto>> obtener(CriteriosBusqueda c) { return Optional.ofNullable(memoria.get(c)); }
        @Override public void guardar(CriteriosBusqueda c, Pagina<Producto> r) { memoria.put(c, r); }
        @Override public void invalidarTodo() { memoria.clear(); invalidaciones.incrementAndGet(); }
    };

    private final BusquedaProductosPort puertoBusqueda = criterios -> {
        consultasABd.incrementAndGet();
        return new Pagina<>(List.copyOf(productosDb.values()), criterios.pagina(), criterios.tamano(), productosDb.size());
    };

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC);
    private final GestionProductos servicio = new GestionProductos(productoRepo, tiendaRepo, cache, reloj);
    private final BusquedaProductos busqueda = new BusquedaProductos(puertoBusqueda, cache);

    private static DatosProducto datos() {
        return new DatosProducto("Miel multifloral", "500 g", Categoria.ALIMENTOS, new BigDecimal("28000"), 50);
    }

    private Tienda tiendaDe(UUID propietario) {
        return tiendaRepo.guardar(Tienda.crear(propietario, "Mi tienda", null, Localidad.SUBA, reloj.instant()));
    }

    @Test
    void crearProductoSinTiendaFalla() {
        assertThrows(NoEncontradoException.class, () -> servicio.crear(DUENO, datos()));
    }

    @Test
    void crearProductoLoPersisteEInvalidaLaCache() {
        tiendaDe(DUENO);
        Producto p = servicio.crear(DUENO, datos());
        assertTrue(productosDb.containsKey(p.id()));
        assertEquals(1, invalidaciones.get());
    }

    @Test
    void unEmprendedorNoPuedeActualizarNiEliminarProductosAjenos() {
        tiendaDe(DUENO);
        Producto p = servicio.crear(DUENO, datos());
        assertThrows(OperacionNoPermitidaException.class, () -> servicio.actualizar(OTRO, p.id(), datos()));
        assertThrows(OperacionNoPermitidaException.class, () -> servicio.eliminar(OTRO, p.id()));
        assertTrue(productosDb.containsKey(p.id()));
    }

    @Test
    void eliminarProductoPropioLoBorra() {
        tiendaDe(DUENO);
        Producto p = servicio.crear(DUENO, datos());
        servicio.eliminar(DUENO, p.id());
        assertFalse(productosDb.containsKey(p.id()));
    }

    @Test
    void productoOcultoPorModeracionNoEsVisibleParaElPublico() {
        tiendaDe(DUENO);
        Producto p = servicio.crear(DUENO, datos());
        servicio.moderar(p.id(), AccionModeracion.OCULTAR, "Contenido inapropiado");
        assertThrows(NoEncontradoException.class, () -> servicio.obtenerPublico(p.id()));

        servicio.moderar(p.id(), AccionModeracion.RESTAURAR, null);
        assertEquals(p.id(), servicio.obtenerPublico(p.id()).id());
    }

    @Test
    void laBusquedaRepetidaSeSirveDesdeLaCacheHastaQueHayaUnaEscritura() {
        tiendaDe(DUENO);
        CriteriosBusqueda criterios = new CriteriosBusqueda("miel", null, null, null, null, null, 0, 20);

        busqueda.buscar(criterios);
        busqueda.buscar(criterios);
        assertEquals(1, consultasABd.get(), "la segunda búsqueda debe salir de la caché");

        servicio.crear(DUENO, datos());          // escritura => invalida la caché
        busqueda.buscar(criterios);
        assertEquals(2, consultasABd.get());
    }
}
