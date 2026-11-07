package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class CitedEntryExporter {

    private CitedEntryExporter() {
    }

    public record ExportResult(List<String> unresolvedKeys, BibDatabase database) {
        public ExportResult {
            unresolvedKeys = List.copyOf(unresolvedKeys);
        }
    }

    /// @param databases The databases to look up the citation keys in.
    /// @return A new database containing clones of explicitly cited entries.
    /// If a key is not found, it is added to `ExportResult.unresolvedKeys`.
    ///
    /// Cross references (in `StandardField.CROSSREF`) are followed once within the entry's library.
    /// Referenced entries are included without cloning; missing cross references are silently ignored.
    public static ExportResult export(List<String> citationKeys, List<BibDatabase> databases) {
        List<String> unresolvedKeys = new ArrayList<>();
        BibDatabase resultDatabase = new BibDatabase();

        List<BibEntry> entriesToInsert = new ArrayList<>();
        SequencedSet<String> seenIdentifiers = new LinkedHashSet<>(citationKeys);
        Set<String> seenCrossReferences = new HashSet<>();

        for (String identifier : seenIdentifiers) {
            Optional<LookupResult> lookupResult = lookupByCitationKey(identifier, databases);
            if (lookupResult.isEmpty()) {
                unresolvedKeys.add(identifier);
                continue;
            }

            BibEntry entry = lookupResult.get().entry();
            BibDatabase loopDatabase = lookupResult.get().database();

            BibEntry clonedEntry = new BibEntry(entry);
            entriesToInsert.add(clonedEntry);

            clonedEntry.getField(StandardField.CROSSREF)
                       .ifPresent(crossReference -> {
                           boolean isNew = seenCrossReferences.add(crossReference);
                           if (isNew) {
                               loopDatabase.getEntryByCitationKey(crossReference)
                                           .ifPresent(entriesToInsert::add);
                           }
                       });
        }

        resultDatabase.insertEntries(entriesToInsert);
        return new ExportResult(unresolvedKeys, resultDatabase);
    }

    private static Optional<LookupResult> lookupByCitationKey(String citationKey, List<BibDatabase> databases) {
        return databases.stream()
                        .map(database -> database.getEntryByCitationKey(citationKey)
                                                 .map(entry -> new LookupResult(entry, database)))
                        .flatMap(Optional::stream)
                        .findFirst();
    }

    private record LookupResult(BibEntry entry, BibDatabase database) {
    }
}
