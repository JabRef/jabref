package org.jabref.model.openoffice.backend;

import java.util.List;
import java.util.Optional;

import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;

public interface NamedRangeManager {

    NamedRange nrmCreate(XTextDocument doc,
                         String markName,
                         XTextCursor position,
                         boolean insertSpaceAfter,
                         boolean withoutBrackets)
        throws
        CreationException;

    List<String> nrmGetUsedNames(XTextDocument doc)
        throws
        NoDocumentException;

    Optional<NamedRange> nrmGetFromDocument(XTextDocument doc, String markName)
        throws
        NoDocumentException,
        WrappedTargetException;
}
