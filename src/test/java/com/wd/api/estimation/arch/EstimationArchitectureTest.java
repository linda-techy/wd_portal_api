package com.wd.api.estimation.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

class EstimationArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.wd.api.estimation");
    }

    @Test
    void calcPackage_doesNotDependOnJpa() {
        noClasses().that().resideInAPackage("..service.calc..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "org.springframework.data.jpa..")
                .check(classes);
    }

    @Test
    void calcPackage_doesNotDependOnSpringFramework() {
        noClasses().that().resideInAPackage("..service.calc..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.beans..",
                        "org.springframework.context..",
                        "org.springframework.stereotype..")
                .check(classes);
    }

    @Test
    void calcPackage_doesNotDependOnEstimationDomainEntities() {
        // Calc takes view records as input, not entities. Mappers convert at the boundary.
        noClasses().that().resideInAPackage("..service.calc..")
                .and().resideOutsideOfPackage("..service.calc.mapping..")
                .should().dependOnClassesThat().resideInAPackage("com.wd.api.estimation.domain")
                .check(classes);
    }

    @Test
    void calcPackage_doesNotUseDoubleForMoney() {
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..service.calc..")
                .should().haveRawType(double.class)
                .check(classes);
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..service.calc..")
                .should().haveRawType(Double.class)
                .check(classes);
    }

    @Test
    void calcPackage_doesNotUseFloatForMoney() {
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..service.calc..")
                .should().haveRawType(float.class)
                .check(classes);
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..service.calc..")
                .should().haveRawType(Float.class)
                .check(classes);
    }

    @Test
    void calcPackage_doesNotHaveAutowiredFields() {
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..service.calc..")
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .check(classes);
    }
}
