package org.jabref.logic.exporter;

import java.util.Optional;

import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.node.ObjectNode;

/// Writes the metadata JabRef keeps as one embedded JSON object, rather than as one `jabref-meta:`
/// item per setting.
///
/// The `jabref-meta:` items cannot nest: their separator is the same `;` that a save action uses
/// inside its own value, so a setting made of more than a list of strings has nowhere to go. One
/// JSON object has room for all of them, which is what
/// <https://github.com/JabRef/jabref/issues/10371> settles.
///
/// Reading it back is [org.jabref.logic.importer.util.JsonMetaDataParser].
@NullMarked
public class JsonMetaDataSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMetaDataSerializer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /// Line breaks of its own would be rewritten by the writer anyway, and a `.bib` file is read in
    /// diffs, so the object is laid out the way a person would lay it out.
    private static final DefaultPrettyPrinter PRINTER =
            new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));

    private static final ObjectWriter WRITER = MAPPER.writer().with(PRINTER).with(new BraceEscapes());

    private JsonMetaDataSerializer() {
    }

    /// The JSON object of everything `metaData` keeps this way, or nothing where it keeps nothing --
    /// which is what lets a library that has never had such a comment be written exactly as before.
    ///
    /// The keys this JabRef knows come first, in a fixed order, and whatever a newer JabRef wrote
    /// follows in the order the file had it. A file this JabRef wrote keeps its order that way, and
    /// so does one of a newer JabRef that puts the keys it adds behind these; a file whose keys
    /// somebody reordered by hand is put back into this order once.
    public static Optional<String> serialize(MetaData metaData) {
        ObjectNode root = MAPPER.createObjectNode();

        // Even an empty block: it says the library is configured, and that nothing is enabled.
        metaData.getFormatting()
                .ifPresent(settings -> root.set(MetaData.FORMATTING, formatting(settings)));

        // Whatever a newer JabRef wrote goes back as it came. That includes a `formatting` this
        // JabRef could not read as a block, unless the library has since been given one it can.
        metaData.getUnknownJsonMetaData().forEach((key, json) -> putUnlessStated(root, key, json));

        if (root.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(WRITER.writeValueAsString(root));
    }

    /// What the library asks JabFix to do, as the `formatting` block.
    ///
    /// The mode comes first. Of the rules, the modes this JabFix read are written first, in the
    /// order it read them, and what it could not read follows. A configuration somebody edited by
    /// hand therefore keeps its order, except that an entry of a newer JabFix moves to the end of its
    /// group -- which beats dropping it.
    ///
    /// Where the library has since stated a key it could not read before -- a migration may have
    /// moved a Save Action onto an entry this JabFix could not read the mode of -- what it states
    /// is what it acts on, so that is what is written.
    static ObjectNode formatting(LintSettings settings) {
        ObjectNode rules = MAPPER.createObjectNode();
        settings.ruleModes().forEach((selector, mode) -> rules.put(selector.asKey(), mode.asKey()));
        settings.unreadRules().forEach((key, json) -> putUnlessStated(rules, key, json));

        ObjectNode block = MAPPER.createObjectNode();
        settings.mode().ifPresent(mode -> block.put(MetaData.FORMATTING_MODE, mode));
        if (!rules.isEmpty()) {
            block.set(MetaData.FORMATTING_RULES, rules);
        }
        settings.unreadKeys().forEach((key, json) -> putUnlessStated(block, key, json));
        return block;
    }

    private static void putUnlessStated(ObjectNode node, String key, String json) {
        if (!node.has(key)) {
            asNode(key, json).ifPresent(value -> node.set(key, value));
        }
    }

    /// Text this JabRef put aside itself, so it parsed once already; a failure here would mean the
    /// library was changed underneath us, and dropping that one key beats failing the save.
    private static Optional<JsonNode> asNode(String key, String json) {
        try {
            return Optional.of(MAPPER.readTree(json));
        } catch (JacksonException e) {
            LOGGER.warn("Dropping the JSON metadata item '{}', which is no longer readable", key, e);
            return Optional.empty();
        }
    }

    /// Writes a brace inside a string or a key as its JSON Unicode escape.
    ///
    /// The `@Comment` holding the object ends where its braces balance, and the reader counts every
    /// brace it meets, those inside a string included. Escaped, the only braces left are the
    /// object's own, which balance by construction -- whatever a value says, and whether or not it
    /// came escaped from a newer JabRef.
    private static final class BraceEscapes extends CharacterEscapes {
        private final int[] escapes;

        BraceEscapes() {
            escapes = CharacterEscapes.standardAsciiEscapesForJSON();
            escapes['{'] = CharacterEscapes.ESCAPE_STANDARD;
            escapes['}'] = CharacterEscapes.ESCAPE_STANDARD;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return escapes;
        }

        /// None: the braces take the standard escape, and nothing else is escaped any other way.
        @Override
        public @Nullable SerializableString getEscapeSequence(int character) {
            return null;
        }
    }
}
