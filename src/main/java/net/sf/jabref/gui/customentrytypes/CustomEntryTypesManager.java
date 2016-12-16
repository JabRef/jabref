package net.sf.jabref.gui.customentrytypes;

import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.preferences.JabRefPreferences;

public class CustomEntryTypesManager {

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
