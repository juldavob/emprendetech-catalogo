package com.emprendetech.catalogo.dominio;

import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import java.util.Arrays;
import java.util.Locale;

public enum Categoria {
    ARTESANIAS, ALIMENTOS, BEBIDAS, MODA, HOGAR, BELLEZA, TECNOLOGIA, MASCOTAS, OTROS;

    /** Convierte el texto recibido (cualquier capitalización) en una categoría válida. */
    public static Categoria desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("La categoría es obligatoria");
        }
        try {
            return Categoria.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException(
                    "Categoría inválida: '" + valor + "'. Valores permitidos: " + Arrays.toString(values()));
        }
    }
}
