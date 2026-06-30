package org.jabref.http;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "org.jabref.http", importOptions = ImportOption.DoNotIncludeTests.class)
public class JabSrvArchitectureTests {

    /// jabsrv is a JAX-RS / HK2 module — resources receive collaborators via
    /// `@Inject`, not via the afterburner DI used by the GUI. Smuggling in
    /// `Injector.instantiateModelOrService(...)` bypasses HK2 bindings and
    /// hides hidden coupling on the GUI bootstrap path. Register the
    /// collaborator as an HK2 constant in `Server` instead.
    @ArchTest
    public void doNotUseAfterburnerInjector(JavaClasses classes) {
        ArchRuleDefinition.noClasses()
                          .should().dependOnClassesThat().resideInAPackage("com.airhacks.afterburner.injection..")
                          .because("jabsrv must inject via HK2 (@Inject) — afterburner Injector belongs to the GUI module")
                          .check(classes);
    }
}
