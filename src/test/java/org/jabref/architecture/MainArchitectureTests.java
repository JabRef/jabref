package org.jabref.architecture;

import java.nio.file.Paths;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "org.jabref")
class MainArchitectureTests {

    public static final String CLASS_ORG_JABREF_GLOBALS = "org.jabref.Globals";
    private static final String PACKAGE_JAVAX_SWING = "javax.swing";
    private static final String PACKAGE_JAVA_AWT = "java.awt";
    private static final String PACKAGE_JAVA_FX = "javafx";
    private static final String PACKAGE_ORG_JABREF_GUI = "org.jabref.gui";
    private static final String PACKAGE_ORG_JABREF_LOGIC = "org.jabref.logic";
    private static final String PACKAGE_ORG_JABREF_MODEL = "org.jabref.model";
    private static final String EXCEPTION_PACKAGE_JAVA_AWT_GEOM = "java.awt.geom";
    private static final String EXCEPTION_PACKAGE_JAVA_FX_COLLECTIONS = "javafx.collections";
    private static final String EXCEPTION_PACKAGE_JAVA_FX_BEANS = "javafx.beans";
    private static final String EXCEPTION_CLASS_JAVA_FX_COLOR = "javafx.scene.paint.Color";
    private static final String EXCEPTION_CLASS_JAVA_FX_PAIR = "javafx.util.Pair";


    @ArchTest
    static void doNotUseApacheCommonsLang3(JavaClasses classes) {
        noClasses().that().areNotAnnotatedWith(ApacheCommonsLang3Allowed.class)
                   .should().accessClassesThat().resideInAPackage("org.apache.commons.lang3")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUseSwing(JavaClasses classes) {
        // This checks for all all Swing packages, but not the UndoManager
        noClasses().should().accessClassesThat().resideInAnyPackage("javax.swing",
                "javax.swing.border..",
                "javax.swing.colorchooser..",
                "javax.swing.event..",
                "javax.swing.filechooser..",
                "javax.swing.plaf..",
                "javax.swing.table..",
                "javax.swing.text..",
                "javax.swing.tree.."
        ).check(classes);
    }

    @ArchTest
    public static void doNotUseJGoodies(JavaClasses classes) {
        noClasses().should().accessClassesThat().resideInAPackage("com.jgoodies..")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUseGlazedLists(JavaClasses classes) {
        noClasses().should().accessClassesThat().resideInAPackage("ca.odell.glazedlists..")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUseGlyphsDirectly(JavaClasses classes) {
        noClasses().that().resideOutsideOfPackage("org.jabref.gui.icon")
                   .should().accessClassesThat().resideInAnyPackage("de.jensd.fx.glyphs", "de.jensd.fx.glyphs.materialdesignicons")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUseAssertJ(JavaClasses classes) {
        noClasses().should().accessClassesThat().resideInAPackage("org.assertj..")
                   .check(classes);
    }

    // "Currently disabled as there is no alternative for the rest of classes who need awt"
    @ArchIgnore
    @ArchTest
    public static void doNotUseJavaAWT(JavaClasses classes) {
        noClasses().should().accessClassesThat().resideInAPackage("java.awt..")
                   .check(classes);
    }

    @ArchTest
    public static void doNotUsePaths(JavaClasses classes) {
        noClasses().should()
                   .accessClassesThat()
                   .belongToAnyOf(Paths.class)
                   .check(classes);
    }

    @ArchTest
    static void respectLayeredArchitecture(JavaClasses classes) {
        layeredArchitecture()
                .layer("Gui").definedBy("org.jabref.gui")
                .layer("Logic").definedBy("org.jabref.logic")
                .layer("Model").definedBy("org.jabref.model")
                .layer("Cli").definedBy("org.jabref.cli")
                .layer("Migrations").definedBy("org.jabref.migrations")
                .layer("Preferences").definedBy("org.jabref.preferences")

                .whereLayer("Gui").mayNotBeAccessedByAnyLayer()
                .whereLayer("Logic").mayNotBeAccessedByAnyLayer()
                .whereLayer("Model").mayNotBeAccessedByAnyLayer()
                .whereLayer("Cli").mayNotBeAccessedByAnyLayer()
                .whereLayer("Migrations").mayNotBeAccessedByAnyLayer()
                .whereLayer("Preferences").mayNotBeAccessedByAnyLayer()

                .check(classes);
    }

    @ArchTest
    static void doNotUseLogicInModel(JavaClasses classes) {
        noClasses().that().resideInAPackage(PACKAGE_ORG_JABREF_MODEL)
                   .should().dependOnClassesThat().resideInAPackage("org.jabref.logic")
                   .orShould().beAnnotatedWith(AllowedToUseLogic.class)
                   .check(classes);
    }

    @ArchTest
    static void restrictUsagesInModel(JavaClasses classes) {
        noClasses().that().resideInAPackage(PACKAGE_ORG_JABREF_MODEL)
                   .should().dependOnClassesThat().resideInAPackage(PACKAGE_JAVA_AWT)
                   .orShould().dependOnClassesThat().resideInAPackage(PACKAGE_JAVAX_SWING)
                   .orShould().dependOnClassesThat().resideInAPackage(PACKAGE_JAVA_FX)
                   .orShould().dependOnClassesThat().haveFullyQualifiedName(CLASS_ORG_JABREF_GLOBALS)
                   .check(classes);
    }

    @ArchTest
    static void restrictUsagesInLogic(JavaClasses classes) {
        noClasses().that().resideInAPackage(PACKAGE_ORG_JABREF_LOGIC)
                   .should().dependOnClassesThat().resideInAPackage(PACKAGE_JAVA_AWT)
                   .orShould().dependOnClassesThat().resideInAPackage(PACKAGE_JAVAX_SWING)
                   .orShould().dependOnClassesThat().resideInAPackage(PACKAGE_JAVA_FX)
                   .orShould().dependOnClassesThat().haveFullyQualifiedName(CLASS_ORG_JABREF_GLOBALS)
                   .check(classes);
    }

}
