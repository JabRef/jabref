package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class MarkCodecTest {

    @ParameterizedTest
    @CsvSource({
            "AUTHORYEAR_PAR, NORMAL",
            "AUTHORYEAR_INTEXT, IN_TEXT",
            "INVISIBLE_CIT, EMPTY",
            "AUTHORYEAR_NOPAR, NOPAR",
            "AUTHOR_ONLY, AUTHOR_ONLY",
            "YEAR_ONLY, YEAR_ONLY"
    })
    void encodesExistingCitationTypesWithoutNumbers(CitationType citationType, String wireType) throws MarkCodecException {
        JabRefMarkData mark = new JabRefMarkData(List.of("Smith2020", "Doe2021"), "stable-uid_1", citationType);

        assertEquals("JABREF_Smith2020, JABREF_Doe2021 " + wireType + " stable-uid_1", MarkCodec.encode(mark));
    }

    @ParameterizedTest
    @CsvSource({
            "NORMAL, AUTHORYEAR_PAR",
            "IN_TEXT, AUTHORYEAR_INTEXT",
            "EMPTY, INVISIBLE_CIT",
            "NOPAR, AUTHORYEAR_NOPAR",
            "AUTHOR_ONLY, AUTHOR_ONLY",
            "YEAR_ONLY, YEAR_ONLY"
    })
    void decodesEveryWireTypeToExistingEnum(String wireType, CitationType citationType) throws MarkCodecException {
        assertEquals(new JabRefMarkData(List.of("key"), "uid", citationType),
                MarkCodec.decode("JABREF_key " + wireType + " uid"));
    }

    @ParameterizedTest
    @EnumSource(CitationType.class)
    void roundTripsEveryCitationType(CitationType citationType) throws MarkCodecException {
        JabRefMarkData mark = new JabRefMarkData(List.of("β2020:一/2–3—4.foo+bar?{x}", "JABREF_literal", "NORMAL", "IN_TEXT", "β2020:一/2–3—4.foo+bar?{x}"),
                "b7140475-b2b2-4b4a-a963-666463d650c3", citationType);

        assertEquals(mark, MarkCodec.decode(MarkCodec.encode(mark)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"uid", "native_User-17", "b7140475-b2b2-4b4a-a963-666463d650c3", "1234567890", "NORMAL", "IN_TEXT", "EMPTY", "NOPAR", "AUTHOR_ONLY", "YEAR_ONLY"})
    void preservesOpaqueIdentifiersWithoutGeneratorSpecificParsing(String uid) throws MarkCodecException {
        JabRefMarkData data = new JabRefMarkData(List.of("key"), uid, CitationType.AUTHORYEAR_PAR);

        assertEquals(data, MarkCodec.decode(MarkCodec.encode(data)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NORMAL", "IN_TEXT", "EMPTY", "NOPAR", "AUTHOR_ONLY", "YEAR_ONLY"})
    void decodesTypeLookingIdentifiersOnlyFromTheFinalToken(String uid) throws MarkCodecException {
        assertEquals(new JabRefMarkData(List.of("NORMAL", "IN_TEXT"), uid, CitationType.INVISIBLE_CIT),
                MarkCodec.decode("JABREF_NORMAL, JABREF_IN_TEXT EMPTY " + uid));
    }

    @Test
    void preservesStorageOrderAndRepeatedKeys() throws MarkCodecException {
        assertEquals(List.of("z", "a", "z"), MarkCodec.decode("JABREF_z, JABREF_a, JABREF_z NORMAL uid").citationKeys());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "JR_cite1_1_key",
            "JR_cite0_6_key1,key2",
            "JABREF_key CID_1 uid NORMAL",
            "JABREF_key CID_42 uid",
            "JABREF_key CID_1, JABREF_other CID_2 uid IN_TEXT",
            "ZOTERO_ITEM CSL_CITATION {\"citationID\":\"uid\"}",
            "ZOTERO_BIBL {}",
            "JR_bib",
            "",
            "JABREF_key NORMAL",
            "JABREF_key AUTHORYEAR_PAR uid",
            "JABREF_key UNKNOWN uid",
            "JABREF_key normal uid",
            "JABREF_key NORMAL uid extra",
            " JABREF_key NORMAL uid",
            "JABREF_key NORMAL uid ",
            "JABREF_key NORMAL uid\n",
            "JABREF_key  NORMAL uid",
            "JABREF_key\tNORMAL uid",
            "JABREF_ NORMAL uid",
            "JABREF_key, NORMAL uid",
            "JABREF_key,JABREF_other NORMAL uid",
            "JABREF_key,  JABREF_other NORMAL uid",
            "JABREF_key, other NORMAL uid",
            "JABREF_key withspace NORMAL uid",
            "JABREF_key\u00a0other NORMAL uid",
            "JABREF_key NORMAL uid:0",
            "JABREF_key NORMAL u,id",
            "JABREF_key NORMAL 日本語"
    })
    void rejectsOldAndMalformedMarkNames(String name) {
        MarkCodecException error = assertThrows(MarkCodecException.class, () -> MarkCodec.decode(name));

        assertEquals(MarkCodecException.Reason.INVALID_MARK_NAME, error.reason());
        assertFalse(MarkCodec.isCanonicalMarkName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "JABREF_key opaque-uid NORMAL",
            "JABREF_key opaque-uid IN_TEXT",
            "JABREF_key opaque-uid EMPTY",
            "JABREF_key opaque-uid NOPAR",
            "JABREF_key opaque-uid AUTHOR_ONLY",
            "JABREF_key opaque-uid YEAR_ONLY",
            "JABREF_first, JABREF_second opaque-uid NORMAL"
    })
    void rejectsFormerUidBeforeTypeOrder(String name) {
        MarkCodecException error = assertThrows(MarkCodecException.class, () -> MarkCodec.decode(name));

        assertEquals(MarkCodecException.Reason.INVALID_MARK_NAME, error.reason());
        assertFalse(MarkCodec.isCanonicalMarkName(name));
    }

    @ParameterizedTest
    @EnumSource(CitationType.class)
    void recognizesCanonicalNames(CitationType citationType) throws MarkCodecException {
        JabRefMarkData mark = new JabRefMarkData(List.of("key"), "uid", citationType);

        assertTrue(MarkCodec.isCanonicalMarkName(MarkCodec.encode(mark)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "key,other", "key other", "key\tother", "key\nother", "key\u00a0other", "key\u2003other"})
    void rejectsMissingOrUnrepresentableKeysWithoutFallback(String key) {
        MarkCodecException error = assertThrows(MarkCodecException.class,
                () -> new JabRefMarkData(List.of(key), "uid", CitationType.AUTHORYEAR_PAR));

        assertEquals(MarkCodecException.Reason.INVALID_KEY, error.reason());
    }

    @ParameterizedTest
    @EnumSource(CitationType.class)
    void requiresKeysEvenForInvisibleCitations(CitationType citationType) {
        MarkCodecException error = assertThrows(MarkCodecException.class,
                () -> new JabRefMarkData(List.of(), "uid", citationType));

        assertEquals(MarkCodecException.Reason.EMPTY_KEYS, error.reason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "uid other", "uid,other", "uid:0", "uid\n", "日本語"})
    void rejectsInvalidUniqueIds(String uniqueId) {
        MarkCodecException error = assertThrows(MarkCodecException.class,
                () -> new JabRefMarkData(List.of("key"), uniqueId, CitationType.AUTHORYEAR_PAR));

        assertEquals(MarkCodecException.Reason.INVALID_UNIQUE_ID, error.reason());
    }

    @Test
    void copiesInputKeys() throws MarkCodecException {
        List<String> keys = new ArrayList<>(List.of("original"));
        JabRefMarkData mark = new JabRefMarkData(keys, "uid", CitationType.AUTHORYEAR_PAR);
        keys.set(0, "replacement");

        assertEquals(List.of("original"), mark.citationKeys());
    }

    @Test
    void exposesImmutableKeys() throws MarkCodecException {
        JabRefMarkData mark = MarkCodec.decode("JABREF_key NORMAL uid");

        assertThrows(UnsupportedOperationException.class, () -> mark.citationKeys().add("other"));
    }

    @Test
    void equalDataHasEqualHashCode() throws MarkCodecException {
        JabRefMarkData mark = new JabRefMarkData(List.of("key"), "uid", CitationType.AUTHORYEAR_PAR);

        assertEquals(mark.hashCode(), MarkCodec.decode(MarkCodec.encode(mark)).hashCode());
    }
}
