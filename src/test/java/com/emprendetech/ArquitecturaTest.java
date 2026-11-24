package com.emprendetech;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verificación automática de las reglas de frontera del SAD: Tabla 4 (dependencias por capa),
 * §8.1 (regla de dependencia hacia el Dominio) y §5.1 (acceso entre módulos solo por fachadas).
 */
class ArquitecturaTest {

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.emprendetech");

    @Test
    void losModulosRespetanSusFronterasYNoTienenCiclos() {
        ApplicationModules.of(EmprendeTechApplication.class).verify();
    }

    /** Dominio: "núcleo puro en Java sin dependencias de frameworks ni de otras capas". */
    @Test
    void elDominioNoDependeDeFrameworksNiDeOtrasCapas() {
        noClasses().that().resideInAPackage("..catalogo.dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "javax..", "com.nimbusds..",
                        "..catalogo.aplicacion..", "..catalogo.infraestructura..", "..catalogo.presentacion..")
                .check(CLASES);
    }

    /** Aplicación: "depende solo de Dominio y expone puertos" (Java puro, sin Spring ni JPA). */
    @Test
    void laAplicacionSoloDependeDelDominioYDelKernel() {
        noClasses().that().resideInAPackage("..catalogo.aplicacion..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..catalogo.infraestructura..", "..catalogo.presentacion..",
                        "jakarta..", "javax..", "com.nimbusds..",
                        "org.springframework.beans..", "org.springframework.stereotype..",
                        "org.springframework.transaction..", "org.springframework.context..",
                        "org.springframework.data..", "org.springframework.web..",
                        "org.springframework.http..", "org.springframework.security..")
                .check(CLASES);
    }

    /** Presentación: "depende únicamente de Aplicación y DTOs de entrada/salida". */
    @Test
    void laPresentacionNoConoceElDominioNiLaInfraestructura() {
        noClasses().that().resideInAPackage("..catalogo.presentacion..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..catalogo.dominio..", "..catalogo.infraestructura..")
                .check(CLASES);
    }

    /** Infraestructura: implementa los puertos de Aplicación por inversión de dependencias. */
    @Test
    void laInfraestructuraNoDependeDeLaPresentacion() {
        noClasses().that().resideInAPackage("..catalogo.infraestructura..")
                .should().dependOnClassesThat().resideInAPackage("..catalogo.presentacion..")
                .check(CLASES);
    }

    @Test
    void elKernelCompartidoNoDependeDeFrameworksNiDeModulos() {
        noClasses().that().resideInAPackage("com.emprendetech.shared_kernel..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.emprendetech.catalogo..")
                .check(CLASES);
    }
}
