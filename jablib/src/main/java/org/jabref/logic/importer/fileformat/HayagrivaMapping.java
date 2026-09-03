package org.jabref.logic.importer.fileformat;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/// Field and type mapping between the Hayagriva YAML format
/// (<https://github.com/typst/hayagriva/blob/main/docs/file-format.md>) and [BibEntry], shared by
/// [HayagrivaImporter] and [org.jabref.logic.exporter.HayagrivaEntryWriter] so that reading and
/// writing stay symmetric.
@NullMarked
public final class HayagrivaMapping {

    public static final Map<String, EntryType> TYPE_TO_ENTRY_TYPE = Map.ofEntries(
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

    /// Inverse of [#TYPE_TO_ENTRY_TYPE], extended by BibTeX types whose closest Hayagriva type
    /// re-imports as a different (but equivalent) BibTeX type, e.g. TechReport -> `report` ->
    /// Report. Unlisted types are written as `misc`.
    public static final Map<EntryType, String> ENTRY_TYPE_TO_TYPE;

    public static final Set<String> JOURNAL_PARENT_TYPES = Set.of("periodical", "newspaper", "blog");
    public static final Set<String> BOOKTITLE_PARENT_TYPES = Set.of("proceedings", "anthology", "conference");
    public static final Set<EntryType> BOOK_PART_TYPES = Set.of(StandardEntryType.InBook, StandardEntryType.InCollection, StandardEntryType.InProceedings);

    public static final Field RUNTIME_FIELD = new UnknownField("runtime");
    public static final Field TIME_RANGE_FIELD = new UnknownField("time-range");

    /// Prefix of JabRef's per-user comment fields
    /// ([org.jabref.model.entry.field.UserSpecificCommentField]), mapped to equally named
    /// extension keys (see [#SCALAR_FIELDS] on extension keys).
    public static final String USER_COMMENT_PREFIX = "comment-";

    /// Hayagriva keys holding a plain scalar, with their BibEntry field, in canonical write
    /// order. `title` and `url` are "formattable strings" (scalar or map with a `value` key) and
    /// are handled separately, as are the person lists and `serial-number`.
    ///
    /// `comment` (like the per-user `comment-<name>` keys, see [#USER_COMMENT_PREFIX]) is not
    /// part of the Hayagriva specification but a JabRef extension key: the Hayagriva parser
    /// silently ignores keys it does not know, so such files stay loadable by Typst.
    public static final SequencedMap<String, Field> SCALAR_FIELDS;

    /// `serial-number` sub-keys with a dedicated BibEntry field. `arxiv` is handled separately
    /// because it involves the EPRINT/EPRINTTYPE field pair.
    public static final SequencedMap<String, Field> SERIAL_NUMBER_FIELDS;

    /// The roles enumerated by the Hayagriva specification. Both directions use the role name as
    /// the BibEntry field name; [#applyAffiliated] additionally accepts arbitrary role names.
    public static final List<String> AFFILIATED_ROLES = List.of(
            "translator", "afterword", "foreword", "introduction", "annotator", "commentator",
            "holder", "compiler", "founder", "collaborator", "organizer", "cast-member",
            "composer", "producer", "executive-producer", "writer", "cinematography", "director",
            "illustrator", "narrator");

    static {
        Map<EntryType, String> inverseTypes = new HashMap<>();
        TYPE_TO_ENTRY_TYPE.forEach((hayagrivaType, entryType) -> inverseTypes.put(entryType, hayagrivaType));
        inverseTypes.put(StandardEntryType.TechReport, "report");
        inverseTypes.put(StandardEntryType.MastersThesis, "thesis");
        inverseTypes.put(StandardEntryType.PhdThesis, "thesis");
        inverseTypes.put(StandardEntryType.WWW, "web");
        inverseTypes.put(StandardEntryType.Conference, "conference");
        ENTRY_TYPE_TO_TYPE = Map.copyOf(inverseTypes);

        SequencedMap<String, Field> scalars = new LinkedHashMap<>();
        scalars.put("date", StandardField.DATE);
        scalars.put("language", StandardField.LANGUAGE);
        scalars.put("publisher", StandardField.PUBLISHER);
        scalars.put("location", StandardField.ADDRESS);
        scalars.put("organization", StandardField.INSTITUTION);
        scalars.put("edition", StandardField.EDITION);
        scalars.put("volume", StandardField.VOLUME);
        scalars.put("volume-total", StandardField.VOLUMES);
        scalars.put("issue", StandardField.NUMBER);
        scalars.put("chapter", StandardField.CHAPTER);
        scalars.put("page-range", StandardField.PAGES);
        scalars.put("page-total", StandardField.PAGETOTAL);
        scalars.put("runtime", RUNTIME_FIELD);
        scalars.put("time-range", TIME_RANGE_FIELD);
        scalars.put("note", StandardField.NOTE);
        scalars.put("abstract", StandardField.ABSTRACT);
        scalars.put("comment", StandardField.COMMENT);
        SCALAR_FIELDS = Collections.unmodifiableSequencedMap(scalars);

        SequencedMap<String, Field> serialNumbers = new LinkedHashMap<>();
        serialNumbers.put("doi", StandardField.DOI);
        serialNumbers.put("isbn", StandardField.ISBN);
        serialNumbers.put("issn", StandardField.ISSN);
        serialNumbers.put("pmid", StandardField.PMID);
        SERIAL_NUMBER_FIELDS = Collections.unmodifiableSequencedMap(serialNumbers);
    }

    private HayagrivaMapping() {
    }

    public static BibEntry toBibEntry(String citationKey, JsonNode entryNode) {
        EntryType entryType = scalarText(entryNode.get("type"))
                .map(type -> TYPE_TO_ENTRY_TYPE.getOrDefault(type.toLowerCase(Locale.ROOT), StandardEntryType.Misc))
                .orElse(StandardEntryType.Misc);
        BibEntry bibEntry = new BibEntry(entryType);
        bibEntry.setCitationKey(citationKey);

        formattedText(entryNode.get("title")).ifPresent(title -> bibEntry.setField(StandardField.TITLE, title));
        formattedText(entryNode.get("url")).ifPresent(url -> bibEntry.setField(StandardField.URL, url));
        persons(entryNode.get("author")).ifPresent(authors -> bibEntry.setField(StandardField.AUTHOR, authors));
        persons(entryNode.get("editor")).ifPresent(editors -> bibEntry.setField(StandardField.EDITOR, editors));

        // A leading `~` (Hayagriva's approximate-date marker) is kept in `date`: JabRef stores
        // the raw string even when it cannot parse it into a structured date, so nothing is lost.
        SCALAR_FIELDS.forEach((key, field) ->
                scalarText(entryNode.get(key)).ifPresent(value -> bibEntry.setField(field, value)));

        applyUserComments(bibEntry, entryNode);
        applyAffiliated(bibEntry, entryNode.get("affiliated"));
        applySerialNumber(bibEntry, entryNode.get("serial-number"));
        applyParents(bibEntry, entryNode.get("parent"));

        return bibEntry;
    }

    private static void applySerialNumber(BibEntry bibEntry, @Nullable JsonNode serialNumber) {
        if (serialNumber != null && serialNumber.isObject()) {
            SERIAL_NUMBER_FIELDS.forEach((key, field) ->
                    scalarText(serialNumber.get(key)).ifPresent(value -> bibEntry.setField(field, value)));
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
    }

    /// A Hayagriva `parent` describes what an entry is contained in and has no direct BibTeX
    /// equivalent. The parent's `type` picks the target field, mirroring what the writer emits
    /// (journal as a `periodical` parent, series as a `book` parent). A missing or unmapped
    /// parent type falls back by entry type: articles are most likely contained in a journal,
    /// everything else in some book-like container. The first parent providing a value for a
    /// field wins; nested parents are not descended into.
    static void applyParents(BibEntry bibEntry, @Nullable JsonNode parentNode) {
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

    /// Per-user comment extension keys (`comment-<name>`, see [#SCALAR_FIELDS]) map to the
    /// equally named [org.jabref.model.entry.field.UserSpecificCommentField]s.
    static void applyUserComments(BibEntry bibEntry, JsonNode entryNode) {
        for (Map.Entry<String, JsonNode> property : entryNode.properties()) {
            if (property.getKey().startsWith(USER_COMMENT_PREFIX)) {
                scalarText(property.getValue()).ifPresent(value ->
                        bibEntry.setField(FieldFactory.parseField(property.getKey()), value));
            }
        }
    }

    /// `affiliated` lists further people by their role (director, translator, ...). Roles with a
    /// matching BibTeX field (e.g. translator) land there; the rest are kept as custom fields
    /// named after the role, so no contributor information is lost on import.
    static void applyAffiliated(BibEntry bibEntry, @Nullable JsonNode affiliatedNode) {
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
    private static Optional<String> persons(@Nullable JsonNode node) {
        if (node == null) {
            return Optional.empty();
        }
        List<JsonNode> personNodes = node.isArray() ? List.copyOf(node.values()) : List.of(node);
        String formatted = personNodes.stream()
                                      .map(HayagrivaMapping::formatPerson)
                                      .flatMap(Optional::stream)
                                      .collect(Collectors.joining(" and "));
        return formatted.isEmpty() ? Optional.empty() : Optional.of(formatted);
    }

    /// Hayagriva person strings already use BibTeX's structured "Family, Given" form and are kept
    /// verbatim; the structured map form is converted to it.
    private static Optional<String> formatPerson(JsonNode personNode) {
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
    public static Optional<String> scalarText(@Nullable JsonNode node) {
        if (node == null || node.isNull() || node.isObject() || node.isArray()) {
            return Optional.empty();
        }
        String text = node.asString().trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    /// Reads a Hayagriva "formattable string": either a plain scalar or a map holding the actual
    /// string under `value` (next to `short`, `verbatim`, ... which JabRef cannot represent).
    public static Optional<String> formattedText(@Nullable JsonNode node) {
        if (node != null && node.isObject()) {
            return scalarText(node.get("value"));
        }
        return scalarText(node);
    }
}
