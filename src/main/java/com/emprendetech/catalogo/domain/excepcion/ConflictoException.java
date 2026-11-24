package com.emprendetech.catalogo.domain.excepcion;

/** La operación es válida pero choca con el estado actual (stock insuficiente, tienda duplicada, etc.). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
