package com.emprendetech.catalogo.domain.excepcion;

/** Un dato viola una regla de negocio del dominio. */
public class DatoInvalidoException extends RuntimeException {
    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
