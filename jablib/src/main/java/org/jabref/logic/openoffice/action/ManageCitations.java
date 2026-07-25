package org.jabref.logic.openoffice.action;

import java.util.List;

import org.jabref.logic.JabRefException;
import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;

public class ManageCitations {

    private ManageCitations() {
    }

    public static OOResult<List<CitationEntry>, JabRefException> getCitationEntries(XTextDocument doc) {
        try {
            OOFrontend frontend = new OOFrontend(doc);
            return OOResult.ok(frontend.getCitationEntries(doc));
        } catch (NoDocumentException | WrappedTargetException e) {
            return OOResult.error(new JabRefException(e.getMessage(), e));
        }
    }

    public static OOVoidResult<JabRefException> applyCitationEntries(XTextDocument doc, List<CitationEntry> citationEntries) {
        try {
            OOFrontend frontend = new OOFrontend(doc);
            frontend.applyCitationEntries(doc, citationEntries);
            return OOVoidResult.ok();
        } catch (NoDocumentException | PropertyVetoException | IllegalTypeException | WrappedTargetException e) {
            return OOVoidResult.error(new JabRefException(e.getMessage(), e));
        }
    }
}
