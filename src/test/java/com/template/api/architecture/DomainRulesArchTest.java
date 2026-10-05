package com.template.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architectural fitness function validating domain layer purity and encapsulation.
 * <p>
 * Verifies domain independence from external frameworks (DOM-01), non-public constructors (DOM-02),
 * and absence of public setters on domain entities and aggregates (DOM-04).
 */
@AnalyzeClasses(packages = "com.template.api", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainRulesArchTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "com.fasterxml.jackson..",
                            "lombok.."
                    ).as("DOM-01: El dominio no debe depender de frameworks o librerías de terceros");

    @ArchTest
    static final ArchRule domain_models_should_not_expose_public_constructors =
            constructors().that().areDeclaredInClassesThat().resideInAPackage("..domain.model..")
                    .should().notBePublic()
                    .as("DOM-02: Las entidades y agregados de dominio no deben exponer constructores públicos");

    @ArchTest
    static final ArchRule domain_models_should_not_expose_public_setters =
            methods().that().areDeclaredInClassesThat().resideInAPackage("..domain.model..")
                    .and().arePublic()
                    .should().haveNameNotStartingWith("set")
                    .as("DOM-04: Los agregados y entidades de dominio no deben exponer setters públicos");
}
