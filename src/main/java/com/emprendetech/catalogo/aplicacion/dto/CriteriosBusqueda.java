package com.emprendetech.catalogo.aplicacion.dto;

import com.emprendetech.catalogo.dominio.Categoria;
import com.emprendetech.shared_kernel.Localidad;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * RF3: filtros de búsqueda por texto, localidad, categoría y precio. Está normalizado, de modo que
 * criterios equivalentes son iguales y generan la misma llave de caché. El orden no es configurable:
 * relevancia si hay texto; de lo contrario, los más recientes.
 */
public record CriteriosBusqueda(String texto, Localidad localidad, String categoria,
                                BigDecimal precioMin, BigDecimal precioMax, int pagina, int tamano) {

    public static final int TAMANO_MAXIMO = 50;

    public CriteriosBusqueda {
        texto = (texto == null || texto.isBlank()) ? null : texto.trim();
        categoria = (categoria == null || categoria.isBlank()) ? null : Categoria.desde(categoria).name();
        precioMin = normalizar(precioMin, "precioMin");
        precioMax = normalizar(precioMax, "precioMax");
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw new DatoInvalidoException("precioMin no puede ser mayor que precioMax");
        }
        if (pagina < 0) {
            throw new DatoInvalidoException("La página debe ser mayor o igual a 0");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new DatoInvalidoException("El tamaño debe estar entre 1 y " + TAMANO_MAXIMO);
        }
    }

    private static BigDecimal normalizar(BigDecimal valor, String campo) {
        if (valor == null) {
            return null;
        }
        if (valor.signum() < 0) {
            throw new DatoInvalidoException(campo + " no puede ser negativo");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
