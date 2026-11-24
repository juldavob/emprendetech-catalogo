package com.emprendetech.catalogo.aplicacion.dto;

import java.io.Serializable;
import java.util.List;

public record PaginaDto<T>(List<T> contenido, int pagina, int tamano, long total, int totalPaginas)
        implements Serializable {

    public PaginaDto {
        contenido = List.copyOf(contenido);
    }

    public static <T> PaginaDto<T> de(List<T> contenido, int pagina, int tamano, long total) {
        int paginas = tamano <= 0 ? 0 : (int) Math.ceil((double) total / tamano);
        return new PaginaDto<>(contenido, pagina, tamano, total, paginas);
    }
}
