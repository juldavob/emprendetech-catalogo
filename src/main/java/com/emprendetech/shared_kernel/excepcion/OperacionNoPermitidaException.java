package com.emprendetech.shared_kernel.excepcion;

/** El usuario autenticado no tiene derecho a realizar la operación (p. ej., no es dueño de la tienda). */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
