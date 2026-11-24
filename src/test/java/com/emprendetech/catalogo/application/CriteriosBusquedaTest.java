package com.emprendetech.catalogo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.emprendetech.catalogo.application.modelo.CriteriosBusqueda;
import com.emprendetech.catalogo.application.modelo.OrdenProductos;
import com.emprendetech.catalogo.domain.excepcion.DatoInvalidoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CriteriosBusquedaTest {

    @Test
    void sinTextoElOrdenPorDefectoSonLosMasRecientes() {
        CriteriosBusqueda c = new CriteriosBusqueda(null, null, null, null, null, null, 0, 20);
        assertEquals(OrdenProductos.RECIENTES, c.orden());
    }

    @Test
    void conTextoElOrdenPorDefectoEsLaRelevancia() {
        CriteriosBusqueda c = new CriteriosBusqueda("  miel ", null, null, null, null, null, 0, 20);
        assertEquals("miel", c.texto());
        assertEquals(OrdenProductos.RELEVANCIA, c.orden());
    }

    @Test
    void relevanciaSinTextoSeReemplazaPorRecientes() {
        CriteriosBusqueda c = new CriteriosBusqueda(" ", null, null, null, null,
                OrdenProductos.RELEVANCIA, 0, 20);
        assertNull(c.texto());
        assertEquals(OrdenProductos.RECIENTES, c.orden());
    }

    @Test
    void criteriosEquivalentesSonIgualesParaLaCache() {
        CriteriosBusqueda a = new CriteriosBusqueda("miel", null, null, new BigDecimal("10"), null, null, 0, 20);
        CriteriosBusqueda b = new CriteriosBusqueda("miel", null, null, new BigDecimal("10.00"), null, null, 0, 20);
        assertEquals(a, b);
    }

    @Test
    void rangoDePrecioInvertidoEsRechazado() {
        assertThrows(DatoInvalidoException.class, () -> new CriteriosBusqueda(null, null, null,
                new BigDecimal("100"), new BigDecimal("50"), null, 0, 20));
    }

    @Test
    void tamanoDePaginaFueraDeRangoEsRechazado() {
        assertThrows(DatoInvalidoException.class,
                () -> new CriteriosBusqueda(null, null, null, null, null, null, 0, 0));
        assertThrows(DatoInvalidoException.class,
                () -> new CriteriosBusqueda(null, null, null, null, null, null, 0, 51));
    }
}
