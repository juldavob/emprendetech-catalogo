package com.emprendetech.shared_kernel.excepcion;

/** El recurso solicitado no existe (o no es visible para el solicitante). */
public class NoEncontradoException extends RuntimeException {
    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
