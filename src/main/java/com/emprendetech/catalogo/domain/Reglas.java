package com.emprendetech.catalogo.domain;

import com.emprendetech.catalogo.domain.excepcion.DatoInvalidoException;

/** Validaciones reutilizables del dominio (sin dependencias externas). */
final class Reglas {

    private Reglas() {
    }

    static String textoObligatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("El campo '" + campo + "' es obligatorio");
        }
        return limitar(valor.trim(), campo, max);
    }

    static String textoOpcional(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return limitar(valor.trim(), campo, max);
    }

    static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw new DatoInvalidoException("El campo '" + campo + "' es obligatorio");
        }
        return valor;
    }

    private static String limitar(String valor, String campo, int max) {
        if (valor.length() > max) {
            throw new DatoInvalidoException("El campo '" + campo + "' no puede superar " + max + " caracteres");
        }
        return valor;
    }
}
