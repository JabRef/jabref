package org.jabref.logic.importer.util;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.metadata.MetaData;

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
        root.properties().forEach(item -> metaData.putUnknownJsonMetaDataItem(item.getKey(), item.getValue().toString()));
    }
}
