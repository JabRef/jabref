package org.jabref.model.entry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.types.BiblatexAPAEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;

public class BibEntryTypesManager {
    private final InternalEntryTypes BIBTEX_ENTRYTYPES = new InternalEntryTypes(
            Stream.concat(BibtexEntryTypeDefinitions.ALL.stream(), IEEETranEntryTypeDefinitions.ALL.stream())
                  .collect(Collectors.toList()));

    private final InternalEntryTypes BIBLATEX_ENTRYTYPES = new InternalEntryTypes(
            Stream.concat(BiblatexEntryTypeDefinitions.ALL.stream(),
                          Stream.concat(BiblatexSoftwareEntryTypeDefinitions.ALL.stream(), BiblatexAPAEntryTypeDefinitions.ALL.stream()))
                  .collect(Collectors.toList()));

    public BibEntryTypesManager() {
    }

    private InternalEntryTypes getEntryTypes(BibDatabaseMode mode) {
        return switch (mode) {
            case BIBTEX -> BIBTEX_ENTRYTYPES;
            case BIBLATEX -> BIBLATEX_ENTRYTYPES;
        };
    }

    /**
     * For a given database mode, determine all custom entry types, i.e. types that are not overwritten standard types but real custom types.
     * For example, a modified "article" type will not be included in the list, but an entry type like "MyCustomType" will be included.
     *
     * @param mode the BibDatabaseMode to be checked
     * @return the list of all found custom types
     */
    public List<BibEntryType> getAllCustomTypes(BibDatabaseMode mode) {
        return getEntryTypes(mode).getAllCustomTypes();
    }

    /**
     * Returns true if the type is a custom type, or if it is a standard type which has customized fields
     */
    public boolean isCustomOrModifiedType(BibEntryType type, BibDatabaseMode mode) {
        return getEntryTypes(mode).isCustomOrModifiedType(type);
    }

    /**
     * Sets the given custom entry types for BibTeX and biblatex mode
     */
    public void addCustomOrModifiedTypes(List<BibEntryType> customizedEntryTypes, BibDatabaseMode mode) {
        InternalEntryTypes entryTypes = getEntryTypes(mode);
        customizedEntryTypes.forEach(entryTypes::addCustomOrModifiedType);
    }

    public void addCustomOrModifiedType(BibEntryType entryType, BibDatabaseMode mode) {
        getEntryTypes(mode).addCustomOrModifiedType(entryType);
    }

    public void removeCustomOrModifiedEntryType(BibEntryType entryType, BibDatabaseMode mode) {
        getEntryTypes(mode).removeCustomOrModifiedEntryType(entryType);
    }

    public void clearAllCustomEntryTypes(BibDatabaseMode mode) {
        getEntryTypes(mode).clearAllCustomEntryTypes();
    }

    public Collection<BibEntryType> getAllTypes(BibDatabaseMode mode) {
        return getEntryTypes(mode).getAllTypes();
    }

    public boolean isCustomType(EntryType type, BibDatabaseMode mode) {
        return getAllCustomTypes(mode).stream().anyMatch(customType -> customType.getType().equals(type));
    }

    /**
     * This method returns the BibEntryType for the entry type.
     *
     * @param mode the mode of the BibDatabase, may be null
     */
    public Optional<BibEntryType> enrich(EntryType type, BibDatabaseMode mode) {
        return getEntryTypes(mode).enrich(type);
    }

    public boolean isDifferentCustomOrModifiedType(BibEntryType type, BibDatabaseMode mode) {
        Optional<BibEntryType> currentlyStoredType = enrich(type.getType(), mode);
        if (currentlyStoredType.isEmpty()) {
            // new customization
            return true;
        } else {
            // different customization
            return !EntryTypeFactory.nameAndFieldsAreEqual(type, currentlyStoredType.get());
        }
    }

    /**
     * This class is used to specify entry types for either BIBTEX and BIBLATEX.
     */
    private static class InternalEntryTypes {
        private final SortedSet<BibEntryType> customOrModifiedType = new TreeSet<>();
        private final SortedSet<BibEntryType> standardTypes;

        private InternalEntryTypes(List<BibEntryType> standardTypes) {
            this.standardTypes = new TreeSet<>(standardTypes);
        }

        private List<BibEntryType> getAllCustomTypes() {
            Collection<BibEntryType> customizedTypes = getAllTypes();
            return customizedTypes.stream()
                                  .filter(bibEntryType -> standardTypes.stream()
                                                                       .noneMatch(item -> item.getType().equals(bibEntryType.getType())))
                                  .toList();
        }

        /**
         * This method returns the BibtexEntryType for the name of a type,
         * or an empty optional if it does not exist.
         */
        private Optional<BibEntryType> enrich(EntryType type) {
            Optional<BibEntryType> enrichedType = customOrModifiedType.stream()
                                                                      .filter(typeEquals(type))
                                                                      .findFirst();
            if (enrichedType.isPresent()) {
                return enrichedType;
            } else {
                return standardTypes.stream()
                                    .filter(typeEquals(type))
                                    .findFirst();
            }
        }

        private Predicate<BibEntryType> typeEquals(EntryType toCompare) {
            return item -> item.getType().equals(toCompare);
        }

        private void addCustomOrModifiedType(BibEntryType type) {
            customOrModifiedType.remove(type);
            customOrModifiedType.add(type);
        }

        private void removeCustomOrModifiedEntryType(BibEntryType type) {
            customOrModifiedType.remove(type);
        }

        private void clearAllCustomEntryTypes() {
            customOrModifiedType.clear();
        }

        private SortedSet<BibEntryType> getAllTypes() {
            SortedSet<BibEntryType> allTypes = new TreeSet<>(customOrModifiedType);
            allTypes.addAll(standardTypes);
            return allTypes;
        }

        private boolean isCustomOrModifiedType(BibEntryType entryType) {
            return customOrModifiedType.stream()
                                       .anyMatch(customizedType -> customizedType.equals(entryType));
        }
    }
}
