package com.emprendetech.catalogo.aplicacion;

import com.emprendetech.catalogo.aplicacion.puerto.Transaccion;
import java.util.function.Supplier;

/** Transacción de prueba: ejecuta el trabajo directamente (sin base de datos ni Spring). */
final class TransaccionDirecta implements Transaccion {

    @Override
    public <T> T escribir(Supplier<T> trabajo) {
        return trabajo.get();
    }

    @Override
    public <T> T leer(Supplier<T> trabajo) {
        return trabajo.get();
    }
}
