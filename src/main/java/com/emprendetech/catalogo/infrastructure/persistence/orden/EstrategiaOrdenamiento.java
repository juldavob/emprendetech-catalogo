package com.emprendetech.catalogo.infrastructure.persistence.orden;

import com.emprendetech.catalogo.application.modelo.OrdenProductos;

/**
 * STRATEGY (comportamiento): cada criterio de ordenamiento de la búsqueda encapsula su propio
 * algoritmo de ordenación. Agregar un criterio nuevo = crear una clase nueva, sin tocar el
 * adaptador de búsqueda (principio Abierto/Cerrado) y sin cadenas de if/switch (RNF5).
 */
public interface EstrategiaOrdenamiento {

    /** Criterio de la aplicación que esta estrategia resuelve. */
    OrdenProductos orden();

    /** Fragmento ORDER BY. Es una constante del código: nunca se arma con datos del usuario. */
    String clausulaOrderBy();
}
