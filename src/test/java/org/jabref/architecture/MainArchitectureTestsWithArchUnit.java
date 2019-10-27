package org.jabref.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.jabref")
class MainArchitectureTestsWithArchUnit {

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
        noClasses().should().accessClassesThat().resideInAPackage("com.jgoodies..").check(classes);
    }

    @ArchTest
    public static void doNotUseGlazedLists(JavaClasses classes) {
        noClasses().should().accessClassesThat().resideInAPackage("ca.odell.glazedlists..").check(classes);
    }

    @ArchTest
    public static void doNotUseGlyphsDirectly(JavaClasses classes) {
        noClasses().that().resideOutsideOfPackage("org.jabref.gui.icon").should().accessClassesThat().resideInAnyPackage("de.jensd.fx.glyphs", "de.jensd.fx.glyphs.materialdesignicons").check(classes);
    }

    //"Currently disabled as there is no alternative for the rest of classes who need awt"
    @ArchIgnore
    @ArchTest
    public static void doNotUseJavaAWT(JavaClasses classes) {
        noClasses().should().accessClassesThat().resideInAPackage("java.awt..").check(classes);
    }
}
