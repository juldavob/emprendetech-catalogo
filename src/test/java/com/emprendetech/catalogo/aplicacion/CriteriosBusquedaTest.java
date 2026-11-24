package com.emprendetech.catalogo.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.emprendetech.catalogo.aplicacion.dto.CriteriosBusqueda;
import com.emprendetech.shared_kernel.excepcion.DatoInvalidoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CriteriosBusquedaTest {

    @Test
    void textoYCategoriaSeNormalizan() {
        CriteriosBusqueda c = new CriteriosBusqueda("  miel ", null, "alimentos", null, null, 0, 20);
        assertEquals("miel", c.texto());
        assertEquals("ALIMENTOS", c.categoria());
    }

    @Test
    void textoEnBlancoYCategoriaVaciaSeTratanComoAusentes() {
        CriteriosBusqueda c = new CriteriosBusqueda("   ", null, " ", null, null, 0, 20);
        assertNull(c.texto());
        assertNull(c.categoria());
    }

    @Test
    void criteriosEquivalentesSonIgualesParaLaCache() {
        CriteriosBusqueda a = new CriteriosBusqueda("miel", null, "HOGAR", new BigDecimal("10"), null, 0, 20);
        CriteriosBusqueda b = new CriteriosBusqueda(" miel", null, "hogar", new BigDecimal("10.00"), null, 0, 20);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.toString(), b.toString());
    }

    @Test
    void categoriaInvalidaEsRechazada() {
        assertThrows(DatoInvalidoException.class,
                () -> new CriteriosBusqueda(null, null, "COHETES", null, null, 0, 20));
    }

    @Test
    void rangoDePrecioInvertidoONegativoEsRechazado() {
        assertThrows(DatoInvalidoException.class, () -> new CriteriosBusqueda(null, null, null,
                new BigDecimal("100"), new BigDecimal("50"), 0, 20));
        assertThrows(DatoInvalidoException.class, () -> new CriteriosBusqueda(null, null, null,
                new BigDecimal("-1"), null, 0, 20));
    }

    @Test
    void paginacionFueraDeRangoEsRechazada() {
        assertThrows(DatoInvalidoException.class, () -> new CriteriosBusqueda(null, null, null, null, null, -1, 20));
        assertThrows(DatoInvalidoException.class, () -> new CriteriosBusqueda(null, null, null, null, null, 0, 0));
        assertThrows(DatoInvalidoException.class, () -> new CriteriosBusqueda(null, null, null, null, null, 0, 51));
    }
}
