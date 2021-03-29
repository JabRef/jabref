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


class CitedKeys {

    /**
     * Order-preserving map from citation keys to associated data.
     */
    LinkedHashMap<String, CitedKey> data;

    CitedKeys(LinkedHashMap<String, CitedKey> data) {
        this.data = data;
    }

    /**
     *  The cited keys in sorted order.
     */
    public List<CitedKey> values() {
        return new ArrayList<>(data.values());
    }

    /**
     * Sort entries for the bibliography.
     */
    void sortByComparator(Comparator<BibEntry> entryComparator) {
        List<CitedKey> cks = new ArrayList<>(data.values());
        cks.sort(new CitationSort.CitationComparator(entryComparator, true));
        LinkedHashMap<String, CitedKey> newData = new LinkedHashMap<>();
        for (CitedKey ck : cks) {
            newData.put(ck.citationKey, ck);
        }
        data = newData;
    }

    void numberCitedKeysInCurrentOrder() {
        int i = 1;
        for (CitedKey ck : data.values()) {
            ck.number = Optional.of(i); // was: -1 for UndefinedBibtexEntry
            i++;
        }
    }

    void lookupInDatabases(List<BibDatabase> databases) {
        for (CitedKey ck : this.data.values()) {
            ck.lookupInDatabases(databases);
        }
    }

    void distributeDatabaseLookupResults(CitationGroups cgs) {
        for (CitedKey ck : this.data.values()) {
            ck.distributeDatabaseLookupResult(cgs);
        }
    }

    void distributeNumbers(CitationGroups cgs) {
        for (CitedKey ck : this.data.values()) {
            ck.distributeNumber(cgs);
        }
    }

    void distributeUniqueLetters(CitationGroups cgs) {
        for (CitedKey ck : this.data.values()) {
            ck.distributeUniqueLetter(cgs);
        }
    }

} // class CitedKeys
