package org.jabref.logic.openoffice.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.openoffice.ReferenceMark;
import org.jabref.logic.openoffice.ZoteroCitationMarkParser;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
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

    @NullMarked
    private record CitationIdentity(IdentityType type, String value) {
    }

    private ImportCited() {
    }

    /// Collects the entries cited by the Zotero-compatible reference marks of a document.
    ///
    /// An entry cited more than once is collected once, in first-citation order and with its first bibliographic data.
    /// The Zotero item identity takes precedence over the explicit citation key when deduplicating document citations.
    /// An entry keeps the citation key that its reference mark names (the key in its JabRef URI or its `citation-key`),
    /// including a key supplied by a later citation of the same item if the first citation was keyless.
    /// Entries that only Zotero knows stay without a citation key, so that the library generates one.
    /// Bibliographic duplicates in the target library are left to the import duplicate handler.
    ///
    /// @param referenceMarkNames the names of all reference marks in the document
    // [impl->feat~import.libreoffice.cited-entries~1]
    public static List<BibEntry> collectCitedEntries(List<String> referenceMarkNames) {
        List<BibEntry> citedEntries = new ArrayList<>();
        Map<CitationIdentity, BibEntry> citedEntriesByIdentity = new HashMap<>();
        for (String referenceMarkName : referenceMarkNames) {
            if (!ReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
                continue;
            }

            String cslJson = ZoteroReferenceMark.getCSLJson(referenceMarkName);
            for (ZoteroCitationMarkParser.ParsedEntry parsedEntry : ZoteroCitationMarkParser.parseCslCitationJsonForImportWithIdentity(cslJson)) {
                BibEntry entry = parsedEntry.entry();
                Optional<CitationIdentity> identity = parsedEntry.itemIdentity()
                                                                 .map(itemId -> new CitationIdentity(IdentityType.ZOTERO_ITEM, itemId))
                                                                 .or(() -> entry.getCitationKey()
                                                                                .map(key -> new CitationIdentity(IdentityType.CITATION_KEY, key)));
                identity.ifPresentOrElse(
                        citationIdentity -> {
                            Optional<BibEntry> collectedEntry = Optional.ofNullable(citedEntriesByIdentity.putIfAbsent(citationIdentity, entry));
                            collectedEntry.ifPresentOrElse(
                                    firstEntry -> {
                                        if (firstEntry.getCitationKey().isEmpty()) {
                                            entry.getCitationKey().ifPresent(firstEntry::withCitationKey);
                                        }
                                    },
                                    () -> citedEntries.add(entry));
                        },
                        () -> citedEntries.add(entry));
            }
        }

        return citedEntries;
    }
}
