package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.openoffice.ZoteroCitationMarkParser.ParsedEntry;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Collects the entries cited in an OpenOffice/LibreOffice document, so that they can be imported into a library.
///
/// This is the counterpart of "Export cited", which looks the cited citation keys up in a library,
/// whereas "Import cited" reads the cited entries from the document itself.
/// Only Zotero-compatible reference marks carry the bibliographic data of the cited entries (as CSL JSON).
/// JabRef reference marks name the citation keys only and are therefore ignored.
@NullMarked
public class CitedEntryCollector {

    private enum IdentityType {
        CITATION_KEY,
        ZOTERO_ITEM
    }

    @NullMarked
    private record CitationIdentity(IdentityType type, String value) {
    }

    private CitedEntryCollector() {
    }

    /// Collects the entries cited by the Zotero-compatible reference marks of a document.
    ///
    /// Citations with the same identity are collected once, in first-citation order and with their first bibliographic data.
    /// Native Zotero URIs take precedence over explicit citation keys, which take precedence over item ids.
    /// Key-only citations stay separate from native Zotero items, even when their citation keys match.
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
            if (!ZoteroReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
                continue;
            }

            String cslJson = ZoteroReferenceMark.getCSLJson(referenceMarkName);
            for (ParsedEntry parsedEntry : ZoteroCitationMarkParser.parseCslCitationJsonForImportWithIdentity(cslJson)) {
                BibEntry entry = parsedEntry.entry();
                getCitationIdentity(parsedEntry).ifPresentOrElse(
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

    private static Optional<CitationIdentity> getCitationIdentity(ParsedEntry parsedEntry) {
        return parsedEntry.itemIdentity()
                          .filter(identity -> identity.startsWith("uri:"))
                          .map(uri -> new CitationIdentity(IdentityType.ZOTERO_ITEM, uri))
                          .or(() -> parsedEntry.entry().getCitationKey()
                                               .map(key -> new CitationIdentity(IdentityType.CITATION_KEY, key)))
                          .or(() -> parsedEntry.itemIdentity()
                                               .map(itemId -> new CitationIdentity(IdentityType.ZOTERO_ITEM, itemId)));
    }
}
