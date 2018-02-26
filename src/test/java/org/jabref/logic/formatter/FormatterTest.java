package org.jabref.logic.formatter;

import java.util.Collections;
import java.util.stream.Stream;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import org.jabref.logic.formatter.bibtexfields.RegexFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import org.jabref.logic.formatter.casechanger.CapitalizeFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.formatter.minifier.MinifyNameListFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.model.cleanup.Formatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FormatterTest {

    private static ProtectedTermsLoader protectedTermsLoader;

    @BeforeAll
    public static void setUp() {
        protectedTermsLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList()));


    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void getNameReturnsNotNull(Formatter formatter) {
        assertNotNull(formatter.getName());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void getNameReturnsNotEmpty(Formatter formatter) {
        assertNotEquals("", formatter.getName());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void getKeyReturnsNotNull(Formatter formatter) {
        assertNotNull(formatter.getKey());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void getKeyReturnsNotEmpty(Formatter formatter) {
        assertNotEquals("", formatter.getKey());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void formatOfNullThrowsException(Formatter formatter) {
        assertThrows(NullPointerException.class, () -> formatter.format(null));
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void formatOfEmptyStringReturnsEmpty(Formatter formatter) {
        assertEquals("", formatter.format(""));
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void formatNotReturnsNull(Formatter formatter) {
        assertNotNull(formatter.format("string"));
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void getDescriptionAlwaysNonEmpty(Formatter formatter) {
        assertFalse(formatter.getDescription().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    public void getExampleInputAlwaysNonEmpty(Formatter formatter) {
        assertFalse(formatter.getExampleInput().isEmpty());
    }

    public static Stream<Formatter> getFormatters() {
        // all classes implementing {@link net.sf.jabref.model.cleanup.Formatter}
        // sorted alphabetically
        // Alternative: Use reflection - https://github.com/ronmamo/reflections
        // @formatter:off
       return Stream.of(
                new CapitalizeFormatter(),
                new ClearFormatter(),
                new HtmlToLatexFormatter(),
                new HtmlToUnicodeFormatter(),
                new IdentityFormatter(),
                new LatexCleanupFormatter(),
                new LatexToUnicodeFormatter(),
                new LowerCaseFormatter(),
                new MinifyNameListFormatter(),
                new NormalizeDateFormatter(),
                new NormalizeMonthFormatter(),
                new NormalizeNamesFormatter(),
                new NormalizePagesFormatter(),
                new OrdinalsToSuperscriptFormatter(),
                new ProtectTermsFormatter(protectedTermsLoader),
                new RegexFormatter(),
                new RemoveBracesFormatter(),
                new SentenceCaseFormatter(),
                new TitleCaseFormatter(),
                new UnicodeToLatexFormatter(),
                new UnitsToLatexFormatter(),
                new UpperCaseFormatter());

        // @formatter:on
    }
}
