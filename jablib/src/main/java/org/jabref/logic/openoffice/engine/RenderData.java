package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationLookupResult;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

/// One action's key resolution. Database precedence is the request's order; entry ids are never keys.
@NullMarked
final class RenderData {

    private final Map<String, Optional<CitationLookupResult>> entries = new LinkedHashMap<>();

    RenderData(RenderRequest request) throws RenderException {
        SequencedSet<CitationGroupId> ids = new LinkedHashSet<>();
        for (CitationGroup group : request.groupsInDocumentOrder()) {
            if (!ids.add(group.groupId) || group.citationsInStorageOrder.isEmpty()) {
                throw new RenderException("Citation groups must have distinct ids and contain at least one citation");
            }
            for (Citation citation : group.citationsInStorageOrder) {
                String key = citation.citationKey;
                if (key.isBlank()) {
                    throw new RenderException("A citation requires a stable nonempty citation key");
                }
                entries.computeIfAbsent(key, _ -> Citation.lookup(request.databases(), key));
            }
        }
    }

    Optional<CitationLookupResult> lookup(String key) {
        return entries.getOrDefault(key, Optional.empty());
    }

    CitationLookupResult resolved(String key) throws RenderException {
        return lookup(key).orElseThrow(() -> new RenderException("Citation key is unresolved: " + key));
    }

    List<String> resolvedKeys() {
        return entries.entrySet().stream().filter(entry -> entry.getValue().isPresent()).map(Map.Entry::getKey).toList();
    }

    List<String> unresolvedKeys() {
        return entries.entrySet().stream().filter(entry -> entry.getValue().isEmpty()).map(Map.Entry::getKey).toList();
    }

    /// Resolve against each entry's own library before combining libraries, including inherited fields
    /// and strings. This avoids losing crossrefs or resolving a string using another library's value.
    List<BibEntry> flattenedEntries() throws RenderException {
        List<BibEntry> result = new ArrayList<>();
        for (String key : resolvedKeys()) {
            CitationLookupResult source = resolved(key);
            BibEntry copy = new BibEntry(source.entry).withCitationKey(key);
            SequencedSet<Field> fields = new LinkedHashSet<>(List.of(StandardField.values()));
            fields.addAll(source.entry.getFields());
            for (Field field : fields) {
                source.entry.getResolvedFieldOrAlias(field, source.database).ifPresent(value -> copy.setField(field, value));
            }
            result.add(copy);
        }
        return result;
    }

    static List<BibDatabase> copyDatabases(List<BibDatabase> databases) {
        return databases.stream().map(database -> {
            BibDatabase copy = new BibDatabase(database.getEntries().stream().map(BibEntry::new).toList());
            copy.setStrings(List.copyOf(database.getStringValues()));
            copy.copyPreamble(database);
            return copy;
        }).toList();
    }

    static String unresolved(String key) {
        return StringEscapeUtils.escapeHtml4("Unresolved(" + key + ")");
    }
}
