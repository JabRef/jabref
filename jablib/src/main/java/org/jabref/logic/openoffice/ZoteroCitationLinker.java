package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.DocumentAnnotation;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;

import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ZoteroCitationLinker {
    private ZoteroCitationLinker() {
    }

    public static int linkZoteroCitations(XTextDocument document,
                                          BibDatabaseContext bibDatabaseContext,
                                          BibEntryTypesManager entryTypesManager)
            throws
            NoDocumentException,
            CreationException,
            com.sun.star.uno.Exception {
        int updatedMarks = 0;
        for (String referenceMarkName : UnoReferenceMark.getListOfNames(document)) {
            String updatedName = linkReferenceMark(referenceMarkName, bibDatabaseContext, entryTypesManager);
            if (!updatedName.equals(referenceMarkName)) {
                updateReferenceMarkName(document, referenceMarkName, updatedName);
                updatedMarks++;
            }
        }
        return updatedMarks;
    }

    static String linkReferenceMark(String referenceMarkName,
                                    BibDatabaseContext bibDatabaseContext,
                                    BibEntryTypesManager entryTypesManager) {
        if (!ReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
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

            if (ZoteroReferenceMark.isZoteroUri(citationItem.uris.getFirst())) {
                updatedReferenceMarkName = ZoteroReferenceMark.addJabRefUri(
                        updatedReferenceMarkName,
                        i,
                        jabRefUri.get());
            }
        }

        return updatedReferenceMarkName;
    }

    private static Optional<String> createJabRefUriForZotero(ZoteroCitationData.CitationItemData citationItem,
                                                             BibDatabaseContext bibDatabaseContext,
                                                             DuplicateCheck duplicateCheck) {
        List<String> uris = Optional.of(citationItem.uris).orElse(List.of());
        // Filter citations that only contain ZoteroUri
        if (uris.stream().anyMatch(ZoteroReferenceMark::isJabRefUri) ||
                uris.stream().noneMatch(ZoteroReferenceMark::isZoteroUri)) {
            return Optional.empty();
        }

        return ZoteroCitationMarkParser.toBibEntry(citationItem)
                                       .flatMap(entry -> findDuplicate(entry, bibDatabaseContext, duplicateCheck))
                                       .flatMap(BibEntry::getCitationKey)
                                       .map(ZoteroReferenceMark::createJabRefUri);
    }

    private static Optional<BibEntry> findDuplicate(BibEntry entry,
                                                    BibDatabaseContext bibDatabaseContext,
                                                    DuplicateCheck duplicateCheck) {
        BibEntry duplicateEntry = null;
        for (BibEntry candidate : bibDatabaseContext.getEntries()) {
            if (candidate.getCitationKey().isPresent() &&
                    duplicateCheck.isDuplicate(entry, candidate, bibDatabaseContext.getMode())) {
                if (duplicateEntry != null) {
                    return Optional.empty();
                }
                duplicateEntry = candidate;
            }
        }
        return Optional.ofNullable(duplicateEntry);
    }

    private static void updateReferenceMarkName(XTextDocument document, String oldName, String newName)
            throws
            NoDocumentException,
            CreationException,
            com.sun.star.uno.Exception {
        Optional<XTextContent> oldContent = UnoReferenceMark.getAsTextContent(document, oldName);
        if (oldContent.isEmpty()) {
            return;
        }

        XTextRange range = oldContent.get().getAnchor();
        if (range == null) {
            return;
        }

        String currentText = range.getString();
        XText text = range.getText();
        XTextCursor cursor = text.createTextCursorByRange(range);

        text.removeTextContent(oldContent.get());

        XTextRange startRange = cursor.getStart();
        OOTextIntoOO.write(document, cursor, OOText.fromString(currentText));
        XTextRange endRange = cursor.getEnd();

        cursor.gotoRange(startRange, false);
        cursor.gotoRange(endRange, true);
        UnoReferenceMark.create(new DocumentAnnotation(document, newName, cursor, true));
    }
}
