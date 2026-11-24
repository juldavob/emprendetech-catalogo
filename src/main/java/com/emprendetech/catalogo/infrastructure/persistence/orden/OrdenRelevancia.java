package com.emprendetech.catalogo.infrastructure.persistence.orden;

import com.emprendetech.catalogo.application.modelo.OrdenProductos;
import org.springframework.stereotype.Component;

/** Ordena por relevancia de texto completo (ts_rank). Requiere el parámetro :texto. */
@Component
public class OrdenRelevancia implements EstrategiaOrdenamiento {

    @Override
    public OrdenProductos orden() {
        return OrdenProductos.RELEVANCIA;
    }

    @Override
    public String clausulaOrderBy() {
        return "ts_rank(p.busqueda, plainto_tsquery('spanish', :texto)) DESC, p.creado_en DESC, p.id";
    }
}
