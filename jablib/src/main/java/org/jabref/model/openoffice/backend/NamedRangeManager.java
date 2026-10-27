package org.jabref.model.openoffice.backend;

import java.util.List;
import java.util.Optional;

import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;

public interface NamedRangeManager {

    NamedRange createNamedRange(XTextDocument doc,
                                XComponentContext context,
                                String markName,
                                XTextCursor position,
                                boolean insertSpaceBefore,
                                boolean insertSpaceAfter,
                                boolean withoutBrackets)
            throws
            CreationException;

    List<String> getUsedNames(XTextDocument doc)
            throws
            NoDocumentException;

    Optional<NamedRange> getNamedRangeFromDocument(XTextDocument doc, String markName)
            throws
            NoDocumentException,
            WrappedTargetException;
}
