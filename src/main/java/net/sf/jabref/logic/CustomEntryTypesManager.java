package net.sf.jabref.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;

public class CustomEntryTypesManager {

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
        String rest = comment.substring(CustomEntryType.ENTRYTYPE_FLAG.length());
        int indexEndOfName = rest.indexOf(':');
        if(indexEndOfName < 0) {
            return null;
        }
        String fieldsDescription = rest.substring(indexEndOfName + 2);

        int indexEndOfRequiredFields = fieldsDescription.indexOf(']');
        int indexEndOfOptionalFields = fieldsDescription.indexOf(']', indexEndOfRequiredFields + 1);
        if (indexEndOfRequiredFields < 4 || indexEndOfOptionalFields < indexEndOfRequiredFields + 6) {
            return null;
        }
        String name = rest.substring(0, indexEndOfName);
        String reqFields = fieldsDescription.substring(4, indexEndOfRequiredFields);
        String optFields = fieldsDescription.substring(indexEndOfRequiredFields + 6, indexEndOfOptionalFields);
        return new CustomEntryType(name, reqFields, optFields);
    }
}
