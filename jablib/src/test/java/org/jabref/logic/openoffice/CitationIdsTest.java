package org.jabref.logic.openoffice;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.jabref.logic.openoffice.engine.JabRefMarkCodec;
import org.jabref.logic.openoffice.engine.JabRefMarkCodecException;
import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class CitationIdsTest {

    private static final Pattern CUID2 = Pattern.compile("[a-z][a-z0-9]*");

    @Test
    void identityHasTenCharacters() {
        assertEquals(10, CitationIds.newId().length());
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 10})
    void identityUsesRequestedWireLength(int length) {
        assertEquals(length, CitationIds.newId(length).length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "abcdefghij", "abcdefghijklmnopqrstuvwx", "foreign_UID-7"})
    void storedIdentityRoundTripsWithoutChangingItsLength(String identity) throws JabRefMarkCodecException {
        JabRefMarkData data = new JabRefMarkData(List.of("key"), identity, CitationType.AUTHORYEAR_PAR);

        assertEquals(data, JabRefMarkCodec.decode(JabRefMarkCodec.encode(data)));
    }

    @Test
    void identityUsesCuid2Alphabet() {
        assertTrue(CUID2.matcher(CitationIds.newId()).matches());
    }

    @Test
    void separateCallsProduceDistinctIdentities() {
        assertEquals(100L, IntStream.range(0, 100).mapToObj(_ -> CitationIds.newId()).distinct().count());
    }

    @Test
    void defaultIdentityRoundTripsThroughCanonicalStorage() throws JabRefMarkCodecException {
        JabRefMarkData data = new JabRefMarkData(List.of("key"), CitationIds.newId(), CitationType.AUTHORYEAR_PAR);

        assertEquals(data, JabRefMarkCodec.decode(JabRefMarkCodec.encode(data)));
    }
}
