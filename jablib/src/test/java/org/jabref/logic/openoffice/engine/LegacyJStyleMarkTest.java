package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
        // [utest->req~openoffice.legacy-jstyle-migration~1]
class LegacyJStyleMarkTest {

    @ParameterizedTest
    @MethodSource("citationTypes")
    void decodesEveryLegacyCitationTypeIgnoringRepetitionIndex(int code, CitationType citationType) throws JabRefMarkCodecException {
        for (String repetitionIndex : List.of("", "0", "1", "42", "0007", "999999999999999999999999999999")) {
            String name = "JR_cite" + repetitionIndex + "_" + code + "_Smith2020,Doe2021";

            assertTrue(LegacyJStyleMark.isLegacyMarkName(name), name);
            LegacyJStyleMark mark = LegacyJStyleMark.decode(name);
            assertEquals(List.of("Smith2020", "Doe2021"), mark.citationKeys(), name);
            assertEquals(citationType, mark.citationType(), name);
        }
    }

    private static Stream<Arguments> citationTypes() {
        return Stream.of(
                Arguments.of(1, CitationType.AUTHORYEAR_PAR),
                Arguments.of(2, CitationType.AUTHORYEAR_INTEXT),
                Arguments.of(3, CitationType.INVISIBLE_CIT),
                Arguments.of(4, CitationType.AUTHORYEAR_NOPAR),
                Arguments.of(5, CitationType.AUTHOR_ONLY),
                Arguments.of(6, CitationType.YEAR_ONLY));
    }

    @ParameterizedTest
    @MethodSource("validKeys")
    void preservesKeyTextOrderAndRepetitions(String name, List<String> expectedKeys) throws JabRefMarkCodecException {
        assertTrue(LegacyJStyleMark.isLegacyMarkName(name));
        assertEquals(expectedKeys, LegacyJStyleMark.decode(name).citationKeys());
    }

    private static Stream<Arguments> validKeys() {
        return Stream.of(
                Arguments.of("JR_cite_1_key", List.of("key")),
                Arguments.of("JR_cite17_1_z,a,z", List.of("z", "a", "z")),
                Arguments.of("JR_cite_1_first_key,__second__,first_key", List.of("first_key", "__second__", "first_key")),
                Arguments.of("JR_cite_1_β2020:一/2–3—4.foo+bar?{x},日本語", List.of("β2020:一/2–3—4.foo+bar?{x}", "日本語")),
                Arguments.of("JR_cite_1_JR_cite42_6_key,1_NORMAL", List.of("JR_cite42_6_key", "1_NORMAL")));
    }

    @ParameterizedTest
    @MethodSource("invalidKeys")
    void recognizesStructuralNamesButRejectsInvalidKeys(String keys) {
        for (int code = 1; code <= 6; code++) {
            String name = "JR_cite12_" + code + "_" + keys;

            assertTrue(LegacyJStyleMark.isLegacyMarkName(name), name);
            assertThrows(JabRefMarkCodecException.class, () -> LegacyJStyleMark.decode(name), name);
        }
    }

    private static Stream<Arguments> invalidKeys() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of(","),
                Arguments.of(",key"),
                Arguments.of("key,"),
                Arguments.of("key,,other"),
                Arguments.of("key,other,"),
                Arguments.of(" "),
                Arguments.of(" key"),
                Arguments.of("key "),
                Arguments.of("key other"),
                Arguments.of("key, other"),
                Arguments.of("key\tother"),
                Arguments.of("key\u000bother"),
                Arguments.of("key\fother"),
                Arguments.of("key\u0085other"),
                Arguments.of("key\u00a0other"),
                Arguments.of("key\u1680other"),
                Arguments.of("key\u2003other"),
                Arguments.of("key\u202fother"),
                Arguments.of("key\u205fother"),
                Arguments.of("key\u3000other"));
    }

    @ParameterizedTest
    @MethodSource("malformedNames")
    void rejectsMalformedNamesWithoutSubstringMatchingOrUncheckedErrors(String name) {
        assertFalse(LegacyJStyleMark.isLegacyMarkName(name));
        assertThrows(JabRefMarkCodecException.class, () -> LegacyJStyleMark.decode(name));
    }

    private static Stream<Arguments> malformedNames() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("unrelated"),
                Arguments.of("JR_bib"),
                Arguments.of("JABREF_key NORMAL uid"),
                Arguments.of("ZOTERO_ITEM CSL_CITATION {}"),
                Arguments.of("JR_cite"),
                Arguments.of("JR_cite1"),
                Arguments.of("JR_cite_1"),
                Arguments.of("JR_cite1_1"),
                Arguments.of("JR_cite__key"),
                Arguments.of("JR_cite_0_key"),
                Arguments.of("JR_cite_7_key"),
                Arguments.of("JR_cite_10_key"),
                Arguments.of("JR_cite_999999999999999999999999_key"),
                Arguments.of("JR_cite_-1_key"),
                Arguments.of("JR_cite_01_key"),
                Arguments.of("JR_cite_NORMAL_key"),
                Arguments.of("JR_cite_1.0_key"),
                Arguments.of("JR_cite-1_1_key"),
                Arguments.of("JR_cite+1_1_key"),
                Arguments.of("JR_cite1.5_1_key"),
                Arguments.of("JR_citeabc_1_key"),
                Arguments.of("JR_cite1__1_key"),
                Arguments.of("jr_cite_1_key"),
                Arguments.of("JR_CITE_1_key"),
                Arguments.of("prefixJR_cite_1_key"),
                Arguments.of(" JR_cite_1_key"),
                Arguments.of("\tJR_cite_1_key"),
                Arguments.of("\nJR_cite_1_key"),
                Arguments.of("prefix\nJR_cite_1_key"));
    }

    @ParameterizedTest
    @MethodSource("namesWithLineBreaks")
    void rejectsLineBreaksInsteadOfDecodingAPartialMatch(String name) {
        assertThrows(JabRefMarkCodecException.class, () -> LegacyJStyleMark.decode(name));
    }

    private static Stream<Arguments> namesWithLineBreaks() {
        return Stream.of(
                Arguments.of("JR_cite_1_key\n"),
                Arguments.of("JR_cite_1_key\r"),
                Arguments.of("JR_cite_1_key\r\n"),
                Arguments.of("JR_cite_1_key\nother"),
                Arguments.of("JR_cite_1_key\r_other"),
                Arguments.of("JR_cite_1_key\u2028other"),
                Arguments.of("JR_cite_1_key\u2029other"));
    }
}
