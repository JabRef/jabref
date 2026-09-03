package org.jabref.logic.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.importer.fileformat.HayagrivaMapping;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/// Serializes [BibEntry]s to Hayagriva YAML, symmetric to
/// [org.jabref.logic.importer.fileformat.HayagrivaImporter] via the shared [HayagrivaMapping].
///
/// The core operation is a read-modify-write merge: [#mergeIntoNode] diffs the entry against
/// what the given YAML node currently yields on import and only rewrites the YAML paths whose
/// value actually changed. Everything JabRef does not own survives a rewrite — structured titles
/// (`{value, short}` keeps its `short`), person details (`prefix`/`suffix`/`alias`), additional
/// `serial-number` schemes, and entry fields unknown to JabRef. Known losses: YAML `#` comments
/// (dropped by the parser) and details of a `parent` node once one of its source fields
/// (journal/series/booktitle) changes, because parents are then re-synthesized from the entry.
@NullMarked
public class HayagrivaEntryWriter {

    /// When the entry has a journal, these keys describe the journal and belong inside the
    /// synthesized `periodical` parent instead of the entry itself.
    private static final Set<String> JOURNAL_RELOCATED_KEYS = Set.of("volume", "issue", "publisher");

    /// Number-looking strings (`edition: 1.10`, `volume: 1e3`) must stay quoted, otherwise the
    /// YAML reader resolves them as numbers and the text changes (`1.10` -> `1.1`).
    private static final YAMLMapper MAPPER = new YAMLMapper(YAMLFactory.builder()
                                                                       .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                                                                       .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                                                                       .enable(YAMLWriteFeature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
                                                                       .build());

    /// One entry of a directory-library sidecar during write-back: `previousKey` locates the
    /// entry's existing YAML node (empty or unknown for new entries), `targetKey` is the key to
    /// write (differs from `previousKey` after a citation-key edit).
    public record KeyedEntry(String previousKey, String targetKey, BibEntry entry) {
    }

    /// Merges the given entries into an existing Hayagriva document (read-modify-write, see
    /// class doc). The result contains exactly the given entries in the given order — top-level
    /// keys without a corresponding entry are dropped, because their entries no longer belong
    /// to this file — while inside each kept entry everything JabRef does not own survives.
    ///
    /// @param existingDocument the document's current text, empty for a new file
    /// @throws IOException if the existing document is not a YAML map (it is then not
    ///                     overwritten: the user may be mid-edit)
    public String mergeIntoDocument(String existingDocument, List<KeyedEntry> entries) throws IOException {
        ObjectNode existingRoot = existingDocument.isBlank() ? MAPPER.createObjectNode() : parseRoot(existingDocument);
        ObjectNode result = MAPPER.createObjectNode();
        for (KeyedEntry keyedEntry : entries) {
            ObjectNode entryNode = existingRoot.get(keyedEntry.previousKey()) instanceof ObjectNode existing
                                   ? existing
                                   : MAPPER.createObjectNode();
            result.set(keyedEntry.targetKey(), mergeIntoNode(keyedEntry.entry(), entryNode));
        }
        return MAPPER.writeValueAsString(result);
    }

    private static ObjectNode parseRoot(String document) throws IOException {
        try {
            if (MAPPER.readTree(document) instanceof ObjectNode root) {
                return root;
            }
        } catch (JacksonException e) {
            throw new IOException("Existing Hayagriva document is not valid YAML", e);
        }
        throw new IOException("Existing Hayagriva document is not a map of entries");
    }

    public String serialize(SequencedMap<String, BibEntry> keyedEntries) {
        ObjectNode root = MAPPER.createObjectNode();
        keyedEntries.forEach((citationKey, entry) -> root.set(citationKey, toEntryNode(entry)));
        return MAPPER.writeValueAsString(root);
    }

    ObjectNode toEntryNode(BibEntry entry) {
        return mergeIntoNode(entry, MAPPER.createObjectNode());
    }

    /// Merges the entry into an existing Hayagriva entry node (read-modify-write, see class doc).
    /// On an empty node this produces a fresh entry in canonical key order.
    ObjectNode mergeIntoNode(BibEntry entry, ObjectNode node) {
        BibEntry current = HayagrivaMapping.toBibEntry("current", node);

        mergeType(node, entry);
        mergeFormattable(node, "title", StandardField.TITLE, entry, current);
        mergePersons(node, "author", StandardField.AUTHOR, entry, current);
        mergePersons(node, "editor", StandardField.EDITOR, entry, current);

        boolean hasJournal = entry.hasField(StandardField.JOURNAL);
        boolean journalRemoved = !hasJournal && current.hasField(StandardField.JOURNAL);
        HayagrivaMapping.SCALAR_FIELDS.forEach((key, field) -> {
            if (!JOURNAL_RELOCATED_KEYS.contains(key)) {
                mergeScalar(node, key, field, entry, current);
            } else if (hasJournal) {
                // Journal details live in the periodical parent (see mergeParent). A top-level
                // value wins on import, so an unchanged one is left where it is — removing it
                // would silently surface the parent's (different) value.
                if (fieldDiffers(field, entry, current)) {
                    node.remove(key);
                }
            } else if (journalRemoved) {
                // The parent goes away with the journal, so the details it carried must
                // resurface at the top level even though their value did not change
                scalarValue(entry, field).ifPresentOrElse(value -> node.put(key, value), () -> node.remove(key));
            } else {
                mergeScalar(node, key, field, entry, current);
            }
        });

        mergeUserComments(node, entry, current);
        mergeFormattable(node, "url", StandardField.URL, entry, current);
        mergeSerialNumber(node, entry, current);
        removeStaleSerialFallback(node, entry, current);
        mergeAffiliated(node, entry, current);
        mergeParent(node, entry, current);
        return node;
    }

    /// An equivalent existing type string (e.g. `post`, importing as Misc) is kept as-is.
    private void mergeType(ObjectNode node, BibEntry entry) {
        boolean typeUnchanged = HayagrivaMapping.scalarText(node.get("type"))
                                                .map(type -> HayagrivaMapping.TYPE_TO_ENTRY_TYPE.getOrDefault(type.toLowerCase(Locale.ROOT), StandardEntryType.Misc))
                                                .filter(entry.getType()::equals)
                                                .isPresent();
        if (!typeUnchanged) {
            node.put("type", HayagrivaMapping.ENTRY_TYPE_TO_TYPE.getOrDefault(entry.getType(), "misc"));
        }
    }

    private void mergeScalar(ObjectNode node, String key, Field field, BibEntry entry, BibEntry current) {
        Optional<String> target = scalarValue(entry, field);
        if (target.equals(scalarValue(current, field))) {
            return;
        }
        target.ifPresentOrElse(value -> node.put(key, value), () -> node.remove(key));
    }

    /// `date` is resolved through the field aliases, so an entry carrying only the BibTeX
    /// YEAR/MONTH/DAY fields still gets its `date` written (normalized, e.g. `2020-05`).
    private Optional<String> scalarValue(BibEntry entry, Field field) {
        return field == StandardField.DATE ? entry.getFieldOrAlias(field) : entry.getField(field);
    }

    /// JabRef's per-user comment fields are written as equally named `comment-<name>` extension
    /// keys, symmetric to [HayagrivaMapping#applyUserComments].
    private void mergeUserComments(ObjectNode node, BibEntry entry, BibEntry current) {
        List<String> keys = Stream.concat(node.propertyNames().stream(), entry.getFields().stream().map(Field::getName))
                                  .filter(name -> name.startsWith(HayagrivaMapping.USER_COMMENT_PREFIX))
                                  .distinct()
                                  .toList();
        keys.forEach(key -> mergeScalar(node, key, FieldFactory.parseField(key), entry, current));
    }

    private void mergeFormattable(ObjectNode node, String key, Field field, BibEntry entry, BibEntry current) {
        Optional<String> target = entry.getField(field);
        if (target.equals(current.getField(field))) {
            return;
        }
        target.ifPresentOrElse(value -> {
            if (node.get(key) instanceof ObjectNode structured) {
                structured.put("value", value);
            } else {
                node.put(key, value);
            }
        }, () -> node.remove(key));
    }

    private void mergePersons(ObjectNode node, String key, Field field, BibEntry entry, BibEntry current) {
        Optional<String> target = entry.getField(field);
        if (target.equals(current.getField(field))) {
            return;
        }
        target.ifPresentOrElse(value -> node.set(key, personList(value)), () -> node.remove(key));
    }

    private JsonNode personList(String bibtexPersons) {
        ArrayNode names = MAPPER.createArrayNode();
        AuthorList.parse(bibtexPersons).getAuthors()
                  .forEach(author -> names.add(author.getFamilyGiven(false)));
        return names;
    }

    private void mergeSerialNumber(ObjectNode node, BibEntry entry, BibEntry current) {
        boolean changed = !arxivEprint(entry).equals(arxivEprint(current))
                || HayagrivaMapping.SERIAL_NUMBER_FIELDS.values().stream()
                                                        .anyMatch(field -> fieldDiffers(field, entry, current));
        if (!changed) {
            return;
        }

        JsonNode existing = node.get("serial-number");
        ObjectNode serialNumber;
        if (existing instanceof ObjectNode existingObject) {
            serialNumber = existingObject;
        } else {
            serialNumber = MAPPER.createObjectNode();
            // A bare serial number is preserved under its explicit key when the node has to
            // become a map to hold further identifiers
            HayagrivaMapping.scalarText(existing).ifPresent(bare -> serialNumber.put("serial", bare));
        }

        HayagrivaMapping.SERIAL_NUMBER_FIELDS.forEach((key, field) ->
                entry.getField(field).ifPresentOrElse(value -> serialNumber.put(key, value), () -> serialNumber.remove(key)));
        arxivEprint(entry).ifPresentOrElse(value -> serialNumber.put("arxiv", value), () -> serialNumber.remove("arxiv"));

        if (serialNumber.isEmpty()) {
            node.remove("serial-number");
        } else {
            node.set("serial-number", serialNumber);
        }
    }

    /// A cleared NUMBER must also clear the `serial-number` fallbacks it may have been imported
    /// from, otherwise the deletion would be undone on the next import.
    private void removeStaleSerialFallback(ObjectNode node, BibEntry entry, BibEntry current) {
        if (entry.hasField(StandardField.NUMBER) || current.getField(StandardField.NUMBER).isEmpty()) {
            return;
        }
        node.optional("serial-number").ifPresent(serialNumber -> {
            if (serialNumber instanceof ObjectNode serialNumberObject) {
                serialNumberObject.remove("serial");
                if (serialNumberObject.isEmpty()) {
                    node.remove("serial-number");
                }
            } else {
                node.remove("serial-number");
            }
        });
    }

    private Optional<String> arxivEprint(BibEntry entry) {
        return entry.getField(StandardField.EPRINTTYPE)
                    .filter("arxiv"::equalsIgnoreCase)
                    .flatMap(_ -> entry.getField(StandardField.EPRINT));
    }

    private void mergeAffiliated(ObjectNode node, BibEntry entry, BibEntry current) {
        // Last item per role wins, like on import
        SequencedMap<String, JsonNode> existingByRole = new LinkedHashMap<>();
        if (node.get("affiliated") instanceof ArrayNode existing) {
            for (JsonNode item : existing.values()) {
                if (item.isObject()) {
                    HayagrivaMapping.scalarText(item.get("role"))
                                    .ifPresent(role -> existingByRole.put(role.toLowerCase(Locale.ROOT), item));
                }
            }
        }
        // Existing roles first, so untouched items keep their position; spec roles cover fields
        // added in JabRef
        SequencedSet<String> roles = new LinkedHashSet<>(existingByRole.keySet());
        roles.addAll(HayagrivaMapping.AFFILIATED_ROLES);

        boolean changed = false;
        List<JsonNode> result = new ArrayList<>();
        for (String role : roles) {
            Field field = FieldFactory.parseField(role);
            Optional<String> target = entry.getField(field);
            if (target.equals(current.getField(field))) {
                Optional.ofNullable(existingByRole.get(role)).ifPresent(result::add);
                continue;
            }
            changed = true;
            target.ifPresent(value -> {
                ObjectNode item = MAPPER.createObjectNode();
                item.put("role", role);
                item.set("names", personList(value));
                result.add(item);
            });
        }

        if (!changed) {
            return;
        }
        if (result.isEmpty()) {
            node.remove("affiliated");
            return;
        }
        node.set("affiliated", MAPPER.createArrayNode().addAll(result));
    }

    private void mergeParent(ObjectNode node, BibEntry entry, BibEntry current) {
        boolean journalDetailsChanged = entry.hasField(StandardField.JOURNAL)
                && (fieldDiffers(StandardField.VOLUME, entry, current)
                || fieldDiffers(StandardField.NUMBER, entry, current)
                || fieldDiffers(StandardField.PUBLISHER, entry, current));
        boolean changed = fieldDiffers(StandardField.JOURNAL, entry, current)
                || fieldDiffers(StandardField.SERIES, entry, current)
                || fieldDiffers(StandardField.BOOKTITLE, entry, current)
                || journalDetailsChanged;
        if (!changed) {
            return;
        }

        List<ObjectNode> parents = new ArrayList<>();
        entry.getField(StandardField.JOURNAL).ifPresent(journal -> {
            ObjectNode parent = parentNode("periodical", journal);
            entry.getField(StandardField.VOLUME).ifPresent(volume -> parent.put("volume", volume));
            entry.getField(StandardField.NUMBER).ifPresent(issue -> parent.put("issue", issue));
            entry.getField(StandardField.PUBLISHER).ifPresent(publisher -> parent.put("publisher", publisher));
            parents.add(parent);
        });
        Optional<ObjectNode> seriesParent = entry.getField(StandardField.SERIES).map(series -> parentNode("book", series));
        entry.getField(StandardField.BOOKTITLE).ifPresentOrElse(booktitle -> {
            ObjectNode parent = parentNode(booktitleParentType(entry.getType()), booktitle);
            // Hayagriva nests containers: the series is the parent of the proceedings/book the
            // part appears in, which is also what makes both fields survive a re-import
            seriesParent.ifPresent(series -> parent.set("parent", series));
            parents.add(parent);
        }, () -> seriesParent.ifPresent(parents::add));

        if (parents.isEmpty()) {
            node.remove("parent");
        } else if (parents.size() == 1) {
            node.set("parent", parents.getFirst());
        } else {
            node.set("parent", MAPPER.createArrayNode().addAll(parents));
        }
    }

    private ObjectNode parentNode(String type, String title) {
        ObjectNode parent = MAPPER.createObjectNode();
        parent.put("type", type);
        parent.put("title", title);
        return parent;
    }

    private String booktitleParentType(EntryType entryType) {
        return switch (entryType) {
            case StandardEntryType.InProceedings -> "proceedings";
            case StandardEntryType.InBook -> "book";
            default -> "anthology";
        };
    }

    private boolean fieldDiffers(Field field, BibEntry entry, BibEntry current) {
        return !entry.getField(field).equals(current.getField(field));
    }
}
