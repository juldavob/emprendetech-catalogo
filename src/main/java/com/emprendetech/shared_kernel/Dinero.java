package com.emprendetech.shared_kernel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Kernel compartido: valor monetario en pesos colombianos (COP), siempre positivo y con 2 decimales. */
public record Dinero(BigDecimal monto) {

    private static final BigDecimal MAXIMO = new BigDecimal("9999999999.99"); // numeric(12,2)

    public Dinero {
        Objects.requireNonNull(monto, "El monto es obligatorio");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException("El monto excede el máximo permitido");
        }
    }
}
