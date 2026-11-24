package com.emprendetech.catalogo.infraestructura.config;

import com.emprendetech.catalogo.aplicacion.fachada.CatalogoFacade;
import com.emprendetech.catalogo.aplicacion.puerto.BusquedaProductosPort;
import com.emprendetech.catalogo.aplicacion.puerto.CacheBusquedaPort;
import com.emprendetech.catalogo.aplicacion.puerto.InventarioRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.ProductoRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.ReservaRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.TiendaRepositorio;
import com.emprendetech.catalogo.aplicacion.puerto.Transaccion;
import com.emprendetech.catalogo.aplicacion.servicio.BusquedaProductos;
import com.emprendetech.catalogo.aplicacion.servicio.GestionInventario;
import com.emprendetech.catalogo.aplicacion.servicio.GestionProductos;
import com.emprendetech.catalogo.aplicacion.servicio.GestionTiendas;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composición del módulo: la capa de Aplicación es Java puro (sin anotaciones de Spring), así que
 * la infraestructura crea y conecta los casos de uso con sus puertos (inversión de dependencias).
 */
@Configuration
public class CatalogoConfig {

    /** Reloj inyectable: los casos de uso no llaman a Instant.now(), lo que facilita las pruebas. */
    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public GestionTiendas gestionTiendas(TiendaRepositorio tiendas, Transaccion tx, Clock reloj) {
        return new GestionTiendas(tiendas, tx, reloj);
    }

    @Bean
    public GestionProductos gestionProductos(ProductoRepositorio productos, TiendaRepositorio tiendas,
                                             CacheBusquedaPort cache, Transaccion tx, Clock reloj) {
        return new GestionProductos(productos, tiendas, cache, tx, reloj);
    }

    @Bean
    public BusquedaProductos busquedaProductos(BusquedaProductosPort busqueda, CacheBusquedaPort cache,
                                               Transaccion tx) {
        return new BusquedaProductos(busqueda, cache, tx);
    }

    @Bean
    public GestionInventario gestionInventario(InventarioRepositorio inventario, ReservaRepositorio reservas,
                                               ProductoRepositorio productos, CacheBusquedaPort cache,
                                               Transaccion tx, Clock reloj) {
        return new GestionInventario(inventario, reservas, productos, cache, tx, reloj);
    }

    @Bean
    public CatalogoFacade catalogoFacade(GestionProductos productos, GestionInventario inventario) {
        return new CatalogoFacade(productos, inventario);
    }
}
