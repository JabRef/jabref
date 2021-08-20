package org.jabref.logic.openoffice.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.logic.openoffice.frontend.UpdateCitationMarkers;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationMarkerEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;
import org.jabref.model.openoffice.style.OODataModel;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoScreenRefresh;
import org.jabref.model.openoffice.util.OOListUtil;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;

public class EditInsert {

    private EditInsert() {
        /**/
    }

    /**
     * In insertEntry we receive BibEntry values from the GUI.
     *
     * In the document we store citations by their citation key.
     *
     * If the citation key is missing, the best we can do is to notify the user. Or the programmer,
     * that we cannot accept such input.
     *
     */
    private static String insertEntryGetCitationKey(BibEntry entry) {
        Optional<String> key = entry.getCitationKey();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("insertEntryGetCitationKey: cannot cite entries without citation key");
        }
        return key.get();
    }

    /**
     * @param cursor Where to insert.
     * @param pageInfo A single pageInfo for a list of entries. This is what we get from the GUI.
     */
    public static void insertCitationGroup(XTextDocument doc,
                                           OOFrontend fr,
                                           XTextCursor cursor,
                                           List<BibEntry> entries,
                                           BibDatabase database,
                                           OOBibStyle style,
                                           CitationType citationType,
                                           String pageInfo)
        throws
        NoDocumentException,
        NotRemoveableException,
        WrappedTargetException,
        PropertyVetoException,
        CreationException,
        IllegalTypeException {

        List<String> citationKeys = OOListUtil.map(entries, EditInsert::insertEntryGetCitationKey);

        final int nEntries = entries.size();
        List<Optional<OOText>> pageInfos = OODataModel.fakePageInfos(pageInfo, nEntries);

        List<CitationMarkerEntry> citations = new ArrayList<>(nEntries);
        for (int i = 0; i < nEntries; i++) {
            Citation cit = new Citation(citationKeys.get(i));
            cit.lookupInDatabases(Collections.singletonList(database));
            cit.setPageInfo(pageInfos.get(i));
            citations.add(cit);
        }

        // The text we insert
        OOText citeText =
            (style.isNumberEntries()
             ? OOText.fromString("[-]") // A dash only. Only refresh later.
             : style.createCitationMarker(citations,
                                          citationType.inParenthesis(),
                                          NonUniqueCitationMarker.FORGIVEN));

        if ("".equals(OOText.toString(citeText))) {
            citeText = OOText.fromString("[?]");
        }

        try {
            UnoScreenRefresh.lockControllers(doc);
            UpdateCitationMarkers.createAndFillCitationGroup(fr,
                                                             doc,
                                                             citationKeys,
                                                             pageInfos,
                                                             citationType,
                                                             citeText,
                                                             cursor,
                                                             style,
                                                             true /* insertSpaceAfter */);
        } finally {
            UnoScreenRefresh.unlockControllers(doc);
        }

    }
}
