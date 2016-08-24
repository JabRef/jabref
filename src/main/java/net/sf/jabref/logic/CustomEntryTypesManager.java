package net.sf.jabref.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.preferences.JabRefPreferences;

public class CustomEntryTypesManager {

    public static final List<EntryType> ALL = new ArrayList<>();
    /**
     * Load all custom entry types from preferences. This method is
     * called from JabRef when the program starts.
     */
    public static void loadCustomEntryTypes(JabRefPreferences prefs) {
        int number = 0;
        Optional<CustomEntryType> type;
        while ((type = prefs.getCustomEntryType(number)).isPresent()) {
            EntryTypes.addOrModifyCustomEntryType(type.get());
            ALL.add(type.get());
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

}
