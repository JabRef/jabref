package org.jabref.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.jabref.architecture.MainArchitectureTest.CLASS_ORG_JABREF_GLOBALS;

/**
 * This class checks JabRef's test classes for architecture quality
 */
@AnalyzeClasses(packages = "org.jabref", importOptions = ImportOption.OnlyIncludeTests.class)
public class TestArchitectureTest {

    private static final String CLASS_ORG_JABREF_PREFERENCES = "org.jabref.preferences.JabRefPreferences";

    @ArchTest
    public void testsAreIndependent(JavaClasses classes) {
        noClasses().that().doNotHaveSimpleName("EntryEditorTest")
                   .and().doNotHaveSimpleName("LinkedFileViewModelTest")
                   .and().doNotHaveSimpleName("JabRefPreferencesTest")
                   .and().doNotHaveSimpleName("PreferencesMigrationsTest")
                   .and().doNotHaveSimpleName("SaveDatabaseActionTest")
                   .and().doNotHaveSimpleName("UpdateTimestampListenerTest")
                   .and().doNotHaveSimpleName("DatabaseSearcherWithBibFilesTest")
                   .and().doNotHaveFullyQualifiedName("org.jabref.benchmarks.Benchmarks")
                   .and().doNotHaveFullyQualifiedName("org.jabref.testutils.interactive.styletester.StyleTesterMain")
                   .should().dependOnClassesThat().haveFullyQualifiedName(CLASS_ORG_JABREF_GLOBALS)
                   .orShould().dependOnClassesThat().haveFullyQualifiedName(CLASS_ORG_JABREF_PREFERENCES)
                   .check(classes);
    }

    @ArchTest
    public void testNaming(JavaClasses classes) {
        classes().that().areTopLevelClasses()
                 .and().doNotHaveFullyQualifiedName("org.jabref.benchmarks.Benchmarks")
                 .and().doNotHaveFullyQualifiedName("org.jabref.http.server.TestBibFile")
                 .and().doNotHaveFullyQualifiedName("org.jabref.gui.autocompleter.AutoCompleterUtil")
                 .and().doNotHaveFullyQualifiedName("org.jabref.gui.search.TextFlowEqualityHelper")
                 .and().doNotHaveFullyQualifiedName("org.jabref.logic.bibtex.BibEntryAssert")
                 .and().doNotHaveFullyQualifiedName("org.jabref.logic.importer.fileformat.ImporterTestEngine")
                 .and().doNotHaveFullyQualifiedName("org.jabref.logic.l10n.JavaLocalizationEntryParser")
                 .and().doNotHaveFullyQualifiedName("org.jabref.logic.l10n.LocalizationEntry")
                 .and().doNotHaveFullyQualifiedName("org.jabref.logic.l10n.LocalizationParser")
                 .and().doNotHaveFullyQualifiedName("org.jabref.logic.openoffice.style.OOBibStyleTestHelper")
                 .and().doNotHaveFullyQualifiedName("org.jabref.logic.shared.TestManager")
                 .and().doNotHaveFullyQualifiedName("org.jabref.model.search.rules.MockSearchMatcher")
                 .and().doNotHaveFullyQualifiedName("org.jabref.model.TreeNodeTestData")
                 .and().doNotHaveFullyQualifiedName("org.jabref.performance.BibtexEntryGenerator")
                 .and().doNotHaveFullyQualifiedName("org.jabref.support.DisabledOnCIServer")
                 .and().doNotHaveFullyQualifiedName("org.jabref.support.CIServerCondition")
                 .and().doNotHaveFullyQualifiedName("org.jabref.testutils.interactive.styletester.StyleTesterMain")
                 .and().doNotHaveFullyQualifiedName("org.jabref.testutils.interactive.styletester.StyleTesterView")
                 .should().haveSimpleNameEndingWith("Test")
                 .check(classes);
    }
}
