package com.emprendetech.catalogo.aplicacion.puerto;

import java.util.function.Supplier;

/**
 * Puerto de salida: la capa de Aplicación orquesta las transacciones (SAD Tabla 4) sin conocer
 * Spring; la infraestructura decide cómo se implementa (TransactionTemplate).
 */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción de lectura/escritura (rollback ante RuntimeException). */
    <T> T escribir(Supplier<T> trabajo);

    /** Ejecuta el trabajo en una transacción de solo lectura. */
    <T> T leer(Supplier<T> trabajo);

    default void ejecutar(Runnable trabajo) {
        escribir(() -> {
            trabajo.run();
            return null;
        });
    }
}
