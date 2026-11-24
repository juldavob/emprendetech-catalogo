package com.emprendetech;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verificación automática de la regla de dependencia (SAD §1.1) y de las fronteras entre módulos
 * (SAD §2.3, regla 1).
 */
class ArquitecturaTest {

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.emprendetech");

    @Test
    void losModulosRespetanSusFronterasYNoTienenCiclos() {
        ApplicationModules.of(EmprendeTechApplication.class).verify();
    }

    @Test
    void elDominioNoDependeDeFrameworksNiDeCapasExternas() {
        noClasses().that().resideInAPackage("..catalogo.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "javax..", "com.nimbusds..",
                        "..catalogo.application..", "..catalogo.infrastructure..", "..catalogo.presentation..")
                .check(CLASES);
    }

    @Test
    void laAplicacionNoDependeDeInfraestructuraNiDePresentacionNiDeJpa() {
        noClasses().that().resideInAPackage("..catalogo.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..catalogo.infrastructure..", "..catalogo.presentation..", "jakarta.persistence..")
                .check(CLASES);
    }

    @Test
    void laInfraestructuraNoDependeDeLaPresentacion() {
        noClasses().that().resideInAPackage("..catalogo.infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..catalogo.presentation..")
                .check(CLASES);
    }

    @Test
    void elKernelCompartidoNoDependeDeFrameworksNiDeModulos() {
        noClasses().that().resideInAPackage("com.emprendetech.shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.emprendetech.catalogo..", "com.emprendetech.api..")
                .check(CLASES);
    }
}
