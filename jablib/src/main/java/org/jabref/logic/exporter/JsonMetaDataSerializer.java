package org.jabref.logic.exporter;

import java.util.Optional;

import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

    private JsonMetaDataSerializer() {
    }

    /// The JSON object of everything `metaData` keeps this way, or nothing where it keeps nothing --
    /// which is what lets a library that has never had such a comment be written exactly as before.
    public static Optional<String> serialize(MetaData metaData) {
        ObjectNode root = MAPPER.createObjectNode();

        // Whatever a newer JabRef wrote goes back as it came. It is put in first so that the keys
        // this JabRef knows cannot push it out of the order the file had.
        metaData.getUnknownJsonMetaData().forEach((key, json) -> asNode(key, json).ifPresent(node -> root.set(key, node)));

        if (root.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(MAPPER.writer().with(PRINTER).writeValueAsString(root));
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
}
