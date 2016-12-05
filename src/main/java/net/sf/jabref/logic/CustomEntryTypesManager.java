package net.sf.jabref.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.preferences.JabRefPreferences;

public class CustomEntryTypesManager {

    public static final Map<BibDatabaseMode, List<CustomEntryType>> CUSTOM_TYPES_BY_MODE_MAP = new HashMap<>();
    /**
     * Load all custom entry types from preferences. This method is
     * called from JabRef when the program starts.
     */
    public static void loadCustomEntryTypes(JabRefPreferences prefs) {
        List<CustomEntryType> customBibtexTypes = prefs.loadCustomEntryTypes(BibDatabaseMode.BIBTEX);
        for(CustomEntryType type : customBibtexTypes) {
            EntryTypes.addOrModifyCustomEntryType(type, BibDatabaseMode.BIBTEX);
        }
        CUSTOM_TYPES_BY_MODE_MAP.put(BibDatabaseMode.BIBTEX, customBibtexTypes);

        List<CustomEntryType> customBiblatexTypes = prefs.loadCustomEntryTypes(BibDatabaseMode.BIBLATEX);
        for(CustomEntryType type :customBiblatexTypes) {
            EntryTypes.addOrModifyCustomEntryType(type, BibDatabaseMode.BIBLATEX);
        }
        CUSTOM_TYPES_BY_MODE_MAP.put(BibDatabaseMode.BIBLATEX, customBiblatexTypes);
    }

    /**
     * Iterate through all entry types, and store those that are
     * custom defined to preferences. This method is called from
     * JabRefFrame when the program closes.
     */
    public static void saveCustomEntryTypes(JabRefPreferences prefs) {
        saveCustomEntryTypes(prefs, BibDatabaseMode.BIBTEX);
        saveCustomEntryTypes(prefs, BibDatabaseMode.BIBLATEX);

    }

    private static void saveCustomEntryTypes(JabRefPreferences prefs, BibDatabaseMode mode) {
        List<CustomEntryType> customBiblatexTypes = EntryTypes.getAllValues(mode).stream()
                .filter(type -> type instanceof CustomEntryType)
                .map(entryType -> (CustomEntryType) entryType).collect(Collectors.toList());

        prefs.storeCustomEntryTypes(customBiblatexTypes, mode);
    }

}
