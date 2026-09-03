package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;

import org.jabref.logic.importer.fileformat.HayagrivaMapping;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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

    private static final YAMLMapper MAPPER = new YAMLMapper(YAMLFactory.builder()
                                                                       .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                                                                       .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                                                                       .build());

    public String serialize(SequencedMap<String, BibEntry> keyedEntries) {
        ObjectNode root = MAPPER.createObjectNode();
        keyedEntries.forEach((citationKey, entry) -> root.set(citationKey, toEntryNode(entry)));
        return MAPPER.writeValueAsString(root);
    }

    public ObjectNode toEntryNode(BibEntry entry) {
        return mergeIntoNode(entry, MAPPER.createObjectNode());
    }

    /// Merges the entry into an existing Hayagriva entry node (read-modify-write, see class doc).
    /// On an empty node this produces a fresh entry in canonical key order.
    public ObjectNode mergeIntoNode(BibEntry entry, ObjectNode node) {
        BibEntry current = HayagrivaMapping.toBibEntry("current", node);

        mergeType(node, entry);
        mergeFormattable(node, "title", StandardField.TITLE, entry, current);
        mergePersons(node, "author", StandardField.AUTHOR, entry, current);
        mergePersons(node, "editor", StandardField.EDITOR, entry, current);

        boolean hasJournal = entry.hasField(StandardField.JOURNAL);
        HayagrivaMapping.SCALAR_FIELDS.forEach((key, field) -> {
            if (hasJournal && JOURNAL_RELOCATED_KEYS.contains(key)) {
                node.remove(key);
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

    private void mergeType(ObjectNode node, BibEntry entry) {
        @Nullable EntryType currentType = HayagrivaMapping.scalarText(node.get("type"))
                                                          .map(type -> HayagrivaMapping.TYPE_TO_ENTRY_TYPE.getOrDefault(type.toLowerCase(Locale.ROOT), StandardEntryType.Misc))
                                                          .orElse(null);
        // An equivalent existing type string (e.g. `post`, importing as Misc) is kept as-is
        if (!entry.getType().equals(currentType)) {
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
        SequencedSet<String> keys = new LinkedHashSet<>();
        for (Map.Entry<String, JsonNode> property : node.properties()) {
            if (property.getKey().startsWith(HayagrivaMapping.USER_COMMENT_PREFIX)) {
                keys.add(property.getKey());
            }
        }
        entry.getFields().stream()
             .map(Field::getName)
             .filter(name -> name.startsWith(HayagrivaMapping.USER_COMMENT_PREFIX))
             .forEach(keys::add);
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
                                                        .anyMatch(field -> !entry.getField(field).equals(current.getField(field)));
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
        JsonNode serialNumber = node.get("serial-number");
        if (serialNumber instanceof ObjectNode serialNumberObject) {
            serialNumberObject.remove("serial");
            if (serialNumberObject.isEmpty()) {
                node.remove("serial-number");
            }
        } else if (serialNumber != null) {
            node.remove("serial-number");
        }
    }

    private Optional<String> arxivEprint(BibEntry entry) {
        boolean isArxiv = entry.getField(StandardField.EPRINTTYPE)
                               .map(eprintType -> "arxiv".equalsIgnoreCase(eprintType))
                               .orElse(false);
        return isArxiv ? entry.getField(StandardField.EPRINT) : Optional.empty();
    }

    private void mergeAffiliated(ObjectNode node, BibEntry entry, BibEntry current) {
        SequencedMap<String, JsonNode> existingByRole = new LinkedHashMap<>();
        if (node.get("affiliated") instanceof ArrayNode existing) {
            for (JsonNode item : existing.values()) {
                if (item.isObject()) {
                    HayagrivaMapping.scalarText(item.get("role"))
                                    .ifPresent(role -> existingByRole.putIfAbsent(role.toLowerCase(Locale.ROOT), item));
                }
            }
        }
        // Existing roles first, so untouched items keep their position; spec roles cover fields
        // added in JabRef
        LinkedHashSet<String> roles = new LinkedHashSet<>(existingByRole.keySet());
        roles.addAll(HayagrivaMapping.AFFILIATED_ROLES);

        boolean changed = false;
        List<JsonNode> result = new ArrayList<>();
        for (String role : roles) {
            Field field = FieldFactory.parseField(role);
            Optional<String> target = entry.getField(field);
            if (target.equals(current.getField(field))) {
                JsonNode item = existingByRole.get(role);
                if (item != null) {
                    result.add(item);
                }
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
        ArrayNode affiliated = MAPPER.createArrayNode();
        result.forEach(affiliated::add);
        node.set("affiliated", affiliated);
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
            ObjectNode parent = MAPPER.createObjectNode();
            parent.put("type", "periodical");
            parent.put("title", journal);
            entry.getField(StandardField.VOLUME).ifPresent(volume -> parent.put("volume", volume));
            entry.getField(StandardField.NUMBER).ifPresent(issue -> parent.put("issue", issue));
            entry.getField(StandardField.PUBLISHER).ifPresent(publisher -> parent.put("publisher", publisher));
            parents.add(parent);
        });
        entry.getField(StandardField.BOOKTITLE).ifPresent(booktitle -> {
            ObjectNode parent = MAPPER.createObjectNode();
            parent.put("type", booktitleParentType(entry.getType()));
            parent.put("title", booktitle);
            parents.add(parent);
        });
        // For book parts a `book` parent re-imports as booktitle, so a series written next to a
        // booktitle survives in the YAML but not a re-import (documented asymmetry)
        entry.getField(StandardField.SERIES).ifPresent(series -> {
            ObjectNode parent = MAPPER.createObjectNode();
            parent.put("type", "book");
            parent.put("title", series);
            parents.add(parent);
        });

        if (parents.isEmpty()) {
            node.remove("parent");
        } else if (parents.size() == 1) {
            node.set("parent", parents.getFirst());
        } else {
            ArrayNode parentArray = MAPPER.createArrayNode();
            parents.forEach(parentArray::add);
            node.set("parent", parentArray);
        }
    }

    private String booktitleParentType(EntryType entryType) {
        if (StandardEntryType.InProceedings == entryType) {
            return "proceedings";
        }
        if (StandardEntryType.InBook == entryType) {
            return "book";
        }
        return "anthology";
    }

    private boolean fieldDiffers(Field field, BibEntry entry, BibEntry current) {
        return !entry.getField(field).equals(current.getField(field));
    }
}
