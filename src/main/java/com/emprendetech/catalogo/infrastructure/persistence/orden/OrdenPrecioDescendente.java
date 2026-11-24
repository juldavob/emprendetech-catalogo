package com.emprendetech.catalogo.infrastructure.persistence.orden;

import com.emprendetech.catalogo.application.modelo.OrdenProductos;
import org.springframework.stereotype.Component;

@Component
public class OrdenPrecioDescendente implements EstrategiaOrdenamiento {

    @Override
    public OrdenProductos orden() {
        return OrdenProductos.PRECIO_DESC;
    }

    @Override
    public String clausulaOrderBy() {
        return "p.precio DESC, p.id";
    }
}
