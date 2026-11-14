package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;

/// Plans URI links without accessing or modifying a Writer document. The citation strategy applies
/// changed names only after the complete citation set has rendered successfully.
@NullMarked
public final class ZoteroCitationLinker {
    private ZoteroCitationLinker() {
    }

    public static LinkPlan planLinks(List<String> referenceMarkNames,
                                     BibDatabaseContext bibDatabaseContext,
                                     BibEntryTypesManager entryTypesManager) {
        List<LinkUpdate> updates = new ArrayList<>();
        Map<String, String> zoteroUris = new LinkedHashMap<>();
        for (String originalName : referenceMarkNames) {
            if (!ZoteroReferenceMark.isZoteroReferenceMarkName(originalName) || ZoteroReferenceMark.parse(originalName).isEmpty()) {
                continue;
            }
            String updatedName = linkReferenceMark(originalName, bibDatabaseContext, entryTypesManager);
            if (!updatedName.equals(originalName)) {
                updates.add(new LinkUpdate(originalName, updatedName, ZoteroReferenceMark.getCSLJson(updatedName)));
            }
            ZoteroReferenceMark.extractZoteroUriByCitationKey(updatedName).forEach(zoteroUris::putIfAbsent);
        }
        return new LinkPlan(updates, zoteroUris);
    }

    static String linkReferenceMark(String referenceMarkName,
                                    BibDatabaseContext bibDatabaseContext,
                                    BibEntryTypesManager entryTypesManager) {
        if (!ZoteroReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
            return referenceMarkName;
        }

        DuplicateCheck duplicateCheck = new DuplicateCheck(entryTypesManager);
        String updatedReferenceMarkName = referenceMarkName;
        List<ZoteroCitationData.CitationItemData> citationItems = ZoteroReferenceMark.getCitationItems(referenceMarkName);
        for (int i = 0; i < citationItems.size(); i++) {
            ZoteroCitationData.CitationItemData citationItem = citationItems.get(i);
            Optional<String> jabRefUri = createJabRefUriForZotero(citationItem, bibDatabaseContext, duplicateCheck);
            if (jabRefUri.isEmpty()) {
                continue;
            }
            updatedReferenceMarkName = ZoteroReferenceMark.addJabRefUri(updatedReferenceMarkName, i, jabRefUri.orElseThrow());
        }
        return updatedReferenceMarkName;
    }

    private static Optional<String> createJabRefUriForZotero(ZoteroCitationData.CitationItemData citationItem,
                                                             BibDatabaseContext bibDatabaseContext,
                                                             DuplicateCheck duplicateCheck) {
        List<String> uris = Optional.ofNullable(citationItem.uris).orElse(List.of());
        if (uris.stream().anyMatch(ZoteroReferenceMark::isJabRefUri) || uris.stream().noneMatch(ZoteroReferenceMark::isZoteroUri)) {
            return Optional.empty();
        }
        return Optional.ofNullable(citationItem.itemData).flatMap(_ -> ZoteroCitationMarkParser.toBibEntry(citationItem))
                       .flatMap(entry -> findDuplicate(entry, bibDatabaseContext, duplicateCheck))
                       .flatMap(BibEntry::getCitationKey)
                       .map(ZoteroReferenceMark::createJabRefUri);
    }

    private static Optional<BibEntry> findDuplicate(BibEntry entry, BibDatabaseContext context, DuplicateCheck duplicateCheck) {
        Optional<BibEntry> duplicate = Optional.empty();
        for (BibEntry candidate : context.getEntries()) {
            if (candidate.getCitationKey().isPresent() && duplicateCheck.isDuplicate(entry, candidate, context.getMode())) {
                if (duplicate.isPresent()) {
                    return Optional.empty();
                }
                duplicate = Optional.of(candidate);
            }
        }
        return duplicate;
    }

    @NullMarked
    public record LinkUpdate(String originalName, String updatedName, String citationJson) {
    }

    @NullMarked
    public record LinkPlan(List<LinkUpdate> updates, Map<String, String> zoteroUriByCitationKey) {
        public LinkPlan {
            updates = List.copyOf(updates);
            zoteroUriByCitationKey = Map.copyOf(zoteroUriByCitationKey);
        }

        public String updatedName(String originalName) {
            return updates.stream().filter(update -> update.originalName().equals(originalName))
                          .map(LinkUpdate::updatedName).findFirst().orElse(originalName);
        }
    }
}
