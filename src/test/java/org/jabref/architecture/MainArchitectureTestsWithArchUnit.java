package org.jabref.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.jabref")
public class MainArchitectureTestsWithArchUnit {

    @ArchTest
    public static void doNotUseApacheCommonsLang3(JavaClasses classes) {
        noClasses().that().areNotAnnotatedWith(ApacheCommonsLang3Allowed.class)
                   .should().accessClassesThat().resideInAPackage("org.apache.commons.lang3")
                   .check(classes);
    }
}
