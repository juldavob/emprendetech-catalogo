package com.emprendetech.catalogo.infrastructure.persistence.orden;

import com.emprendetech.catalogo.application.modelo.OrdenProductos;
import org.springframework.stereotype.Component;

@Component
public class OrdenPrecioAscendente implements EstrategiaOrdenamiento {

    @Override
    public OrdenProductos orden() {
        return OrdenProductos.PRECIO_ASC;
    }

    @Override
    public String clausulaOrderBy() {
        return "p.precio ASC, p.id";
    }
}
