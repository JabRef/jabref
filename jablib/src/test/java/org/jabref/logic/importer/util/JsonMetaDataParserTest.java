package org.jabref.logic.importer.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;
import org.jabref.model.metadata.RuleSettings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ResourceLock("Localization.lang")
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

    @Test
    void theModeIsRead() throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("""
                {"formatting": {"mode": "whitespace"}}"""));

        assertEquals(Optional.of("whitespace"), metaData.getFormatting().orElseThrow().mode());
    }

    /// Not a mode this JabFix can name, so it is kept for the JabFix that wrote it.
    @Test
    void aModeThatIsNotAStringIsKeptAsAnUnreadKey() throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("""
                {"formatting": {"mode": ["whitespace", "report"]}}"""));

        assertEquals(Map.of("mode", "[\"whitespace\",\"report\"]"), metaData.getFormatting().orElseThrow().unreadKeys());
    }

    @Test
    void aRuleGivenValuesIsRead() throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("""
                {"formatting": {"rules": {"entry-brackets": {"mode": "check", "bracket": "parentheses"}}}}"""));

        assertEquals(new RuleSettings(RuleMode.CHECK, new LinkedHashMap<>(Map.of("bracket", "parentheses"))),
                metaData.getFormatting().orElseThrow().rules().get(RuleSelector.of("entry-brackets")));
    }

    /// An object that holds no values says what the short form says.
    @ParameterizedTest
    @ValueSource(strings = {"{\"mode\":\"fix\"}", "\"fix\""})
    void anEntryWithoutValuesIsAMode(String entry) throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("{\"formatting\": {\"rules\": {\"some-rule\": " + entry + "}}}"));

        assertEquals(new RuleSettings(RuleMode.FIX), metaData.getFormatting().orElseThrow().rules().get(RuleSelector.of("some-rule")));
    }

    /// Not read rather than read without what it does not understand: such a rule does not run, and
    /// the entry goes back as it came.
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"bracket\":\"parentheses\"}", // no mode
            "{\"mode\":\"warn\",\"bracket\":\"parentheses\"}", // a mode this JabFix does not know
            "{\"mode\":[\"fix\"],\"bracket\":\"parentheses\"}", // a mode that is no string
            "{\"mode\":\"check\",\"max\":120}", // a value that is no string
            "[\"fix\"]",
            "true"
    })
    void anEntryThisJabFixCannotReadIsKeptAsItStands(String entry) throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("{\"formatting\": {\"rules\": {\"some-rule\": " + entry + "}}}"));

        LintSettings formatting = metaData.getFormatting().orElseThrow();
        assertEquals(Map.of(), formatting.rules());
        assertEquals(Map.of("some-rule", entry), formatting.unreadRules());
    }

    /// A rule is built once for a run, so values could not be acted on field by field.
    @Test
    void valuesOnAnEntryNamingAFieldAreKeptAsAnUnreadRule() throws ParseException {
        MetaData metaData = new MetaData();

        JsonMetaDataParser.parse(metaData, JsonMetaDataParser.read("""
                {"formatting": {"rules": {"title:some-rule": {"mode": "fix", "max": "120"}}}}"""));

        assertEquals(Map.of("title:some-rule", "{\"mode\":\"fix\",\"max\":\"120\"}"), metaData.getFormatting().orElseThrow().unreadRules());
    }

    /// The caller keeps the text of a comment it could not read, so saying so is the whole job.
    @ParameterizedTest
    @ValueSource(strings = {"{\"a\": }", "{", "", "[1, 2]", "\"a string\"", "12"})
    void aCommentThatDoesNotHoldAnObjectIsRejected(String json) {
        assertThrows(ParseException.class, () -> JsonMetaDataParser.read(json));
    }
}
