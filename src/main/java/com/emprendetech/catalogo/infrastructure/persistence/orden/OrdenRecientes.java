package com.emprendetech.catalogo.infrastructure.persistence.orden;

import com.emprendetech.catalogo.application.modelo.OrdenProductos;
import org.springframework.stereotype.Component;

@Component
public class OrdenRecientes implements EstrategiaOrdenamiento {

    @Override
    public OrdenProductos orden() {
        return OrdenProductos.RECIENTES;
    }

    @Override
    public String clausulaOrderBy() {
        return "p.creado_en DESC, p.id";
    }
}
