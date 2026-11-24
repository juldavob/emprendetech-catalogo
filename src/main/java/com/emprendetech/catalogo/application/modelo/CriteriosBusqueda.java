package com.emprendetech.catalogo.application.modelo;

import com.emprendetech.catalogo.domain.Categoria;
import com.emprendetech.catalogo.domain.excepcion.DatoInvalidoException;
import com.emprendetech.shared.Localidad;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * RF3: filtros de búsqueda por texto, localidad, categoría y precio.
 * Es un record normalizado, por lo que sirve como llave de la caché de consultas.
 */
public record CriteriosBusqueda(String texto, Localidad localidad, Categoria categoria,
                                BigDecimal precioMin, BigDecimal precioMax,
                                OrdenProductos orden, int pagina, int tamano) {

    public static final int TAMANO_MAXIMO = 50;

    public CriteriosBusqueda {
        texto = (texto == null || texto.isBlank()) ? null : texto.trim();
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
        // Orden por defecto: relevancia si hay texto; si no, los más recientes.
        if (orden == null || (orden == OrdenProductos.RELEVANCIA && texto == null)) {
            orden = texto != null ? OrdenProductos.RELEVANCIA : OrdenProductos.RECIENTES;
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
