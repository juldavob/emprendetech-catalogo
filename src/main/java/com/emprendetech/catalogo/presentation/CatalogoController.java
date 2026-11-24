package com.emprendetech.catalogo.presentation;

import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.DatosProducto;
import com.emprendetech.catalogo.application.modelo.OrdenProductos;
import com.emprendetech.catalogo.application.servicio.BusquedaProductos;
import com.emprendetech.catalogo.application.servicio.GestionProductos;
import com.emprendetech.catalogo.application.servicio.GestionTiendas;
import com.emprendetech.catalogo.domain.Categoria;
import com.emprendetech.catalogo.domain.Producto;
import com.emprendetech.catalogo.domain.Tienda;
import com.emprendetech.catalogo.presentation.CatalogoDtos.CrearTiendaRequest;
import com.emprendetech.catalogo.presentation.CatalogoDtos.ModeracionRequest;
import com.emprendetech.catalogo.presentation.CatalogoDtos.PaginaResponse;
import com.emprendetech.catalogo.presentation.CatalogoDtos.ProductoRequest;
import com.emprendetech.catalogo.presentation.CatalogoDtos.ProductoResponse;
import com.emprendetech.catalogo.presentation.CatalogoDtos.TiendaResponse;
import com.emprendetech.shared.Localidad;
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
 * Capa de presentación: controlador REST del módulo Catálogo. Solo traduce HTTP a casos de uso.
 * La autorización por rol se declara en SeguridadConfig; la propiedad del recurso (que el producto
 * sea del emprendedor autenticado) la valida el caso de uso.
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
    public ResponseEntity<TiendaResponse> crearTienda(Authentication usuario,
                                                      @Valid @RequestBody CrearTiendaRequest request) {
        Tienda tienda = tiendas.crear(idDe(usuario), request.nombre(), request.descripcion(), request.localidad());
        return ResponseEntity.created(URI.create(BASE + "/tiendas/" + tienda.id()))
                .body(TiendaResponse.desde(tienda));
    }

    /** Tienda del emprendedor autenticado (la usa la interfaz para mostrar sus productos). */
    @GetMapping("/tiendas/mia")
    public TiendaResponse miTienda(Authentication usuario) {
        return TiendaResponse.desde(tiendas.obtenerDelPropietario(idDe(usuario)));
    }

    @GetMapping("/tiendas/{id}")
    public TiendaResponse obtenerTienda(@PathVariable UUID id) {
        return TiendaResponse.desde(tiendas.obtener(id));
    }

    // ------------------------------------------------------ productos (RF2, RF3)

    @GetMapping("/productos")
    public PaginaResponse<ProductoResponse> buscar(
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(required = false) Localidad localidad,
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) OrdenProductos orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        CriteriosBusqueda criterios = new CriteriosBusqueda(
                texto, localidad, categoria, precioMin, precioMax, orden, pagina, tamano);
        return PaginaResponse.desde(busqueda.buscar(criterios));
    }

    @GetMapping("/productos/{id}")
    public ProductoResponse obtenerProducto(@PathVariable UUID id) {
        return ProductoResponse.desde(productos.obtenerPublico(id));
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductoResponse> crearProducto(Authentication usuario,
                                                          @Valid @RequestBody ProductoRequest request) {
        Producto producto = productos.crear(idDe(usuario), datos(request));
        return ResponseEntity.created(URI.create(BASE + "/productos/" + producto.id()))
                .body(ProductoResponse.desde(producto));
    }

    @PutMapping("/productos/{id}")
    public ProductoResponse actualizarProducto(Authentication usuario, @PathVariable UUID id,
                                               @Valid @RequestBody ProductoRequest request) {
        return ProductoResponse.desde(productos.actualizar(idDe(usuario), id, datos(request)));
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> eliminarProducto(Authentication usuario, @PathVariable UUID id) {
        productos.eliminar(idDe(usuario), id);
        return ResponseEntity.noContent().build();
    }

    /** Moderación de contenido (solo rol ADMIN, configurado en SeguridadConfig). */
    @PatchMapping("/productos/{id}/moderacion")
    public ProductoResponse moderar(@PathVariable UUID id, @Valid @RequestBody ModeracionRequest request) {
        return ProductoResponse.desde(productos.moderar(id, request.accion(), request.motivo()));
    }

    // ---------------------------------------------------------------- utilidades

    private static UUID idDe(Authentication usuario) {
        return UUID.fromString(usuario.getName());
    }

    private static DatosProducto datos(ProductoRequest r) {
        return new DatosProducto(r.nombre(), r.descripcion(), r.categoria(), r.precio(), r.stock());
    }
}
