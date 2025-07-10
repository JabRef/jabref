package org.jabref.support;

import java.net.URI;
import java.nio.file.Paths;

import org.jabref.architecture.AllowedToUseApacheCommonsLang3;
import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.architecture.AllowedToUseStandardStreams;
import org.jabref.architecture.AllowedToUseSwing;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * This class checks JabRef's shipped classes for architecture quality.
 * Does not analyze test classes. Hint from <a href="https://stackoverflow.com/a/44681895/873282">StackOverflow</a>
 */
@AnalyzeClasses(packages = "org.jabref", importOptions = ImportOption.DoNotIncludeTests.class)
public class CommonArchitectureTest {

    private static final String PACKAGE_JAVAX_SWING = "javax.swing..";
    private static final String PACKAGE_JAVA_AWT = "java.awt..";
    private static final String PACKAGE_ORG_JABREF_GUI = "org.jabref.gui..";
    private static final String PACKAGE_ORG_JABREF_LOGIC = "org.jabref.logic..";
    private static final String PACKAGE_ORG_JABREF_MODEL = "org.jabref.model..";
    private static final String PACKAGE_ORG_JABREF_CLI = "org.jabref.cli..";

    @ArchTest
    public void doNotUseApacheCommonsLang3(JavaClasses classes) {
        ArchRuleDefinition.noClasses().that().areNotAnnotatedWith(AllowedToUseApacheCommonsLang3.class)
                          .should().accessClassesThat().resideInAPackage("org.apache.commons.lang3")
                          .check(classes);
    }

    @ArchTest
    public void doNotUseSwing(JavaClasses classes) {
        // This checks for all Swing packages, but not the UndoManager
        ArchRuleDefinition.noClasses().that().areNotAnnotatedWith(AllowedToUseSwing.class)
                          .should().accessClassesThat()
                          .resideInAnyPackage("javax.swing",
                           "javax.swing.border..",
                           "javax.swing.colorchooser..",
                           "javax.swing.event..",
                           "javax.swing.filechooser..",
                           "javax.swing.plaf..",
                           "javax.swing.table..",
                           "javax.swing.text..",
                           "javax.swing.tree..")
                          .check(classes);
    }

    @ArchTest
    public void doNotUseAssertJ(JavaClasses classes) {
        ArchRuleDefinition.noClasses().should().accessClassesThat().resideInAPackage("org.assertj..")
                          .check(classes);
    }

    @ArchTest
    public void doNotUseJavaAWT(JavaClasses classes) {
        ArchRuleDefinition.noClasses().that().areNotAnnotatedWith(AllowedToUseAwt.class)
                          .should().accessClassesThat().resideInAPackage(PACKAGE_JAVA_AWT)
                          .check(classes);
    }

    @ArchTest
    public void doNotUsePaths(JavaClasses classes) {
        ArchRuleDefinition.noClasses().should()
                          .accessClassesThat()
                          .belongToAnyOf(Paths.class)
                          .because("Path.of(...) should be used instead")
                          .check(classes);
    }

    @ArchTest
    public void useStreamsOfResources(JavaClasses classes) {
        // Reason: https://github.com/oracle/graal/issues/7682#issuecomment-1786704111
        ArchRuleDefinition.noClasses().that().haveNameNotMatching(".*Test")
                          .and().areNotAnnotatedWith(AllowedToUseClassGetResource.class)
                          .and().areNotAssignableFrom("org.jabref.logic.importer.fileformat.ImporterTestEngine")
                          .should()
                          .callMethod(Class.class, "getResource", String.class)
                          .because("getResourceAsStream(...) should be used instead")
                          .check(classes);
    }

    // TODO: no org.jabref.gui package may reside in org.jabref.logic

    @ArchTest
    public void doNotUseLogicInModel(JavaClasses classes) {
        ArchRuleDefinition.noClasses().that().resideInAPackage(PACKAGE_ORG_JABREF_MODEL)
                          .and().areNotAnnotatedWith(AllowedToUseLogic.class)
                          .should().dependOnClassesThat().resideInAPackage(PACKAGE_ORG_JABREF_LOGIC)
                          .check(classes);
    }

    @ArchTest
    public void restrictUsagesInModel(JavaClasses classes) {
        // Until we switch to Lucene, we need to access Globals.stateManager().getActiveDatabase() from the search classes,
        // because the PDFSearch needs to access the index of the corresponding database
        ArchRuleDefinition.noClasses().that().areNotAssignableFrom("org.jabref.model.search.rules.ContainBasedSearchRule")
                          .and().areNotAssignableFrom("org.jabref.model.search.rules.RegexBasedSearchRule")
                          .and().areNotAssignableFrom("org.jabref.model.search.rules.GrammarBasedSearchRule")
                          .and().resideInAPackage(PACKAGE_ORG_JABREF_MODEL)
                          .should().dependOnClassesThat().resideInAPackage(PACKAGE_JAVAX_SWING)
                          .check(classes);
    }

    @ArchTest
    public void restrictUsagesInLogic(JavaClasses classes) {
        ArchRuleDefinition.noClasses().that().resideInAPackage(PACKAGE_ORG_JABREF_LOGIC)
                          .and().areNotAnnotatedWith(AllowedToUseSwing.class)
                          .and().areNotAssignableFrom("org.jabref.logic.search.DatabaseSearcherWithBibFilesTest")
                          .should().dependOnClassesThat().resideInAPackage(PACKAGE_JAVAX_SWING)
                          .check(classes);
    }

    @ArchTest
    public void restrictStandardStreams(JavaClasses classes) {
        ArchRuleDefinition.noClasses().that().resideOutsideOfPackages(PACKAGE_ORG_JABREF_CLI)
                          .and().resideOutsideOfPackages("org.jabref.gui.openoffice..") // Uses LibreOffice SDK
                          .and().areNotAnnotatedWith(AllowedToUseStandardStreams.class)
                          .should(GeneralCodingRules.ACCESS_STANDARD_STREAMS)
                          .because("logging framework should be used instead or the class be marked explicitly as @AllowedToUseStandardStreams")
                          .check(classes);
    }

    /// Use constructor new URI(...) instead
    @ArchTest
    public void shouldNotCallUriCreateMethod(JavaClasses classes) {
       ArchRuleDefinition.noClasses()
                         .that()
                         .resideInAPackage("org.jabref..")
                         .should().callMethod(URI.class, "create", String.class)
                         .check(classes);
    }
}
