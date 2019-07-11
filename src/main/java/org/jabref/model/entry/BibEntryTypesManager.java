package org.jabref.model.entry;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseMode;

public class BibEntryTypesManager {
    public static final InternalEntryTypes BIBTEX = new InternalEntryTypes(BibtexEntryTypes.ALL);
    public static final InternalEntryTypes BIBLATEX = new InternalEntryTypes(BiblatexEntryTypes.ALL);

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

    public static void addOrModifyCustomEntryType(CustomEntryType customEntryType, BibDatabaseMode mode) {
        if (BibDatabaseMode.BIBLATEX == mode) {
            BIBLATEX.addOrModifyEntryType(customEntryType);
        } else if (BibDatabaseMode.BIBTEX == mode) {
            BIBTEX.addOrModifyEntryType(customEntryType);
        }
    }

    public static Set<String> getAllTypes(BibDatabaseMode type) {
        return type == BibDatabaseMode.BIBLATEX ? BIBLATEX.getAllTypes() : BIBTEX.getAllTypes();
    }

    public static Collection<BibEntryType> getAllValues(BibDatabaseMode type) {
        return type == BibDatabaseMode.BIBLATEX ? BIBLATEX.getAllValues() : BIBTEX.getAllValues();
    }

    /**
     * Determine all CustomTypes which are not overwritten standard types but real custom types for a given BibDatabaseMode
     *
     * I.e., a modified "article" type will not be included in the list, but an EntryType like "MyCustomType" will be included.
     *
     * @param mode the BibDatabaseMode to be checked
     * @return the list of all found custom types
     */
    public static List<BibEntryType> getAllCustomTypes(BibDatabaseMode mode) {
        Collection<EntryType> allTypes = getAllValues(mode);
        if (mode == BibDatabaseMode.BIBTEX) {
            return allTypes.stream().filter(entryType -> !BibtexEntryTypes.getType(entryType.getType()).isPresent())
                           .filter(entryType -> !IEEETranEntryTypes.getType(entryType.getType()).isPresent())
                           .collect(Collectors.toList());
        } else {
            return allTypes.stream().filter(entryType -> !BiblatexEntryTypes.getType(entryType.getType()).isPresent())
                           .collect(Collectors.toList());
        }
    }

    public static List<EntryType> getAllModifiedStandardTypes(BibDatabaseMode mode) {
        if (mode == BibDatabaseMode.BIBTEX) {
            return getAllModifiedStandardTypes(BIBTEX);
        } else {
            return getAllModifiedStandardTypes(BIBLATEX);
        }
    }

    private static List<EntryType> getAllModifiedStandardTypes(InternalEntryTypes internalTypes) {
        return internalTypes.getAllValues().stream().filter(type -> type instanceof CustomEntryType)
                            .filter(type -> internalTypes.getStandardType(type).isPresent())
                            .collect(Collectors.toList());
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
        for (BibDatabaseMode type : BibDatabaseMode.values()) {
            for (String typeName : new HashSet<>(getAllTypes(type))) {
                getType(typeName, type).ifPresent(entryType -> {
                    if (entryType instanceof CustomEntryType) {
                        removeType(typeName, type);
                    }
                });
            }
        }
    }

    /**
     * Load given custom entry types for BibTeX and biblatex mode
     */
    public static void loadCustomEntryTypes(List<CustomEntryType> customBibtexEntryTypes, List<CustomEntryType> customBiblatexEntryTypes) {
        for (CustomEntryType type : customBibtexEntryTypes) {
            addOrModifyCustomEntryType(type, BibDatabaseMode.BIBTEX);
        }

        for (CustomEntryType type : customBiblatexEntryTypes) {
            addOrModifyCustomEntryType(type, BibDatabaseMode.BIBLATEX);
        }
    }

    /**
     * This class is used to specify entry types for either BIBTEX and BIBLATEX.
     */
    static class InternalEntryTypes {
        private final Map<String, EntryType> ALL_TYPES = new TreeMap<>();
        private final Map<String, EntryType> STANDARD_TYPES;
        private final EntryType defaultType;

        public InternalEntryTypes(EntryType defaultType, List<List<EntryType>> entryTypes) {
            this.defaultType = defaultType;

            for (List<EntryType> list : entryTypes) {
                for (EntryType type : list) {
                    ALL_TYPES.put(type.getType().toLowerCase(Locale.ROOT), type);
                }
            }
            STANDARD_TYPES = new TreeMap<>(ALL_TYPES);
        }

        /**
         * This method returns the BibtexEntryType for the name of a type,
         * or null if it does not exist.
         */
        public Optional<EntryType> getType(String name) {
            return Optional.ofNullable(ALL_TYPES.get(name.toLowerCase(Locale.ROOT)));
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
            return Optional.ofNullable(STANDARD_TYPES.get(name.toLowerCase(Locale.ROOT)));
        }

        private void addOrModifyEntryType(EntryType type) {
            ALL_TYPES.put(type.getType().toLowerCase(Locale.ROOT), type);
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
            String toLowerCase = name.toLowerCase(Locale.ROOT);

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
}
