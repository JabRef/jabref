package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;

import org.jabref.model.entry.field.BiblatexSoftwareField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SWHIDTest {

    private static final String DIRECTORY_SWHID = "swh:1:dir:2dc0f462d191524530f5612d2935851505af41dd";

    private static final String QUALIFIED_SWHID = "swh:1:dir:2dc0f462d191524530f5612d2935851505af41dd;origin=https://github.com/rdicosmo/parmap;visit=swh:1:snp:2128ed4f25f2d7ae7c8b7950a611d69cf4429063";

    @Test
    void parse_validPlainSwhid_returnsSwhid() {
        Optional<SWHID> swhid = SWHID.parse(DIRECTORY_SWHID);
        assertTrue(swhid.isPresent());
        assertEquals(DIRECTORY_SWHID, swhid.get().asString());
        assertEquals(BiblatexSoftwareField.SWHID, swhid.get().getDefaultField());
        assertEquals(Optional.of(URI.create("https://archive.softwareheritage.org/" + DIRECTORY_SWHID)), swhid.get().getExternalURI());
    }

    @Test
    void parse_qualifiedSwhid_returnsSwhid() {
        Optional<SWHID> swhid = SWHID.parse(QUALIFIED_SWHID);
        assertTrue(swhid.isPresent());
        assertEquals(QUALIFIED_SWHID, swhid.get().asString());
    }

    @Test
    void parse_urlPrefixedSwhid_returnsStrippedSwhid() {
        Optional<SWHID> swhid = SWHID.parse("https://archive.softwareheritage.org/" + DIRECTORY_SWHID);
        assertTrue(swhid.isPresent());
        assertEquals(DIRECTORY_SWHID, swhid.get().asString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "not-a-swhid",
            "swh:2:dir2dc0f462d191524530f5612d2935851505af41dd",
            "swh:1:dir2dc0f462d191524530f5612d2935851505af41dd",
            "swh:1:dir:short"})
    void parse_invalidInputs_returnsEmpty(String invalidInput) {
        assertFalse(SWHID.parse(invalidInput).isPresent());
    }
}
