package org.jabref.logic.importer.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;

import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/// Reads the metadata JabRef keeps as one embedded JSON object. Writing it is
/// [org.jabref.logic.exporter.JsonMetaDataSerializer].
@NullMarked
public class JsonMetaDataParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonMetaDataParser() {
    }

    /// The object `json` holds.
    ///
    /// Reading is separate from applying it so that a caller learns whether the comment is readable
    /// while it still can act on the answer: JabRef writes such a comment back itself and therefore
    /// does not keep its text, and an unreadable one has to keep its text instead. A missing comma
    /// is no reason to delete what somebody wrote.
    ///
    /// @throws ParseException if the comment does not hold one JSON object
    public static JsonNode read(String json) throws ParseException {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new ParseException("Ill-formed JSON metadata comment in BIB file", e);
        }
        if (!root.isObject()) {
            throw new ParseException("The JSON metadata comment in the BIB file does not hold an object");
        }
        return root;
    }

    /// Reads what `root` says into `metaData`.
    ///
    /// A key this JabRef does not model is kept as it stands rather than dropped, so that a library
    /// configured by a newer one is handed back with everything it arrived with.
    public static void parse(MetaData metaData, JsonNode root) {
        root.properties().forEach(item -> {
            if (MetaData.FORMATTING.equals(item.getKey()) && item.getValue().isObject()) {
                metaData.setFormatting(formatting(item.getValue()));
            } else {
                metaData.putUnknownJsonMetaDataItem(item.getKey(), item.getValue().toString());
            }
        });
    }

    /// What the library asks JabFix to do.
    ///
    /// An entry this JabFix cannot read -- a selector spelled in a way it does not accept, or a
    /// mode it does not know -- is kept as it stands rather than dropped or guessed at. A newer
    /// JabFix may mean something by it, and this one saving the library must not be what loses it.
    private static LintSettings formatting(JsonNode block) {
        SequencedMap<RuleSelector, RuleMode> ruleModes = new LinkedHashMap<>();
        SequencedMap<String, String> unreadRules = new LinkedHashMap<>();
        SequencedMap<String, String> unreadKeys = new LinkedHashMap<>();

        block.properties().forEach(item -> {
            if (MetaData.FORMATTING_RULES.equals(item.getKey()) && item.getValue().isObject()) {
                item.getValue().properties().forEach(rule -> read(rule.getKey(), rule.getValue())
                        .ifPresentOrElse(
                                mode -> ruleModes.put(mode.getKey(), mode.getValue()),
                                () -> unreadRules.put(rule.getKey(), rule.getValue().toString())));
            } else {
                unreadKeys.put(item.getKey(), item.getValue().toString());
            }
        });
        return new LintSettings(ruleModes, unreadRules, unreadKeys);
    }

    /// The selector and the mode of one entry, where this JabFix reads both.
    private static Optional<Map.Entry<RuleSelector, RuleMode>> read(String key, JsonNode value) {
        if (!value.isString()) {
            return Optional.empty();
        }
        return RuleSelector.parse(key)
                           .flatMap(selector -> RuleMode.fromKey(value.stringValue())
                                                        .map(mode -> Map.entry(selector, mode)));
    }
}
