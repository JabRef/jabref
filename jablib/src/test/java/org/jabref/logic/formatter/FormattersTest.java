package org.jabref.logic.formatter;

import java.util.Set;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormattersTest {

    /// A formatter missing from [Formatters#getAll()] cannot be picked in the cleanup and save action dialogs,
    /// and a save action stored under its key in a `.bib` file does not resolve back to it.
    @Test
    void allFormattersAreRegistered() {
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().acceptPackages("org.jabref.logic").scan()) {
            Set<String> expected = scanResult.getSubclasses(Formatter.class.getName())
                                             .filter(classInfo -> !classInfo.isAbstract() && !classInfo.isAnonymousInnerClass())
                                             .getNames()
                                             .stream()
                                             .collect(Collectors.toSet());
            expected.removeAll(Set.of(
                    // Need configuration
                    "org.jabref.logic.formatter.bibtexfields.RegexFormatter",
                    "org.jabref.logic.formatter.casechanger.CamelNFormatter",
                    "org.jabref.logic.formatter.casechanger.ProtectTermsFormatter",
                    "org.jabref.logic.formatter.minifier.TruncateFormatter",
                    "org.jabref.logic.layout.LayoutFormatterBasedFormatter",
                    // Used internally only, not offered to the user
                    "org.jabref.logic.formatter.IdentityFormatter",
                    "org.jabref.logic.formatter.bibtexfields.RemoveDigitsFormatter",
                    "org.jabref.logic.formatter.bibtexfields.RemoveHyphenatedNewlinesFormatter",
                    "org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter",
                    "org.jabref.logic.formatter.bibtexfields.RemoveRedundantSpacesFormatter",
                    "org.jabref.logic.formatter.bibtexfields.ReplaceTabsBySpaceFormater",
                    "org.jabref.logic.formatter.bibtexfields.TrimWhitespaceFormatter"));

            assertEquals(expected, Formatters.getAll().stream().map(formatter -> formatter.getClass().getName()).collect(Collectors.toSet()));
        }
    }
}
