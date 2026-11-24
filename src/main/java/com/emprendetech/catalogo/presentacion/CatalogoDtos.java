package com.emprendetech.catalogo.presentacion;

import com.emprendetech.catalogo.aplicacion.dto.AccionModeracion;
import com.emprendetech.shared_kernel.Localidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTOs de ENTRADA del módulo (con validación de formato). Las respuestas son los DTO de la capa de
 * Aplicación: la presentación no conoce el dominio (SAD Tabla 4).
 */
public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record CrearTiendaRequest(
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 500) String descripcion,
            @NotNull Localidad localidad) {
    }

    public record ProductoRequest(
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 2000) String descripcion,
            @NotBlank @Size(max = 30) String categoria,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precio,
            @NotNull @Min(0) Integer stock) {
    }

    public record ModeracionRequest(
            @NotNull AccionModeracion accion,
            @Size(max = 300) String motivo) {
    }
}
