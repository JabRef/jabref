package net.sf.jabref.bibtex;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.*;

import java.util.*;

public class EntryTypes {

    private static final TreeMap<String, EntryType> ALL_TYPES = new TreeMap<>();
    private static final TreeMap<String, EntryType> STANDARD_TYPES;


    static {
        // Put the standard entry types into the type map.
        Globals.prefs = JabRefPreferences.getInstance();
        if (!Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE)) {
            initBibtexEntryTypes();
        } else {
            initBibLatexEntryTypes();
        }
        // We need a record of the standard types, in case the user wants
        // to remove a customized version. Therefore we clone the map.
        STANDARD_TYPES = new TreeMap<>(ALL_TYPES);
    }

    private static void initBibLatexEntryTypes() {
        for (EntryType type : BibLatexEntryTypes.ALL) {
            ALL_TYPES.put(type.getName().toLowerCase(), type);
        }
    }

    private static void initBibtexEntryTypes() {
        // BibTex
        for (EntryType type : BibtexEntryTypes.ALL) {
            ALL_TYPES.put(type.getName().toLowerCase(), type);
        }
        // IEEE types
        for (EntryType type : IEEETranEntryTypes.ALL) {
            ALL_TYPES.put(type.getName().toLowerCase(), type);
        }
    }

    /**
     * This method returns the BibtexEntryType for the name of a type,
     * or null if it does not exist.
     */
    public static EntryType getType(String name) {
        return ALL_TYPES.get(name.toLowerCase());
    }

    /**
     * This method returns the standard BibtexEntryType for the
     * name of a type, or null if it does not exist.
     */
    public static EntryType getStandardType(String name) {
        return STANDARD_TYPES.get(name.toLowerCase());
    }

    public static void addOrModifyCustomEntryType(CustomEntryType type) {
        addOrModifyEntryType(type);
    }

    private static void addOrModifyEntryType(EntryType type) {
        ALL_TYPES.put(type.getName().toLowerCase(), type);
    }

    public static Set<String> getAllTypes() {
        return ALL_TYPES.keySet();
    }

    public static Collection<EntryType> getAllValues() {
        return ALL_TYPES.values();
    }

    /**
     * Removes a customized entry type from the type map. If this type
     * overrode a standard type, we reinstate the standard one.
     *
     * @param name The customized entry type to remove.
     */
    public static void removeType(String name) {
        String toLowerCase = name.toLowerCase();

        if (!ALL_TYPES.get(toLowerCase).equals(STANDARD_TYPES.get(toLowerCase))) {
            ALL_TYPES.remove(toLowerCase);

            if (STANDARD_TYPES.get(toLowerCase) != null) {
                // In this case the user has removed a customized version
                // of a standard type. We reinstate the standard type.
                addOrModifyEntryType(STANDARD_TYPES.get(toLowerCase));
            }
        }
    }

    // Get an entry type defined in BibtexEntryType
    public static EntryType getBibtexEntryType(String type) {
        // decide which entryType object to return
        EntryType o = getType(type);
        if (o != null) {
            return o;
        }
        if (Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE)) {
            return BibLatexEntryTypes.MISC;
        } else {
            return BibtexEntryTypes.MISC;
        }
    }
}
