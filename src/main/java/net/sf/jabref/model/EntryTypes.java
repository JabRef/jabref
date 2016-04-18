package net.sf.jabref.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IEEETranEntryTypes;

public class EntryTypes {

    /**
     * This class is used to specify entry types for either BIBTEX and BIBLATEX.
     */
    private static class InternalEntryTypes {

        private final Map<String, EntryType> ALL_TYPES = new TreeMap<>();
        private final Map<String, EntryType> STANDARD_TYPES;
        private final EntryType defaultType;

        public InternalEntryTypes(EntryType defaultType, List<EntryType>... entryTypes) {
            this.defaultType = defaultType;
            for (List<EntryType> list : entryTypes) {
                for (EntryType type : list) {
                    ALL_TYPES.put(type.getName().toLowerCase(), type);
                }
            }
            STANDARD_TYPES = new TreeMap<>(ALL_TYPES);
        }

        /**
         * This method returns the BibtexEntryType for the name of a type,
         * or null if it does not exist.
         */
        public Optional<EntryType> getType(String name) {
            return Optional.ofNullable(ALL_TYPES.get(name.toLowerCase()));
        }

        /**
         * This method returns the EntryType for the name of a type,
         * or the default type (*.MISC) if it does not exist.
         */
        // Get an entry type defined in BibtexEntryType
        public EntryType getTypeOrDefault(String type) {
            return getType(type).orElse(defaultType);
        }

        /**
         * This method returns the standard BibtexEntryType for the
         * name of a type, or null if it does not exist.
         */
        public Optional<EntryType> getStandardType(String name) {
            return Optional.ofNullable(STANDARD_TYPES.get(name.toLowerCase()));
        }

        private void addOrModifyEntryType(EntryType type) {
            ALL_TYPES.put(type.getName().toLowerCase(), type);
        }

        public Set<String> getAllTypes() {
            return ALL_TYPES.keySet();
        }

        public Collection<EntryType> getAllValues() {
            return ALL_TYPES.values();
        }

        /**
         * Removes a customized entry type from the type map. If this type
         * overrode a standard type, we reinstate the standard one.
         *
         * @param name The customized entry type to remove.
         */
        public void removeType(String name) {
            String toLowerCase = name.toLowerCase();

            if (!ALL_TYPES.get(toLowerCase).equals(STANDARD_TYPES.get(toLowerCase))) {
                ALL_TYPES.remove(toLowerCase);

                if (STANDARD_TYPES.containsKey(toLowerCase)) {
                    // In this case the user has removed a customized version
                    // of a standard type. We reinstate the standard type.
                    addOrModifyEntryType(STANDARD_TYPES.get(toLowerCase));
                }
            }
        }

    }

    public static final InternalEntryTypes BIBTEX = new InternalEntryTypes(BibtexEntryTypes.MISC, BibtexEntryTypes.ALL, IEEETranEntryTypes.ALL);
    public static final InternalEntryTypes BIBLATEX = new InternalEntryTypes(BibLatexEntryTypes.MISC, BibLatexEntryTypes.ALL);

    /**
     * This method returns the BibtexEntryType for the name of a type,
     * or null if it does not exist.
     */
    public static Optional<EntryType> getType(String name, BibDatabaseMode type) {
        return type == BibDatabaseMode.BIBLATEX ? BIBLATEX.getType(name) : BIBTEX.getType(name);
    }

    /**
     * This method returns the EntryType for the name of a type,
     * or the default type (*.MISC) if it does not exist.
     */
    // Get an entry type defined in BibtexEntryType
    public static EntryType getTypeOrDefault(String name, BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBLATEX ? BIBLATEX.getTypeOrDefault(name) : BIBTEX.getTypeOrDefault(name);
    }

    /**
     * This method returns the standard BibtexEntryType for the
     * name of a type, or null if it does not exist.
     */
    public static Optional<EntryType> getStandardType(String name, BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBLATEX ? BIBLATEX.getStandardType(name) : BIBTEX.getStandardType(name);
    }

    public static void addOrModifyCustomEntryType(CustomEntryType customEntryType) {
        addOrModifyEntryType(customEntryType);
    }

    private static void addOrModifyEntryType(EntryType name) {
        BIBLATEX.addOrModifyEntryType(name);
        BIBTEX.addOrModifyEntryType(name);
    }

    public static Set<String> getAllTypes(BibDatabaseMode type) {
        return type == BibDatabaseMode.BIBLATEX ? BIBLATEX.getAllTypes() : BIBTEX.getAllTypes();
    }

    public static Collection<EntryType> getAllValues(BibDatabaseMode type) {
        return type == BibDatabaseMode.BIBLATEX ? BIBLATEX.getAllValues() : BIBTEX.getAllValues();
    }

    /**
     * Removes a customized entry type from the type map. If this type
     * overrode a standard type, we reinstate the standard one.
     *
     * @param name The customized entry type to remove.
     */
    public static void removeType(String name, BibDatabaseMode type) {
        if (type == BibDatabaseMode.BIBLATEX) {
            BIBLATEX.removeType(name);
        } else {
            BIBTEX.removeType(name);
        }
    }

    public static void removeAllCustomEntryTypes() {
        for(BibDatabaseMode type : BibDatabaseMode.values()) {
            for(String typeName : new HashSet<>(getAllTypes(type))) {
                getType(typeName, type).ifPresent(entryType -> {
                    if (entryType instanceof CustomEntryType) {
                        removeType(typeName, type);
                    }
                });
            }
        }
    }
}
