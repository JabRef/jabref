package org.jabref.logic.openoffice;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.engine.MarkCodec;
import org.jabref.logic.openoffice.engine.MarkCodecException;
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
    void defaultIdentityHasTwentyFourCharacters() {
        assertEquals(24, CitationIds.newId().length());
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 10, 24})
    void identityUsesRequestedWireLength(int length) {
        assertEquals(length, CitationIds.newId(length).length());
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 10, 24})
    void identityUsesCuid2Alphabet(int length) {
        assertTrue(CUID2.matcher(CitationIds.newId(length)).matches());
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 10, 24})
    void separateCallsProduceDistinctIdentities(int length) {
        assertEquals(100L, IntStream.range(0, 100).mapToObj(_ -> CitationIds.newId(length)).distinct().count());
    }

    @Test
    void defaultIdentityRoundTripsThroughCanonicalStorage() throws MarkCodecException {
        JabRefMarkData data = new JabRefMarkData(List.of("key"), CitationIds.newId(), CitationType.AUTHORYEAR_PAR);

        assertEquals(data, MarkCodec.decode(MarkCodec.encode(data)));
    }
}
