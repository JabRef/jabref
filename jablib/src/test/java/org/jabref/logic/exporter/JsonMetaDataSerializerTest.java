package org.jabref.logic.exporter;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonMetaDataSerializerTest {

    /// Which is what lets a library that has never had such a comment be written exactly as before.
    @Test
    void aLibraryThatKeepsNothingThisWayProducesNoObject() {
        assertEquals(Optional.empty(), JsonMetaDataSerializer.serialize(new MetaData()));
    }

    @Test
    void whatANewerJabRefWroteIsWrittenBackAsItCame() {
        MetaData metaData = new MetaData();
        metaData.putUnknownJsonMetaDataItem("somethingElse", "{\"a\":[1,2]}");

        assertEquals(Optional.of("""
                {
                  "somethingElse" : {
                    "a" : [ 1, 2 ]
                  }
                }"""), JsonMetaDataSerializer.serialize(metaData));
    }

    /// Among themselves, the keys of a newer JabRef keep the order the file had.
    @Test
    void unknownKeysKeepTheOrderTheyCameIn() {
        MetaData metaData = new MetaData();
        metaData.putUnknownJsonMetaDataItem("zeta", "1");
        metaData.putUnknownJsonMetaDataItem("alpha", "2");

        assertEquals(Optional.of("""
                {
                  "zeta" : 1,
                  "alpha" : 2
                }"""), JsonMetaDataSerializer.serialize(metaData));
    }

    /// The keys this JabRef knows come first, so that what it writes is written the same each time.
    @Test
    void theFormattingBlockComesBeforeTheKeysOfANewerJabRef() {
        MetaData metaData = new MetaData();
        metaData.putUnknownJsonMetaDataItem("somethingElse", "1");
        metaData.setFormatting(LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX));

        assertEquals(Optional.of("""
                {
                  "formatting" : {
                    "rules" : {
                      "surrounding-whitespace" : "fix"
                    }
                  },
                  "somethingElse" : 1
                }"""), JsonMetaDataSerializer.serialize(metaData));
    }

    /// A migration may state a selector whose entry this JabFix could not read; what is stated is
    /// what this JabFix acts on, so it must not be written over.
    @Test
    void anUnreadEntryDoesNotReplaceOneStatedUnderTheSameKey() {
        SequencedMap<RuleSelector, RuleMode> modes = new LinkedHashMap<>();
        modes.put(RuleSelector.on(StandardField.TITLE, "lower-case"), RuleMode.FIX);
        SequencedMap<String, String> unreadRules = new LinkedHashMap<>();
        unreadRules.put("title:lower-case", "\"warn\"");

        assertEquals("{\"rules\":{\"title:lower-case\":\"fix\"}}",
                JsonMetaDataSerializer.formatting(new LintSettings(modes, unreadRules, new LinkedHashMap<>())).toString());
    }

    @Test
    void anUnreadRulesKeyDoesNotReplaceTheRulesThatAreStated() {
        SequencedMap<String, String> unreadKeys = new LinkedHashMap<>();
        unreadKeys.put(MetaData.FORMATTING_RULES, "\"recommended\"");
        LintSettings settings = new LintSettings(
                LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX).ruleModes(),
                new LinkedHashMap<>(),
                unreadKeys);

        assertEquals("{\"rules\":{\"surrounding-whitespace\":\"fix\"}}",
                JsonMetaDataSerializer.formatting(settings).toString());
    }

    @Test
    void anUnreadFormattingKeyDoesNotReplaceTheBlockThatIsStated() {
        MetaData metaData = new MetaData();
        metaData.putUnknownJsonMetaDataItem(MetaData.FORMATTING, "\"recommended\"");
        metaData.setFormatting(LintSettings.of(RuleSelector.of("surrounding-whitespace"), RuleMode.FIX));

        assertEquals(Optional.of("""
                {
                  "formatting" : {
                    "rules" : {
                      "surrounding-whitespace" : "fix"
                    }
                  }
                }"""), JsonMetaDataSerializer.serialize(metaData));
    }

    /// The comment holding the object ends where its braces balance, so none may stand in a string.
    @Test
    void aBraceInsideAStringIsEscaped() {
        MetaData metaData = new MetaData();
        metaData.putUnknownJsonMetaDataItem("pattern{", "\"}\"");

        assertEquals(Optional.of("""
                {
                  "pattern\\u007B" : "\\u007D"
                }"""), JsonMetaDataSerializer.serialize(metaData));
    }
}
