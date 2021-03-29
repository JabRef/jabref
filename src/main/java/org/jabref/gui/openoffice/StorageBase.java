package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import org.jabref.gui.openoffice.CitationSort;
import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class StorageBase {

    interface HasName {
        public String getName();
    }

    interface HasTextRange {

        /**
         * @return Null if the mark is missing from the document.
         */
        public XTextRange getMarkRangeOrNull(DocumentConnection documentConnection)
            throws
            NoDocumentException,
            WrappedTargetException;

        /**
         * Cursor for the reference marks as is, not prepared for filling,
         * but does not need cleanFillCursorForCitationGroup either.
         */
        public XTextCursor getRawCursor(DocumentConnection documentConnection)
            throws
            NoDocumentException,
            WrappedTargetException,
            CreationException;

        /**
         * Get a cursor for filling in text.
         *
         * Must be followed by cleanFillCursor()
         */
        public XTextCursor getFillCursor(DocumentConnection documentConnection)
            throws
            NoDocumentException,
            WrappedTargetException,
            CreationException;

        /**
         * Remove brackets, but if the result would become empty, leave
         * them; if the result would be a single characer, leave the left bracket.
         *
         * @param removeBracketsFromEmpty is intended to force removal if
         *        we are working on an "Empty citation" (INVISIBLE_CIT).
         */
        public void cleanFillCursor(DocumentConnection documentConnection)
            throws
            NoDocumentException,
            WrappedTargetException,
            CreationException ;

        /**
         *  Note: create is in NamedRangeManager
         */
        public void removeFromDocument(DocumentConnection documentConnection)
            throws
            WrappedTargetException,
            NoDocumentException,
            NoSuchElementException;
    }

    interface NamedRange extends HasName, HasTextRange {
        // nothing new here
    }

    interface NamedRangeManager {
        public NamedRange create( DocumentConnection documentConnection,
                                  String refMarkName,
                                  XTextCursor position,
                                  boolean insertSpaceAfter,
                                  boolean withoutBrackets )
            throws
            CreationException;

        public List<String> getUsedNames(DocumentConnection documentConnection)
            throws
            NoDocumentException;

        public NamedRange getFromDocumentOrNull(DocumentConnection documentConnection,
                                                String refMarkName)
            throws
            NoDocumentException,
            WrappedTargetException ;
    }
}
