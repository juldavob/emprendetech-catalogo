package com.emprendetech.catalogo.infraestructura.config;

import com.emprendetech.catalogo.aplicacion.puerto.Transaccion;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Implementa el puerto {@link Transaccion} con TransactionTemplate (rollback ante RuntimeException). */
@Component
public class TransaccionSpring implements Transaccion {

    private final TransactionTemplate escritura;
    private final TransactionTemplate lectura;

    public TransaccionSpring(PlatformTransactionManager administrador) {
        this.escritura = new TransactionTemplate(administrador);
        this.lectura = new TransactionTemplate(administrador);
        this.lectura.setReadOnly(true);
    }

    @Override
    public <T> T escribir(Supplier<T> trabajo) {
        return escritura.execute(estado -> trabajo.get());
    }

    @Override
    public <T> T leer(Supplier<T> trabajo) {
        return lectura.execute(estado -> trabajo.get());
    }
}
