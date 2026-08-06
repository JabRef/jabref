package org.jabref.logic.openoffice.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.jabref.logic.JabRefException;
import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitedReference;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;

public class ExportCited {

    private ExportCited() {
    }

    public static class GenerateDatabaseResult {
        /// null: not done; isEmpty: no unresolved
        public final List<String> unresolvedKeys;
        public final BibDatabase newDatabase;

        GenerateDatabaseResult(List<String> unresolvedKeys, BibDatabase newDatabase) {
            this.unresolvedKeys = unresolvedKeys;
            this.newDatabase = newDatabase;
        }
    }

    /// @param databases The databases to look up the citation keys in the document from.
    /// @return A new database, with cloned entries.
    /// If a key is not found, it is added to result.unresolvedKeys
    ///
    /// Cross references (in StandardField.CROSSREF) are followed (not recursively): If the referenced entry is found, it is included in the result. If it is not found, it is silently ignored.
    public static OOResult<GenerateDatabaseResult, JabRefException> generateDatabase(XTextDocument doc, List<BibDatabase> databases) {
        try {
            OOFrontend frontend = new OOFrontend(doc);
            List<String> citationKeys = frontend.citationGroups.getCitedReferencesUnordered()
                                                               .values()
                                                               .stream()
                                                               .map(CitedReference::getCitationKey)
                                                               .toList();
            return OOResult.ok(generateDatabaseFromCitationKeys(citationKeys, databases));
        } catch (NoDocumentException | WrappedTargetException e) {
            return OOResult.error(new JabRefException(e.getMessage(), e));
        }
    }

    public static GenerateDatabaseResult generateDatabaseFromCitationKeys(List<String> citationKeys, List<BibDatabase> databases) {
        return generateDatabase(citationKeys, databases, ExportCited::lookupByCitationKey);
    }

    public static GenerateDatabaseResult generateDatabaseFromIdentifiers(List<String> identifiers, List<BibDatabase> databases) {
        return generateDatabase(identifiers, databases, ExportCited::lookupByIdentifier);
    }

    private static GenerateDatabaseResult generateDatabase(List<String> identifiers,
                                                           List<BibDatabase> databases,
                                                           BiFunction<String, List<BibDatabase>, Optional<LookupResult>> lookupFunction) {
        List<String> unresolvedKeys = new ArrayList<>();
        BibDatabase resultDatabase = new BibDatabase();

        List<BibEntry> entriesToInsert = new ArrayList<>();
        SequencedSet<String> seenIdentifiers = new LinkedHashSet<>(identifiers);
        Set<String> seenCrossReferences = new HashSet<>();

        for (String identifier : seenIdentifiers) {
            Optional<LookupResult> lookupResult = lookupFunction.apply(identifier, databases);
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
        return new GenerateDatabaseResult(unresolvedKeys, resultDatabase);
    }

    private static Optional<LookupResult> lookupByCitationKey(String citationKey, List<BibDatabase> databases) {
        return databases.stream()
                        .map(database -> database.getEntryByCitationKey(citationKey)
                                                 .map(entry -> new LookupResult(entry, database)))
                        .flatMap(Optional::stream)
                        .findFirst();
    }

    private static Optional<LookupResult> lookupByIdentifier(String identifier, List<BibDatabase> databases) {
        Optional<LookupResult> citationKeyLookup = lookupByCitationKey(identifier, databases);
        if (citationKeyLookup.isPresent()) {
            return citationKeyLookup;
        }

        return databases.stream()
                        .map(database -> database.getEntryById(identifier)
                                                 .map(entry -> new LookupResult(entry, database)))
                        .flatMap(Optional::stream)
                        .findFirst();
    }

    private record LookupResult(BibEntry entry, BibDatabase database) {
    }
}
