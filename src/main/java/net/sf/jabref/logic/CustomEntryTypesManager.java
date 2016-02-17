package net.sf.jabref.logic;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.EntryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomEntryTypesManager {
    private static final Log LOGGER = LogFactory.getLog(CustomEntryTypesManager.class);

    public static final List<EntryType> ALL = new ArrayList<>();
    /**
     * Load all custom entry types from preferences. This method is
     * called from JabRef when the program starts.
     */
    public static void loadCustomEntryTypes(JabRefPreferences prefs) {
        int number = 0;
        CustomEntryType type;
        while ((type = prefs.getCustomEntryType(number)) != null) {
            EntryTypes.addOrModifyCustomEntryType(type);
            ALL.add(type);
            number++;
        }
    }

    /**
     * Iterate through all entry types, and store those that are
     * custom defined to preferences. This method is called from
     * JabRefFrame when the program closes.
     */
    public static void saveCustomEntryTypes(JabRefPreferences prefs) {
        Iterator<EntryType> iterator = EntryTypes.getAllValues(BibDatabaseMode.BIBTEX).iterator();
        int number = 0;

        while (iterator.hasNext()) {
            EntryType entryType = iterator.next();
            if (entryType instanceof CustomEntryType) {
                // Store this entry type.
                prefs.storeCustomEntryType((CustomEntryType) entryType, number);
                number++;
            }
        }
        // Then, if there are more 'old' custom types defined, remove these
        // from preferences. This is necessary if the number of custom types
        // has decreased.
        prefs.purgeCustomEntryTypes(number);
    }

    public static CustomEntryType parseEntryType(String comment) {
        try {
            String rest;
            rest = comment.substring(CustomEntryType.ENTRYTYPE_FLAG.length());
            int nPos = rest.indexOf(':');
            rest = rest.substring(nPos + 2);

            int rPos = rest.indexOf(']');
            if (rPos < 4) {
                throw new IndexOutOfBoundsException();
            }
            String name = rest.substring(0, nPos);
            String reqFields = rest.substring(4, rPos);
            int oPos = rest.indexOf(']', rPos + 1);
            String optFields = rest.substring(rPos + 6, oPos);
            return new CustomEntryType(name, reqFields, optFields);
        } catch (IndexOutOfBoundsException ex) {
            LOGGER.info("Ill-formed entrytype comment in BibTeX file.", ex);
            return null;
        }
    }
}
