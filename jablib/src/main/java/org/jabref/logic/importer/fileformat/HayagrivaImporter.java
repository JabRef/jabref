package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/// Importer for the Hayagriva YAML format used by Typst (<https://github.com/typst/hayagriva>).
///
/// Hayagriva allows most fields to take either a plain scalar or a structured form: `title` and
/// `url` may be a string or a map with a `value` key, `author`/`editor` may be a string, a list of
/// strings, or a list of maps, `parent` may be a map or a list of maps, and `serial-number` may be
/// a scalar or a map. Parsing therefore works on the untyped node tree instead of fixed POJOs,
/// extracting what JabRef can represent and ignoring the rest. The actual field mapping lives in
/// [HayagrivaMapping], shared with the Hayagriva writer.
@NullMarked
public class HayagrivaImporter extends Importer {

    /// All entry types of the Hayagriva specification
    /// (<https://github.com/typst/hayagriva/blob/main/docs/file-format.md#entry-type>); used for
    /// format recognition. Types without a BibTeX equivalent in
    /// [HayagrivaMapping#TYPE_TO_ENTRY_TYPE] are imported as
    /// [org.jabref.model.entry.types.StandardEntryType#Misc].
    private static final Set<String> RECOGNIZED_TYPES = Set.of(
            "anthology", "anthos", "article", "artwork", "audio", "blog", "book", "case",
            "chapter", "conference", "entry", "exhibition", "legislation", "manuscript", "misc",
            "newspaper", "original", "patent", "performance", "periodical", "post", "proceedings",
            "reference", "report", "repository", "scene", "thesis", "thread", "video", "web"
    );

    /// Generous room for leading comments plus the first entry, whose `type` key conventionally
    /// appears within its first few lines.
    private static final int MAX_LOOKAHEAD_CHARS = 3_000;
    private static final Pattern TYPE_LINE_PATTERN = Pattern.compile("^\\s*type:\\s*\"?'?([A-Za-z-]+)\"?'?\\s*$");

    private static final YAMLMapper MAPPER = YAMLMapper.builder()
                                                       .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                                                       .build();

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        JsonNode root;
        try {
            root = MAPPER.readTree(input);
        } catch (JacksonException e) {
            return ParserResult.fromError(e);
        }

        List<BibEntry> bibEntries = new ArrayList<>();
        if (!root.isObject()) {
            return new ParserResult(bibEntries);
        }

        for (Map.Entry<String, JsonNode> property : root.properties()) {
            JsonNode entryNode = property.getValue();
            if (entryNode.isObject()) {
                bibEntries.add(HayagrivaMapping.toBibEntry(property.getKey(), entryNode));
            }
        }

        return new ParserResult(bibEntries);
    }

    @Override
    public String getId() {
        return "hayagrivayaml";
    }

    @Override
    public String getName() {
        return "Hayagriva YAML";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the Hayagriva YAML format.");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.YAML;
    }

    /// Lightweight recognition for the [BufferedReader] overload, which must reset the reader
    /// afterwards. Instead of doing a full YAML parse (which could read past the marked
    /// read-ahead limit and make `reset()` throw), this scans the first [#MAX_LOOKAHEAD_CHARS]
    /// characters for a `type:` line matching a known Hayagriva entry type. The block read never
    /// exceeds the marked limit, so `reset()` is always possible (a line-based read could exceed
    /// it on a single overlong line).
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        input.mark(MAX_LOOKAHEAD_CHARS);
        try {
            char[] lookahead = new char[MAX_LOOKAHEAD_CHARS];
            int filled = 0;
            int charsRead;
            while (filled < MAX_LOOKAHEAD_CHARS && (charsRead = input.read(lookahead, filled, MAX_LOOKAHEAD_CHARS - filled)) != -1) {
                filled += charsRead;
            }

            return new String(lookahead, 0, filled).lines().anyMatch(line -> {
                Matcher matcher = TYPE_LINE_PATTERN.matcher(line);
                return matcher.matches() && RECOGNIZED_TYPES.contains(matcher.group(1).toLowerCase(Locale.ROOT));
            });
        } finally {
            input.reset();
        }
    }

    /// Full recognition for the [Reader] overload, which has no reset requirement, so a complete
    /// (but untyped) YAML parse is safe here.
    @Override
    public boolean isRecognizedFormat(Reader input) throws IOException {
        JsonNode root;
        try {
            root = MAPPER.readTree(input);
        } catch (JacksonException e) {
            return false;
        }

        if (!root.isObject()) {
            return false;
        }

        for (JsonNode entryNode : root.values()) {
            if (entryNode.isObject() && HayagrivaMapping.scalarText(entryNode.get("type"))
                    .map(type -> RECOGNIZED_TYPES.contains(type.toLowerCase(Locale.ROOT)))
                    .orElse(false)) {
                return true;
            }
        }

        return false;
    }
}
