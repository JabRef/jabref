package org.jabref.logic.openoffice.action;

import java.util.List;

import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;

public class ManageCitations {

    public static List<CitationEntry> getCitationEntries(XTextDocument doc)
        throws
        NoDocumentException,
        UnknownPropertyException,
        WrappedTargetException {
        OOFrontend fr = new OOFrontend(doc);
        return fr.getCitationEntries(doc);
    }

    public static void applyCitationEntries(XTextDocument doc, List<CitationEntry> citationEntries)
        throws
        NoDocumentException,
        UnknownPropertyException,
        NotRemoveableException,
        PropertyExistException,
        PropertyVetoException,
        IllegalTypeException,
        WrappedTargetException,
        IllegalArgumentException {
        OOFrontend fr = new OOFrontend(doc);
        fr.applyCitationEntries(doc, citationEntries);
    }
}
