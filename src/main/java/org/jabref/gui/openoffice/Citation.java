package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

class Citation  implements CitationSort.ComparableCitation {

    /** key in database */
    String citationKey;
    /** Result from database lookup. Optional.empty() if not found. */
    Optional<CitationDatabaseLookup.Result> db;
    /** The number used for numbered citation styles . */
    Optional<Integer> number;
    /** Letter that makes the in-text citation unique. */
    Optional<String> uniqueLetter;

    /** pageInfo: For Compat.DataModel.JabRef53 */
    Optional<String> pageInfo;

    /* missing: something that differentiates this from other
     * citations of the same citationKey. In particular, a
     * CitationGroup may contain multiple citations of the same
     * source. We use CitationPath.storageIndexInGroup to refer to
     * citations.
     */

    // TODO: Citation constructor needs dataModel, to check
    //       if usage of pageInfo confirms to expectations.
    Citation(String citationKey) {
        this.citationKey = citationKey;
        this.db = Optional.empty();
        this.number = Optional.empty();
        this.uniqueLetter = Optional.empty();
        this.pageInfo = Optional.empty();
    }

    @Override
    public String getCitationKey(){
        return citationKey;
    }

    @Override
    public String getPageInfoOrNull(){
        return pageInfo.orElse(null);
    }

    @Override
    public Optional<BibEntry> getBibEntry(){
        return (db.isPresent()
                ? Optional.of(db.get().entry)
                : Optional.empty());
    }
}
