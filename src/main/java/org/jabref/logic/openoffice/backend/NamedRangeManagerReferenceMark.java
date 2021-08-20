package org.jabref.logic.openoffice.backend;

import java.util.List;
import java.util.Optional;

import org.jabref.model.openoffice.backend.NamedRange;
import org.jabref.model.openoffice.backend.NamedRangeManager;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;

public class NamedRangeManagerReferenceMark implements NamedRangeManager {

    @Override
    public NamedRange createNamedRange(XTextDocument doc,
                                       String refMarkName,
                                       XTextCursor position,
                                       boolean insertSpaceAfter,
                                       boolean withoutBrackets)
        throws
        CreationException {
        return NamedRangeReferenceMark.create(doc, refMarkName, position, insertSpaceAfter, withoutBrackets);
    }

    @Override
    public List<String> getUsedNames(XTextDocument doc)
        throws
        NoDocumentException {
        return UnoReferenceMark.getListOfNames(doc);
    }

    @Override
    public Optional<NamedRange> getNamedRangeFromDocument(XTextDocument doc, String refMarkName)
        throws
        NoDocumentException,
        WrappedTargetException {
        return (NamedRangeReferenceMark
                .getFromDocument(doc, refMarkName)
                .map(x -> x));
    }
}

