package org.jabref.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.jabref")
public class MainArchitectureTestsWithArchUnit {

    @ArchTest
    public static final ArchRule doNotUseApacheCommonsLang3 =
            noClasses().that().areNotAnnotatedWith(ApacheCommonsLang3Allowed.class)
            .should().accessClassesThat().resideInAPackage("org.apache.commons.lang3");

}
