package com.emprendetech.catalogo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendetech.catalogo.domain.excepcion.ConflictoException;
import com.emprendetech.catalogo.domain.excepcion.DatoInvalidoException;
import com.emprendetech.shared.Dinero;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductoTest {

    private static final Instant T0 = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-18T11:00:00Z");
    private static final UUID TIENDA = UUID.randomUUID();

    private static Producto nuevo() {
        return Producto.crear(TIENDA, "Café de origen", "Tostión media", Categoria.BEBIDAS,
                new Dinero(new BigDecimal("38000")), 10, T0);
    }

    @Test
    void productoNuevoQuedaActivoYConPrecioNormalizado() {
        Producto p = nuevo();
        assertTrue(p.estaActivo());
        assertEquals(new BigDecimal("38000.00"), p.precio().monto());
        assertEquals(10, p.stock());
    }

    @Test
    void stockNegativoEsRechazado() {
        assertThrows(DatoInvalidoException.class, () -> Producto.crear(TIENDA, "Café", null,
                Categoria.BEBIDAS, new Dinero(BigDecimal.TEN), -1, T0));
    }

    @Test
    void nombreEnBlancoEsRechazado() {
        assertThrows(DatoInvalidoException.class, () -> Producto.crear(TIENDA, "   ", null,
                Categoria.BEBIDAS, new Dinero(BigDecimal.TEN), 1, T0));
    }

    @Test
    void precioCeroOMenorEsRechazadoPorElKernel() {
        assertThrows(IllegalArgumentException.class, () -> new Dinero(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Dinero(new BigDecimal("-5")));
    }

    @Test
    void ocultarYRestaurarCambiaElEstadoYLimpiaElMotivo() {
        Producto p = nuevo();
        p.ocultar("Sin registro sanitario", T1);
        assertFalse(p.estaActivo());
        assertEquals("Sin registro sanitario", p.motivoModeracion());

        p.restaurar(T1);
        assertTrue(p.estaActivo());
        assertNull(p.motivoModeracion());
    }

    @Test
    void ocultarDosVecesGeneraConflicto() {
        Producto p = nuevo();
        p.ocultar("Motivo", T1);
        assertThrows(ConflictoException.class, () -> p.ocultar("Otro", T1));
    }

    @Test
    void ocultarExigeMotivo() {
        assertThrows(DatoInvalidoException.class, () -> nuevo().ocultar(" ", T1));
    }

    @Test
    void restaurarUnProductoActivoGeneraConflicto() {
        assertThrows(ConflictoException.class, () -> nuevo().restaurar(T1));
    }
}
