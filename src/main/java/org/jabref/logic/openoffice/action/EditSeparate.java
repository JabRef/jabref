package org.jabref.logic.openoffice.action;

import java.util.List;

import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.logic.openoffice.frontend.UpdateCitationMarkers;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.logic.openoffice.style.OOProcess;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoScreenRefresh;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class EditSeparate {

    private EditSeparate() {
        /**/
    }

    public static boolean separateCitations(XTextDocument doc,
                                            OOFrontend frontend,
                                            List<BibDatabase> databases,
                                            OOBibStyle style)
        throws
        CreationException,
        IllegalTypeException,
        NoDocumentException,
        NotRemoveableException,
        PropertyVetoException,
        WrappedTargetException,
        com.sun.star.lang.IllegalArgumentException {

        boolean madeModifications = false;

        // To reduce surprises in JabRef52 mode, impose localOrder to
        // decide the visually last Citation in the group. Unless the
        // style changed since refresh this is the last on the screen
        // as well.
        frontend.citationGroups.lookupCitations(databases);
        frontend.citationGroups.imposeLocalOrder(OOProcess.comparatorForMulticite(style));

        List<CitationGroup> cgs = frontend.citationGroups.getCitationGroupsUnordered();

        try {
            UnoScreenRefresh.lockControllers(doc);

            for (CitationGroup group : cgs) {

                XTextRange range1 = (frontend
                                     .getMarkRange(doc, group)
                                     .orElseThrow(IllegalStateException::new));
                XTextCursor textCursor = range1.getText().createTextCursorByRange(range1);

                List<Citation> cits = group.citationsInStorageOrder;
                if (cits.size() <= 1) {
                    continue;
                }

                frontend.removeCitationGroup(group, doc);
                // Now we own the content of cits

                // Create a citation group for each citation.
                final int last = cits.size() - 1;
                for (int i = 0; i < cits.size(); i++) {
                    boolean insertSpaceAfter = (i != last);
                    Citation cit = cits.get(i);

                    UpdateCitationMarkers.createAndFillCitationGroup(frontend,
                                                                     doc,
                                                                     List.of(cit.citationKey),
                                                                     List.of(cit.getPageInfo()),
                                                                     group.citationType,
                                                                     OOText.fromString(cit.citationKey),
                                                                     textCursor,
                                                                     style,
                                                                     insertSpaceAfter);

                    textCursor.collapseToEnd();
                }

                madeModifications = true;
            }
        } finally {
            UnoScreenRefresh.unlockControllers(doc);
        }
        return madeModifications;
    }
}
