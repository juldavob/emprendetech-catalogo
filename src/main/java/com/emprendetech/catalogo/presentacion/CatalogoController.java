package com.emprendetech.catalogo.presentacion;

import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;
import com.emprendetech.catalogo.aplicacion.dto.DatosProducto;
import com.emprendetech.catalogo.aplicacion.dto.PaginaDto;
import com.emprendetech.catalogo.aplicacion.dto.ProductoDto;
import com.emprendetech.catalogo.aplicacion.dto.TiendaDto;
import com.emprendetech.catalogo.aplicacion.servicio.BusquedaProductos;
import com.emprendetech.catalogo.aplicacion.servicio.GestionProductos;
import com.emprendetech.catalogo.aplicacion.servicio.GestionTiendas;
import com.emprendetech.catalogo.presentacion.CatalogoDtos.CrearTiendaRequest;
import com.emprendetech.catalogo.presentacion.CatalogoDtos.ModeracionRequest;
import com.emprendetech.catalogo.presentacion.CatalogoDtos.ProductoRequest;
import com.emprendetech.shared_kernel.Localidad;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Capa de presentación: CatalogoController (SAD Tabla 4). Solo traduce HTTP a casos de uso. La
 * autorización por rol está en SeguridadConfig; que el producto sea del emprendedor autenticado lo
 * valida el caso de uso.
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {

    private static final String BASE = "/api/v1/catalogo";

    private final GestionTiendas tiendas;
    private final GestionProductos productos;
    private final BusquedaProductos busqueda;

    public CatalogoController(GestionTiendas tiendas, GestionProductos productos, BusquedaProductos busqueda) {
        this.tiendas = tiendas;
        this.productos = productos;
        this.busqueda = busqueda;
    }

    // ---------------------------------------------------------------- tiendas

    @PostMapping("/tiendas")
    public ResponseEntity<TiendaDto> crearTienda(Authentication usuario,
                                                 @Valid @RequestBody CrearTiendaRequest request) {
        TiendaDto tienda = tiendas.crear(idDe(usuario), request.nombre(), request.descripcion(), request.localidad());
        return ResponseEntity.created(URI.create(BASE + "/tiendas/" + tienda.id())).body(tienda);
    }

    /** Tienda del emprendedor autenticado (la usa la interfaz para mostrar sus productos). */
    @GetMapping("/tiendas/mia")
    public TiendaDto miTienda(Authentication usuario) {
        return tiendas.obtenerDelPropietario(idDe(usuario));
    }

    @GetMapping("/tiendas/{id}")
    public TiendaDto obtenerTienda(@PathVariable UUID id) {
        return tiendas.obtener(id);
    }

    // ------------------------------------------------------ productos (RF2, RF3)

    @GetMapping("/productos")
    public PaginaDto<ProductoDto> buscar(
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(required = false) Localidad localidad,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return busqueda.buscar(new CriteriosBusqueda(texto, localidad, categoria, precioMin, precioMax, pagina, tamano));
    }

    @GetMapping("/productos/{id}")
    public ProductoDto obtenerProducto(@PathVariable UUID id) {
        return productos.obtenerPublico(id);
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductoDto> crearProducto(Authentication usuario,
                                                     @Valid @RequestBody ProductoRequest request) {
        ProductoDto producto = productos.crear(idDe(usuario), datos(request));
        return ResponseEntity.created(URI.create(BASE + "/productos/" + producto.id())).body(producto);
    }

    @PutMapping("/productos/{id}")
    public ProductoDto actualizarProducto(Authentication usuario, @PathVariable UUID id,
                                          @Valid @RequestBody ProductoRequest request) {
        return productos.actualizar(idDe(usuario), id, datos(request));
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> eliminarProducto(Authentication usuario, @PathVariable UUID id) {
        productos.eliminar(idDe(usuario), id);
        return ResponseEntity.noContent().build();
    }

    /** Moderación de contenido (solo rol ADMIN, configurado en SeguridadConfig). */
    @PatchMapping("/productos/{id}/moderacion")
    public ProductoDto moderar(@PathVariable UUID id, @Valid @RequestBody ModeracionRequest request) {
        return productos.moderar(id, request.accion(), request.motivo());
    }

    // ---------------------------------------------------------------- utilidades

    private static UUID idDe(Authentication usuario) {
        return UUID.fromString(usuario.getName());
    }

    private static DatosProducto datos(ProductoRequest r) {
        return new DatosProducto(r.nombre(), r.descripcion(), r.categoria(), r.precio(), r.stock());
    }
}
