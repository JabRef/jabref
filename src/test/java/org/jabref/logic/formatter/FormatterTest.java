package org.jabref.logic.formatter;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.formatter.minifier.TruncateFormatter;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatterTest {

    private static ProtectedTermsLoader protectedTermsLoader;

    @BeforeAll
    static void setUp() {
        protectedTermsLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList()));
    }

    /**
     * When a new formatter is added by copy and pasting another formatter, it may happen that the <code>getKey()</code>
     * method is not adapted. This results in duplicate keys, which this test tests for.
     */
    @Test
    public void allFormatterKeysAreUnique() {
        // idea for uniqueness checking by https://stackoverflow.com/a/44032568/873282
        assertEquals(Collections.emptyList(),
                getFormatters().collect(Collectors.groupingBy(
                        formatter -> formatter.getKey(),
                        Collectors.counting()))
                               .entrySet().stream()
                               .filter(e -> e.getValue() > 1)
                               .map(Map.Entry::getKey)
                               .collect(Collectors.toList()));
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void getNameReturnsNotNull(Formatter formatter) {
        assertNotNull(formatter.getName());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void getNameReturnsNotEmpty(Formatter formatter) {
        assertNotEquals("", formatter.getName());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void getKeyReturnsNotNull(Formatter formatter) {
        assertNotNull(formatter.getKey());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void getKeyReturnsNotEmpty(Formatter formatter) {
        assertNotEquals("", formatter.getKey());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void formatOfNullThrowsException(Formatter formatter) {
        assertThrows(NullPointerException.class, () -> formatter.format(null));
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void formatOfEmptyStringReturnsEmpty(Formatter formatter) {
        assertEquals("", formatter.format(""));
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void formatNotReturnsNull(Formatter formatter) {
        assertNotNull(formatter.format("string"));
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void getDescriptionAlwaysNonEmpty(Formatter formatter) {
        assertFalse(formatter.getDescription().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getFormatters")
    void getExampleInputAlwaysNonEmpty(Formatter formatter) {
        assertFalse(formatter.getExampleInput().isEmpty());
    }

    public static Stream<Formatter> getFormatters() {
        // all classes implementing {@link net.sf.jabref.model.cleanup.Formatter}
        // Alternative: Use reflection - https://github.com/ronmamo/reflections
        // @formatter:off
        return Stream.concat(
                Formatters.getAll().stream(),
                // following formatters are not contained in the list of all formatters, because
                // - the IdentityFormatter is not offered to the user,
                // - the ProtectTermsFormatter needs more configuration,
                // - the TruncateFormatter needs setup,
                Stream.of(
                        new IdentityFormatter(),
                        new ProtectTermsFormatter(protectedTermsLoader),
                        new TruncateFormatter(0)));
        // @formatter:on
    }
}
