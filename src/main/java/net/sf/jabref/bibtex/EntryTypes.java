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
        ALL_TYPES.put("article", BibLatexEntryTypes.ARTICLE);
        ALL_TYPES.put("book", BibLatexEntryTypes.BOOK);
        ALL_TYPES.put("inbook", BibLatexEntryTypes.INBOOK);
        ALL_TYPES.put("bookinbook", BibLatexEntryTypes.BOOKINBOOK);
        ALL_TYPES.put("suppbook", BibLatexEntryTypes.SUPPBOOK);
        ALL_TYPES.put("booklet", BibLatexEntryTypes.BOOKLET);
        ALL_TYPES.put("collection", BibLatexEntryTypes.COLLECTION);
        ALL_TYPES.put("incollection", BibLatexEntryTypes.INCOLLECTION);
        ALL_TYPES.put("suppcollection", BibLatexEntryTypes.SUPPCOLLECTION);
        ALL_TYPES.put("manual", BibLatexEntryTypes.MANUAL);
        ALL_TYPES.put("misc", BibLatexEntryTypes.MISC);
        ALL_TYPES.put("online", BibLatexEntryTypes.ONLINE);
        ALL_TYPES.put("patent", BibLatexEntryTypes.PATENT);
        ALL_TYPES.put("periodical", BibLatexEntryTypes.PERIODICAL);
        ALL_TYPES.put("suppperiodical", BibLatexEntryTypes.SUPPPERIODICAL);
        ALL_TYPES.put("proceedings", BibLatexEntryTypes.PROCEEDINGS);
        ALL_TYPES.put("inproceedings", BibLatexEntryTypes.INPROCEEDINGS);
        ALL_TYPES.put("reference", BibLatexEntryTypes.REFERENCE);
        ALL_TYPES.put("inreference", BibLatexEntryTypes.INREFERENCE);
        ALL_TYPES.put("report", BibLatexEntryTypes.REPORT);
        ALL_TYPES.put("set", BibLatexEntryTypes.SET);
        ALL_TYPES.put("thesis", BibLatexEntryTypes.THESIS);
        ALL_TYPES.put("unpublished", BibLatexEntryTypes.UNPUBLISHED);
        ALL_TYPES.put("conference", BibLatexEntryTypes.CONFERENCE);
        ALL_TYPES.put("electronic", BibLatexEntryTypes.ELECTRONIC);
        ALL_TYPES.put("mastersthesis", BibLatexEntryTypes.MASTERSTHESIS);
        ALL_TYPES.put("phdthesis", BibLatexEntryTypes.PHDTHESIS);
        ALL_TYPES.put("techreport", BibLatexEntryTypes.TECHREPORT);
        ALL_TYPES.put("www", BibLatexEntryTypes.WWW);
        ALL_TYPES.put("ieeetranbstctl", BibLatexEntryTypes.IEEETRANBSTCTL);
    }

    private static void initBibtexEntryTypes() {
        // BibTex
        for(EntryType type: BibtexEntryTypes.ALL) {
            ALL_TYPES.put(type.getName().toLowerCase(), type);
        }
        // IEEE types
        for(EntryType type: IEEETranEntryTypes.ALL) {
            ALL_TYPES.put(type.getName().toLowerCase(), type);
        }
    }


    /**
     * This method returns the BibtexEntryType for the name of a type,
     * or null if it does not exist.
     */
    public static EntryType getType(String name) {
        EntryType entryType = ALL_TYPES.get(name.toLowerCase(Locale.US));
        if (entryType == null) {
            return null;
        }
        return entryType;
    }

    /**
     * This method returns the standard BibtexEntryType for the
     * name of a type, or null if it does not exist.
     */
    public static EntryType getStandardType(String name) {
        EntryType entryType = STANDARD_TYPES.get(name.toLowerCase());
        if (entryType == null) {
            return null;
        } else {
            return entryType;
        }
    }

    public static void addOrModifyCustomEntryType(CustomEntryType type) {
        ALL_TYPES.put(type.getName().toLowerCase(Locale.US), type);
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

        ALL_TYPES.remove(toLowerCase);

        if (STANDARD_TYPES.get(toLowerCase) != null) {
            // In this case the user has removed a customized version
            // of a standard type. We reinstate the standard type.
            addOrModifyCustomEntryType((CustomEntryType) STANDARD_TYPES.get(toLowerCase));
        }
    }

    // Get an entry type defined in BibtexEntryType
    public static EntryType getBibtexEntryType(String type) {
        // decide which entryType object to return
        EntryType o = getType(type);
        if (o != null) {
            return o;
        }
        return BibtexEntryTypes.OTHER;
    }
}
