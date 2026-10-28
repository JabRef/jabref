package org.jabref.logic.openoffice.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.logic.openoffice.ZoteroCitationMarkParser;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Collects the entries cited in an OpenOffice/LibreOffice document, so that they can be imported into a library.
///
/// This is the counterpart of [ExportCited]: "Export cited" looks the cited citation keys up in a library,
/// whereas "Import cited" reads the cited entries from the document itself.
/// Only Zotero-compatible reference marks carry the bibliographic data of the cited entries (as CSL JSON).
/// JabRef reference marks name the citation keys only and are therefore ignored.
@NullMarked
public class ImportCited {

    private enum IdentityType {
        CITATION_KEY,
        ZOTERO_ITEM
    }

    private record CitationIdentity(IdentityType type, String value) {
    }

    private ImportCited() {
    }

    /// @param newEntries              the cited entries that are to be imported, in the order of their first citation
    /// @param entriesAlreadyInLibrary the cited entries whose citation key the library already contains
    public record CitedEntries(List<BibEntry> newEntries, List<BibEntry> entriesAlreadyInLibrary) {
    }

    /// Collects the entries cited by the Zotero-compatible reference marks of a document.
    ///
    /// An entry cited more than once is collected once.
    /// An entry keeps the citation key that its reference mark names (the key in its JabRef URI or its `citation-key`).
    /// Entries that only Zotero knows stay without a citation key, so that the library generates one.
    ///
    /// @param referenceMarkNames the names of all reference marks in the document
    /// @param targetDatabase     the library the entries are to be imported into
    // [impl->feat~import.libreoffice.cited-entries~1]
    public static CitedEntries collectCitedEntries(List<String> referenceMarkNames, BibDatabase targetDatabase) {
        List<BibEntry> citedEntries = new ArrayList<>();
        Set<CitationIdentity> citedIdentities = new HashSet<>();
        for (String referenceMarkName : referenceMarkNames) {
            if (!ReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
                continue;
            }

            String cslJson = ZoteroReferenceMark.getCSLJson(referenceMarkName);
            for (ZoteroCitationMarkParser.ParsedEntry parsedEntry : ZoteroCitationMarkParser.parseCslCitationJsonForImportWithIdentity(cslJson)) {
                BibEntry entry = parsedEntry.entry();
                Optional<CitationIdentity> identity = entry.getCitationKey()
                                                           .map(key -> new CitationIdentity(IdentityType.CITATION_KEY, key))
                                                           .or(() -> parsedEntry.itemIdentity()
                                                                                .map(itemId -> new CitationIdentity(IdentityType.ZOTERO_ITEM, itemId)));
                identity.ifPresentOrElse(
                        citationIdentity -> {
                            if (citedIdentities.add(citationIdentity)) {
                                citedEntries.add(entry);
                            }
                        },
                        () -> citedEntries.add(entry));
            }
        }

        List<BibEntry> newEntries = citedEntries.stream()
                                                .filter(entry -> !isInLibrary(entry, targetDatabase))
                                                .toList();
        List<BibEntry> entriesAlreadyInLibrary = citedEntries.stream()
                                                             .filter(entry -> isInLibrary(entry, targetDatabase))
                                                             .toList();
        return new CitedEntries(newEntries, entriesAlreadyInLibrary);
    }

    private static boolean isInLibrary(BibEntry entry, BibDatabase targetDatabase) {
        return entry.getCitationKey()
                    .flatMap(targetDatabase::getEntryByCitationKey)
                    .isPresent();
    }
}
