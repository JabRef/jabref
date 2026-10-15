package org.jabref.logic.importer.util;

import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMetaDataParserTest {

    /// So that a library configured by a newer JabRef is handed back with everything it arrived
    /// with, rather than with the parts this one happens to understand.
    @Test
    void aKeyThisJabRefDoesNotModelIsKeptAsItStands() throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("""
                {"somethingElse": {"a": [1, 2]}}"""));

        assertEquals(Map.of("somethingElse", "{\"a\":[1,2]}"), metaData.getUnknownJsonMetaData());
    }

    @Test
    void theKeysKeepTheOrderTheFileHadThem() throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("""
                {"zeta": 1, "alpha": 2}"""));

        assertEquals(List.of("zeta", "alpha"), List.copyOf(metaData.getUnknownJsonMetaData().keySet()));
    }

    /// A key this JabFix does not read is kept for the JabFix that wrote it, rather than failing the
    /// whole library.
    @Test
    void aRuleKeyWithAPipeIsKeptAsAnUnreadRule() throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("""
                {"formatting": {"rules": {"title|booktitle:lower-case": "fix"}}}"""));

        assertEquals(Map.of("title|booktitle:lower-case", "\"fix\""),
                metaData.getFormatting().orElseThrow().unreadRules());
    }

    /// The caller keeps the text of a comment it could not read, so saying so is the whole job.
    @ParameterizedTest
    @ValueSource(strings = {"{\"a\": }", "{", "", "[1, 2]", "\"a string\"", "12"})
    void aCommentThatDoesNotHoldAnObjectIsRejected(String json) {
        assertThrows(ParseException.class, () -> JsonMetaDataParser.read(json));
    }
}
