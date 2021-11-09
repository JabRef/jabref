package org.jabref.logic.openoffice.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitedKey;
import org.jabref.model.openoffice.style.CitedKeys;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;

public class ExportCited {

    private ExportCited() {
        /**/
    }

    public static class GenerateDatabaseResult {
        /**
         * null: not done; isEmpty: no unresolved
         */
        public final List<String> unresolvedKeys;
        public final BibDatabase newDatabase;

        GenerateDatabaseResult(List<String> unresolvedKeys, BibDatabase newDatabase) {
            this.unresolvedKeys = unresolvedKeys;
            this.newDatabase = newDatabase;
        }
    }

    /**
     *
     * @param databases The databases to look up the citation keys in the document from.
     * @return A new database, with cloned entries.
     *
     * If a key is not found, it is added to result.unresolvedKeys
     *
     * Cross references (in StandardField.CROSSREF) are followed (not recursively):
     * If the referenced entry is found, it is included in the result.
     * If it is not found, it is silently ignored.
     */
    public static GenerateDatabaseResult generateDatabase(XTextDocument doc, List<BibDatabase> databases)
        throws
        NoDocumentException,
        WrappedTargetException {

        OOFrontend frontend = new OOFrontend(doc);
        CitedKeys citationKeys = frontend.citationGroups.getCitedKeysUnordered();
        citationKeys.lookupInDatabases(databases);

        List<String> unresolvedKeys = new ArrayList<>();
        BibDatabase resultDatabase = new BibDatabase();

        List<BibEntry> entriesToInsert = new ArrayList<>();
        Set<String> seen = new HashSet<>(); // Only add crossReference once.

        for (CitedKey citation : citationKeys.values()) {
            if (citation.getLookupResult().isEmpty()) {
                unresolvedKeys.add(citation.citationKey);
                continue;
            } else {
                BibEntry entry = citation.getLookupResult().get().entry;
                BibDatabase loopDatabase = citation.getLookupResult().get().database;

                // If entry found
                BibEntry clonedEntry = (BibEntry) entry.clone();

                // Insert a copy of the entry
                entriesToInsert.add(clonedEntry);

                // Check if the cloned entry has a cross-reference field
                clonedEntry
                    .getField(StandardField.CROSSREF)
                    .ifPresent(crossReference -> {
                            boolean isNew = !seen.contains(crossReference);
                            if (isNew) {
                                // Add it if it is in the current library
                                loopDatabase
                                    .getEntryByCitationKey(crossReference)
                                    .ifPresent(entriesToInsert::add);
                                seen.add(crossReference);
                            }
                        });
            }
        }

        resultDatabase.insertEntries(entriesToInsert);
        return new GenerateDatabaseResult(unresolvedKeys, resultDatabase);
    }

}
