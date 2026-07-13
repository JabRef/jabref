package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
/// extracting what JabRef can represent and ignoring the rest.
@NullMarked
public class HayagrivaImporter extends Importer {

    private static final Map<String, EntryType> TYPES_MAP = Map.ofEntries(
            Map.entry("anthology", StandardEntryType.Collection),
            Map.entry("anthos", StandardEntryType.InCollection),
            Map.entry("article", StandardEntryType.Article),
            Map.entry("book", StandardEntryType.Book),
            Map.entry("chapter", StandardEntryType.InBook),
            Map.entry("conference", StandardEntryType.InProceedings),
            Map.entry("misc", StandardEntryType.Misc),
            Map.entry("proceedings", StandardEntryType.Proceedings),
            Map.entry("reference", StandardEntryType.Reference),
            Map.entry("report", StandardEntryType.Report),
            Map.entry("thesis", StandardEntryType.Thesis),
            Map.entry("web", StandardEntryType.Online)
    );

    /// All entry types of the Hayagriva specification
    /// (<https://github.com/typst/hayagriva/blob/main/docs/file-format.md#entry-type>); used for
    /// format recognition. Types without a BibTeX equivalent in [#TYPES_MAP] are imported as
    /// [StandardEntryType#Misc].
    private static final Set<String> RECOGNIZED_TYPES = Set.of(
            "anthology", "anthos", "article", "artwork", "audio", "blog", "book", "case",
            "chapter", "conference", "entry", "exhibition", "legislation", "manuscript", "misc",
            "newspaper", "original", "patent", "performance", "periodical", "post", "proceedings",
            "reference", "report", "repository", "scene", "thesis", "thread", "video", "web"
    );

    private static final Set<String> JOURNAL_PARENT_TYPES = Set.of("periodical", "newspaper", "blog");
    private static final Set<String> BOOKTITLE_PARENT_TYPES = Set.of("proceedings", "anthology", "conference");
    private static final Set<EntryType> BOOK_PART_TYPES = Set.of(StandardEntryType.InBook, StandardEntryType.InCollection, StandardEntryType.InProceedings);

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
                bibEntries.add(toBibEntry(property.getKey(), entryNode));
            }
        }

        return new ParserResult(bibEntries);
    }

    private BibEntry toBibEntry(String citationKey, JsonNode entryNode) {
        EntryType entryType = scalarText(entryNode.get("type"))
                .map(type -> TYPES_MAP.getOrDefault(type.toLowerCase(Locale.ROOT), StandardEntryType.Misc))
                .orElse(StandardEntryType.Misc);
        BibEntry bibEntry = new BibEntry(entryType);
        bibEntry.setCitationKey(citationKey);

        formattedText(entryNode.get("title")).ifPresent(title -> bibEntry.setField(StandardField.TITLE, title));
        formattedText(entryNode.get("url")).ifPresent(url -> bibEntry.setField(StandardField.URL, url));
        // A leading `~` (Hayagriva's approximate-date marker) is kept: JabRef stores the raw
        // string even when it cannot parse it into a structured date, so nothing is lost.
        scalarText(entryNode.get("date")).ifPresent(date -> bibEntry.setField(StandardField.DATE, date));
        persons(entryNode.get("author")).ifPresent(authors -> bibEntry.setField(StandardField.AUTHOR, authors));
        persons(entryNode.get("editor")).ifPresent(editors -> bibEntry.setField(StandardField.EDITOR, editors));
        scalarText(entryNode.get("volume")).ifPresent(volume -> bibEntry.setField(StandardField.VOLUME, volume));
        scalarText(entryNode.get("issue")).ifPresent(issue -> bibEntry.setField(StandardField.NUMBER, issue));
        scalarText(entryNode.get("page-range")).ifPresent(pages -> bibEntry.setField(StandardField.PAGES, pages));
        scalarText(entryNode.get("chapter")).ifPresent(chapter -> bibEntry.setField(StandardField.CHAPTER, chapter));
        scalarText(entryNode.get("edition")).ifPresent(edition -> bibEntry.setField(StandardField.EDITION, edition));
        scalarText(entryNode.get("publisher")).ifPresent(publisher -> bibEntry.setField(StandardField.PUBLISHER, publisher));
        scalarText(entryNode.get("location")).ifPresent(location -> bibEntry.setField(StandardField.ADDRESS, location));
        scalarText(entryNode.get("organization")).ifPresent(organization -> bibEntry.setField(StandardField.INSTITUTION, organization));
        scalarText(entryNode.get("note")).ifPresent(note -> bibEntry.setField(StandardField.NOTE, note));
        scalarText(entryNode.get("abstract")).ifPresent(abstractText -> bibEntry.setField(StandardField.ABSTRACT, abstractText));
        scalarText(entryNode.get("language")).ifPresent(language -> bibEntry.setField(StandardField.LANGUAGE, language));
        scalarText(entryNode.get("page-total")).ifPresent(pageTotal -> bibEntry.setField(StandardField.PAGETOTAL, pageTotal));
        scalarText(entryNode.get("volume-total")).ifPresent(volumeTotal -> bibEntry.setField(StandardField.VOLUMES, volumeTotal));
        scalarText(entryNode.get("runtime")).ifPresent(runtime -> bibEntry.setField(new UnknownField("runtime"), runtime));
        scalarText(entryNode.get("time-range")).ifPresent(timeRange -> bibEntry.setField(new UnknownField("time-range"), timeRange));

        applyAffiliated(bibEntry, entryNode.get("affiliated"));

        JsonNode serialNumber = entryNode.get("serial-number");
        if (serialNumber != null && serialNumber.isObject()) {
            scalarText(serialNumber.get("doi")).ifPresent(doi -> bibEntry.setField(StandardField.DOI, doi));
            scalarText(serialNumber.get("isbn")).ifPresent(isbn -> bibEntry.setField(StandardField.ISBN, isbn));
            scalarText(serialNumber.get("issn")).ifPresent(issn -> bibEntry.setField(StandardField.ISSN, issn));
            scalarText(serialNumber.get("pmid")).ifPresent(pmid -> bibEntry.setField(StandardField.PMID, pmid));
            scalarText(serialNumber.get("arxiv")).ifPresent(arxiv -> {
                bibEntry.setField(StandardField.EPRINT, arxiv);
                bibEntry.setField(StandardField.EPRINTTYPE, "arxiv");
            });
            scalarText(serialNumber.get("serial")).ifPresent(serial -> setIfAbsent(bibEntry, StandardField.NUMBER, serial));
        } else {
            // A bare serial number (an RFC number, a court docket, a version string, ...) has no
            // dedicated BibTeX field; number is the closest match, but an explicit issue wins
            scalarText(serialNumber).ifPresent(serial -> setIfAbsent(bibEntry, StandardField.NUMBER, serial));
        }

        applyParents(bibEntry, entryNode.get("parent"));

        return bibEntry;
    }

    /// A Hayagriva `parent` describes what an entry is contained in and has no direct BibTeX
    /// equivalent. The parent's `type` picks the target field, mirroring the Hayagriva exporter
    /// layout where possible (journal is exported as a `periodical` parent, series as a `book`
    /// parent). A missing or unmapped parent type falls back by entry type: articles are most
    /// likely contained in a journal, everything else in some book-like container. The first
    /// parent providing a value for a field wins; nested parents are not descended into.
    private void applyParents(BibEntry bibEntry, @Nullable JsonNode parentNode) {
        if (parentNode == null) {
            return;
        }

        List<JsonNode> parents = parentNode.isArray() ? List.copyOf(parentNode.values()) : List.of(parentNode);
        for (JsonNode parent : parents) {
            if (!parent.isObject()) {
                continue;
            }
            String parentType = scalarText(parent.get("type")).map(type -> type.toLowerCase(Locale.ROOT)).orElse("");

            StandardField targetField;
            if (JOURNAL_PARENT_TYPES.contains(parentType)) {
                targetField = StandardField.JOURNAL;
            } else if ("book".equals(parentType)) {
                targetField = BOOK_PART_TYPES.contains(bibEntry.getType()) ? StandardField.BOOKTITLE : StandardField.SERIES;
            } else if (BOOKTITLE_PARENT_TYPES.contains(parentType)) {
                targetField = StandardField.BOOKTITLE;
            } else {
                targetField = bibEntry.getType() == StandardEntryType.Article ? StandardField.JOURNAL : StandardField.BOOKTITLE;
            }

            formattedText(parent.get("title")).ifPresent(title -> setIfAbsent(bibEntry, targetField, title));
            if (targetField == StandardField.JOURNAL) {
                scalarText(parent.get("volume")).ifPresent(volume -> setIfAbsent(bibEntry, StandardField.VOLUME, volume));
                scalarText(parent.get("issue")).ifPresent(issue -> setIfAbsent(bibEntry, StandardField.NUMBER, issue));
                scalarText(parent.get("publisher")).ifPresent(publisher -> setIfAbsent(bibEntry, StandardField.PUBLISHER, publisher));
            }
        }
    }

    /// `affiliated` lists further people by their role (director, translator, ...). Roles with a
    /// matching BibTeX field (e.g. translator) land there; the rest are kept as custom fields
    /// named after the role, so no contributor information is lost on import.
    private void applyAffiliated(BibEntry bibEntry, @Nullable JsonNode affiliatedNode) {
        if (affiliatedNode == null || !affiliatedNode.isArray()) {
            return;
        }
        for (JsonNode affiliation : affiliatedNode.values()) {
            if (!affiliation.isObject()) {
                continue;
            }
            scalarText(affiliation.get("role")).ifPresent(role ->
                    persons(affiliation.get("names")).ifPresent(names ->
                            bibEntry.setField(FieldFactory.parseField(role.toLowerCase(Locale.ROOT)), names)));
        }
    }

    private static void setIfAbsent(BibEntry entry, StandardField field, String value) {
        if (!entry.hasField(field)) {
            entry.setField(field, value);
        }
    }

    /// Persons may be a single scalar, a list of scalars, a map, or a list of maps.
    private Optional<String> persons(@Nullable JsonNode node) {
        if (node == null) {
            return Optional.empty();
        }
        List<JsonNode> personNodes = node.isArray() ? List.copyOf(node.values()) : List.of(node);
        String formatted = personNodes.stream()
                                      .map(this::formatPerson)
                                      .flatMap(Optional::stream)
                                      .collect(Collectors.joining(" and "));
        return formatted.isEmpty() ? Optional.empty() : Optional.of(formatted);
    }

    /// Hayagriva person strings already use BibTeX's structured "Family, Given" form and are kept
    /// verbatim; the structured map form is converted to it.
    private Optional<String> formatPerson(JsonNode personNode) {
        if (personNode.isObject()) {
            // Structured form: `name` is the family name, next to optional `given-name`, `prefix`, `suffix`, `alias`
            return scalarText(personNode.get("name"))
                    .map(name -> scalarText(personNode.get("given-name"))
                            .map(givenName -> name + ", " + givenName)
                            .orElse(name));
        }
        return scalarText(personNode);
    }

    /// Returns the text of a scalar node (string, number, boolean); empty for containers and null.
    private static Optional<String> scalarText(@Nullable JsonNode node) {
        if (node == null || node.isNull() || node.isObject() || node.isArray()) {
            return Optional.empty();
        }
        String text = node.asString().trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    /// Reads a Hayagriva "formattable string": either a plain scalar or a map holding the actual
    /// string under `value` (next to `short`, `verbatim`, ... which JabRef cannot represent).
    private static Optional<String> formattedText(@Nullable JsonNode node) {
        if (node != null && node.isObject()) {
            return scalarText(node.get("value"));
        }
        return scalarText(node);
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
            if (entryNode.isObject() && scalarText(entryNode.get("type"))
                    .map(type -> RECOGNIZED_TYPES.contains(type.toLowerCase(Locale.ROOT)))
                    .orElse(false)) {
                return true;
            }
        }

        return false;
    }
}
