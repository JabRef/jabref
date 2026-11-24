package org.jabref.logic.importer.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;
import org.jabref.model.metadata.RuleSettings;

import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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
    public static ObjectNode read(String json) throws ParseException {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new ParseException(Localization.lang("Ill-formed JSON metadata comment in BIB file"), e);
        }
        if (!(root instanceof ObjectNode object)) {
            throw new ParseException(Localization.lang("The JSON metadata comment in the BIB file does not hold an object"));
        }
        return object;
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
        Optional<String> mode = Optional.empty();
        SequencedMap<RuleSelector, RuleSettings> rules = new LinkedHashMap<>();
        SequencedMap<String, String> unreadRules = new LinkedHashMap<>();
        SequencedMap<String, String> unreadKeys = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> item : block.properties()) {
            if (MetaData.FORMATTING_MODE.equals(item.getKey()) && item.getValue().isString()) {
                mode = Optional.of(item.getValue().stringValue());
            } else if (MetaData.FORMATTING_RULES.equals(item.getKey()) && item.getValue().isObject()) {
                item.getValue().properties().forEach(rule -> read(rule.getKey(), rule.getValue())
                        .ifPresentOrElse(
                                read -> rules.put(read.getKey(), read.getValue()),
                                () -> unreadRules.put(rule.getKey(), rule.getValue().toString())));
            } else {
                unreadKeys.put(item.getKey(), item.getValue().toString());
            }
        }
        return new LintSettings(mode, rules, unreadRules, unreadKeys);
    }

    /// The selector and the settings of one entry, where this JabFix reads both.
    ///
    /// Values on an entry naming a field are not read yet: a rule is built once for a run and not
    /// once per field, so they could not be acted on, and reading them would let the same entry
    /// quietly mean one thing now and another later.
    private static Optional<Map.Entry<RuleSelector, RuleSettings>> read(String key, JsonNode value) {
        return RuleSelector.parse(key)
                           .flatMap(selector -> ruleSettings(value)
                                   .filter(settings -> selector.field().isEmpty() || settings.values().isEmpty())
                                   .map(settings -> Map.entry(selector, settings)));
    }

    /// An entry is a mode, or an object holding a mode beside the values the rule takes.
    ///
    /// Values are strings for now, since no rule takes anything else, and an entry holding something
    /// else is not read at all rather than read without it.
    private static Optional<RuleSettings> ruleSettings(JsonNode entry) {
        if (entry.isString()) {
            return RuleMode.fromKey(entry.stringValue()).map(RuleSettings::new);
        }
        if (!entry.isObject()) {
            return Optional.empty();
        }
        Optional<RuleMode> mode = Optional.empty();
        SequencedMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> item : entry.properties()) {
            if (!item.getValue().isString()) {
                return Optional.empty();
            }
            if (MetaData.FORMATTING_RULE_MODE.equals(item.getKey())) {
                mode = RuleMode.fromKey(item.getValue().stringValue());
            } else {
                values.put(item.getKey(), item.getValue().stringValue());
            }
        }
        return mode.map(ruleMode -> new RuleSettings(ruleMode, values));
    }
}
