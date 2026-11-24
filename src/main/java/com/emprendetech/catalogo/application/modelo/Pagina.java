package com.emprendetech.catalogo.application.modelo;

import java.util.List;

/** Resultado paginado e inmutable (apto para guardarse en caché). */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long total) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        return tamano <= 0 ? 0 : (int) Math.ceil((double) total / tamano);
    }
}
