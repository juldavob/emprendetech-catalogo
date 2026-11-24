package com.emprendetech.shared;

/** Kernel compartido: las 20 localidades de Bogotá, D.C. */
public enum Localidad {
    USAQUEN("Usaquén"),
    CHAPINERO("Chapinero"),
    SANTA_FE("Santa Fe"),
    SAN_CRISTOBAL("San Cristóbal"),
    USME("Usme"),
    TUNJUELITO("Tunjuelito"),
    BOSA("Bosa"),
    KENNEDY("Kennedy"),
    FONTIBON("Fontibón"),
    ENGATIVA("Engativá"),
    SUBA("Suba"),
    BARRIOS_UNIDOS("Barrios Unidos"),
    TEUSAQUILLO("Teusaquillo"),
    LOS_MARTIRES("Los Mártires"),
    ANTONIO_NARINO("Antonio Nariño"),
    PUENTE_ARANDA("Puente Aranda"),
    LA_CANDELARIA("La Candelaria"),
    RAFAEL_URIBE_URIBE("Rafael Uribe Uribe"),
    CIUDAD_BOLIVAR("Ciudad Bolívar"),
    SUMAPAZ("Sumapaz");

    private final String nombre;

    Localidad(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
