package com.emprendetech.catalogo.presentation;

import com.emprendetech.catalogo.application.modelo.AccionModeracion;
import com.emprendetech.catalogo.application.modelo.Pagina;
import com.emprendetech.catalogo.domain.Categoria;
import com.emprendetech.catalogo.domain.EstadoProducto;
import com.emprendetech.catalogo.domain.Producto;
import com.emprendetech.catalogo.domain.Tienda;
import com.emprendetech.shared.Localidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Contratos HTTP del módulo Catálogo (entrada con validación y salida). */
public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record CrearTiendaRequest(
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 500) String descripcion,
            @NotNull Localidad localidad) {
    }

    public record TiendaResponse(UUID id, UUID propietarioId, String nombre,
                                 String descripcion, Localidad localidad) {
        public static TiendaResponse desde(Tienda t) {
            return new TiendaResponse(t.id(), t.propietarioId(), t.nombre(), t.descripcion(), t.localidad());
        }
    }

    public record ProductoRequest(
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 2000) String descripcion,
            @NotNull Categoria categoria,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precio,
            @NotNull @Min(0) Integer stock) {
    }

    public record ProductoResponse(UUID id, UUID tiendaId, String nombre, String descripcion,
                                   Categoria categoria, BigDecimal precio, int stock,
                                   EstadoProducto estado, String motivoModeracion) {
        public static ProductoResponse desde(Producto p) {
            return new ProductoResponse(p.id(), p.tiendaId(), p.nombre(), p.descripcion(), p.categoria(),
                    p.precio().monto(), p.stock(), p.estado(), p.motivoModeracion());
        }
    }

    public record ModeracionRequest(
            @NotNull AccionModeracion accion,
            @Size(max = 300) String motivo) {
    }

    public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long total, int totalPaginas) {
        public static PaginaResponse<ProductoResponse> desde(Pagina<Producto> p) {
            return new PaginaResponse<>(
                    p.contenido().stream().map(ProductoResponse::desde).toList(),
                    p.pagina(), p.tamano(), p.total(), p.totalPaginas());
        }
    }
}
