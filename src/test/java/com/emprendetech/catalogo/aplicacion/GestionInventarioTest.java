package com.emprendetech.catalogo.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.emprendetech.catalogo.aplicacion.dto.LineaReserva;
import com.emprendetech.catalogo.aplicacion.fachada.CatalogoFacade;
import com.emprendetech.catalogo.aplicacion.fachada.ItemReserva;
import com.emprendetech.catalogo.aplicacion.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.aplicacion.puerto.InventarioRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.ReservaRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.aplicacion.servicio.GestionInventario;
import com.emprendetech.catalogo.aplicacion.servicio.GestionProductos;
import com.emprendetech.catalogo.dominio.Categoria;
import com.emprendetech.catalogo.dominio.EstadoReserva;
import com.emprendetech.catalogo.dominio.Producto;
import com.emprendetech.catalogo.dominio.ReservaStock;
import com.emprendetech.shared_kernel.Dinero;
import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import com.emprendetech.shared_kernel.excepcion.NoEncontradoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Reserva de stock en dos fases (SAD §4.1 y §6), probada a través de la fachada pública. */
class GestionInventarioTest {

    private final Map<UUID, Producto> catalogo = new HashMap<>();
    private final Map<UUID, Integer> stock = new HashMap<>();
    private final List<ReservaStock> reservasDb = new ArrayList<>();
    private final AtomicInteger invalidaciones = new AtomicInteger();

    private final InventarioRepositorio inventario = new InventarioRepositorio() {
        @Override public boolean descontar(UUID id, int n) {
            Integer actual = stock.get(id);
            if (actual == null || actual < n) return false;
            stock.put(id, actual - n);
            return true;
        }
        @Override public void incrementar(UUID id, int n) { stock.merge(id, n, Integer::sum); }
    };

    private final ReservaRepositorio reservas = new ReservaRepositorio() {
        @Override public void guardarTodas(List<ReservaStock> rs) {
            rs.forEach(r -> { reservasDb.removeIf(x -> x.id().equals(r.id())); reservasDb.add(r); });
        }
        @Override public List<ReservaStock> buscarPorPedido(UUID p) {
            return reservasDb.stream().filter(r -> r.pedidoId().equals(p)).toList();
        }
        @Override public boolean existePorPedido(UUID p) { return !buscarPorPedido(p).isEmpty(); }
    };

    private final ProductoRepositorio productos = new ProductoRepositorio() {
        @Override public Producto guardar(Producto p) { catalogo.put(p.id(), p); return p; }
        @Override public Optional<Producto> buscarPorId(UUID id) { return Optional.ofNullable(catalogo.get(id)); }
        @Override public void eliminar(UUID id) { catalogo.remove(id); }
    };

    private final CacheBusquedaPort cache = new CacheBusquedaPort() {
        @Override public Optional<com.emprendetech.catalogo.aplicacion.dto.PaginaDto<com.emprendetech.catalogo.aplicacion.dto.ProductoDto>> obtener(com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda c) { return Optional.empty(); }
        @Override public void guardar(com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda c, com.emprendetech.catalogo.aplicacion.dto.PaginaDto<com.emprendetech.catalogo.aplicacion.dto.ProductoDto> r) { }
        @Override public void invalidarTodo() { invalidaciones.incrementAndGet(); }
    };

    private CatalogoFacade fachada;
    private UUID cafe;
    private UUID miel;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
        TransaccionDirecta tx = new TransaccionDirecta();
        TiendaRepositorio sinTiendas = null; // no se usa en las reservas
        GestionProductos gp = new GestionProductos(productos, sinTiendas, cache, tx, reloj);
        GestionInventario gi = new GestionInventario(inventario, reservas, productos, cache, tx, reloj);
        fachada = new CatalogoFacade(gp, gi);
        cafe = producto("Café", 10);
        miel = producto("Miel", 5);
    }

    private UUID producto(String nombre, int existencias) {
        Producto p = Producto.crear(UUID.randomUUID(), nombre, null, Categoria.ALIMENTOS,
                new Dinero(new BigDecimal("1000")), existencias, Instant.parse("2026-11-20T09:00:00Z"));
        catalogo.put(p.id(), p);
        stock.put(p.id(), existencias);
        return p.id();
    }

    @Test
    void reservarDescuentaElStockDeTodosLosItemsYRegistraLasReservas() {
        UUID pedido = UUID.randomUUID();
        fachada.reservarStock(pedido, List.of(new ItemReserva(cafe, 3), new ItemReserva(miel, 5)));

        assertEquals(7, stock.get(cafe));
        assertEquals(0, stock.get(miel));
        assertEquals(2, reservas.buscarPorPedido(pedido).size());
        assertEquals(1, invalidaciones.get());
    }

    @Test
    void stockInsuficienteFallaConConflicto() {
        assertThrows(ConflictoException.class,
                () -> fachada.reservarStock(UUID.randomUUID(), List.of(new ItemReserva(cafe, 11))));
        assertEquals(10, stock.get(cafe));
    }

    @Test
    void productoInexistenteFallaConNoEncontrado() {
        assertThrows(NoEncontradoException.class,
                () -> fachada.reservarStock(UUID.randomUUID(), List.of(new ItemReserva(UUID.randomUUID(), 1))));
    }

    @Test
    void noSePuedeReservarDosVecesElMismoPedido() {
        UUID pedido = UUID.randomUUID();
        fachada.reservarStock(pedido, List.of(new ItemReserva(cafe, 1)));
        assertThrows(ConflictoException.class, () -> fachada.reservarStock(pedido, List.of(new ItemReserva(cafe, 1))));
        assertEquals(9, stock.get(cafe));
    }

    @Test
    void entradasInvalidasSonRechazadasSinTocarElStock() {
        UUID pedido = UUID.randomUUID();
        assertThrows(DatoInvalidoException.class, () -> fachada.reservarStock(pedido, List.of()));
        assertThrows(DatoInvalidoException.class, () -> fachada.reservarStock(pedido, null));
        assertThrows(DatoInvalidoException.class, () -> fachada.reservarStock(pedido, List.of(new ItemReserva(cafe, 0))));
        assertThrows(DatoInvalidoException.class,
                () -> fachada.reservarStock(pedido, List.of(new ItemReserva(cafe, 1), new ItemReserva(cafe, 2))));
        assertEquals(10, stock.get(cafe));
    }

    @Test
    void confirmarDejaElDescuentoFirmeYEsIdempotente() {
        UUID pedido = UUID.randomUUID();
        fachada.reservarStock(pedido, List.of(new ItemReserva(cafe, 4)));
        fachada.confirmarStock(pedido);
        fachada.confirmarStock(pedido);   // el webhook puede reintentarse

        assertEquals(6, stock.get(cafe));
        assertEquals(EstadoReserva.CONFIRMADA, reservas.buscarPorPedido(pedido).get(0).estado());
        assertThrows(ConflictoException.class, () -> fachada.liberarStock(pedido));
    }

    @Test
    void liberarDevuelveElStockUnaSolaVez() {
        UUID pedido = UUID.randomUUID();
        fachada.reservarStock(pedido, List.of(new ItemReserva(cafe, 4), new ItemReserva(miel, 2)));
        fachada.liberarStock(pedido);
        fachada.liberarStock(pedido);     // idempotente: no devuelve dos veces

        assertEquals(10, stock.get(cafe));
        assertEquals(5, stock.get(miel));
        assertThrows(ConflictoException.class, () -> fachada.confirmarStock(pedido));
    }

    @Test
    void confirmarOLiberarUnPedidoSinReservaFalla() {
        assertThrows(NoEncontradoException.class, () -> fachada.confirmarStock(UUID.randomUUID()));
        assertThrows(NoEncontradoException.class, () -> fachada.liberarStock(UUID.randomUUID()));
    }

    @Test
    void consultarProductoSoloDevuelveProductosActivos() {
        assertEquals("Café", fachada.consultarProducto(cafe).orElseThrow().nombre());
        catalogo.get(miel).ocultar("Motivo", Instant.parse("2026-11-20T11:00:00Z"));
        assertEquals(true, fachada.consultarProducto(miel).isEmpty());
        assertEquals(true, fachada.consultarProducto(UUID.randomUUID()).isEmpty());
    }
}
