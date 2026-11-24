package com.emprendetech.catalogo.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendetech.shared_kernel.excepcion.ConflictoException;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservaStockTest {

    private static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-20T10:05:00Z");

    private static ReservaStock nueva() {
        return ReservaStock.crear(UUID.randomUUID(), UUID.randomUUID(), 3, T0);
    }

    @Test
    void laReservaNuevaQuedaEnEstadoReservada() {
        assertEquals(EstadoReserva.RESERVADA, nueva().estado());
    }

    @Test
    void laCantidadDebeSerPositiva() {
        assertThrows(DatoInvalidoException.class,
                () -> ReservaStock.crear(UUID.randomUUID(), UUID.randomUUID(), 0, T0));
    }

    @Test
    void confirmarEsIdempotente() {
        ReservaStock r = nueva();
        r.confirmar(T1);
        r.confirmar(T1);
        assertEquals(EstadoReserva.CONFIRMADA, r.estado());
    }

    @Test
    void liberarDevuelveTrueSoloLaPrimeraVez() {
        ReservaStock r = nueva();
        assertTrue(r.liberar(T1));
        assertFalse(r.liberar(T1));
        assertEquals(EstadoReserva.LIBERADA, r.estado());
    }

    @Test
    void noSePuedeLiberarUnaReservaConfirmadaNiConfirmarUnaLiberada() {
        ReservaStock confirmada = nueva();
        confirmada.confirmar(T1);
        assertThrows(ConflictoException.class, () -> confirmada.liberar(T1));

        ReservaStock liberada = nueva();
        liberada.liberar(T1);
        assertThrows(ConflictoException.class, () -> liberada.confirmar(T1));
    }
}
