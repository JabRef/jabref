package org.jabref.logic.openoffice.action;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import java.util.List;
import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.uno.NoDocumentException;

public class ManageCitations {

    private ManageCitations() {}

    public static List<CitationEntry> getCitationEntries(XTextDocument doc)
        throws NoDocumentException, WrappedTargetException {
        OOFrontend frontend = new OOFrontend(doc);
        return frontend.getCitationEntries(doc);
    }

    public static void applyCitationEntries(
        XTextDocument doc,
        List<CitationEntry> citationEntries
    )
        throws NoDocumentException, PropertyVetoException, IllegalTypeException, WrappedTargetException, IllegalArgumentException {
        OOFrontend frontend = new OOFrontend(doc);
        frontend.applyCitationEntries(doc, citationEntries);
    }
}
