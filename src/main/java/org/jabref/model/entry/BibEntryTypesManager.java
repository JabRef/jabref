package org.jabref.model.entry;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.types.BiblatexAPAEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibEntryTypesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryTypesManager.class);

    private final InternalEntryTypes BIBTEX_ENTRYTYPES = new InternalEntryTypes(
            Stream.concat(BibtexEntryTypeDefinitions.ALL.stream(), IEEETranEntryTypeDefinitions.ALL.stream())
                  .collect(Collectors.toList()));

    private final InternalEntryTypes BIBLATEX_ENTRYTYPES = new InternalEntryTypes(
            Stream.concat(BiblatexEntryTypeDefinitions.ALL.stream(),
                          Stream.concat(BiblatexSoftwareEntryTypeDefinitions.ALL.stream(), BiblatexAPAEntryTypeDefinitions.ALL.stream()))
                  .collect(Collectors.toList()));

    public BibEntryTypesManager() {
    }

    @VisibleForTesting
    InternalEntryTypes getEntryTypes(BibDatabaseMode mode) {
        return switch (mode) {
            case BIBTEX -> BIBTEX_ENTRYTYPES;
            case BIBLATEX -> BIBLATEX_ENTRYTYPES;
        };
    }

    /**
     * Returns all types known to JabRef. This includes the standard types as well as the customized types
     */
    public Collection<BibEntryType> getAllTypes(BibDatabaseMode mode) {
        return getEntryTypes(mode).getAllTypes();
    }

    /**
     * Returns all types which are customized (be it a variant of a standard type or a completely new type)
     */
    public Collection<BibEntryType> getAllCustomizedTypes(BibDatabaseMode mode) {
        return getEntryTypes(mode).getAllCustomizedTypes();
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
     * Returns true if the type is a custom type, or if it is a standard type which has different customized fields
     */
    public boolean isCustomOrModifiedType(BibEntryType type, BibDatabaseMode mode) {
        return getEntryTypes(mode).isCustomOrModifiedType(type);
    }

    /**
     * Required to check if during load of a .bib file the customization of the entry type is different
     *
     * @return true if the given type is unknown here or is different from the stored one
     */
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
     * Sets the given custom entry types for BibTeX and biblatex mode
     */
    public void addCustomOrModifiedTypes(List<BibEntryType> customizedEntryTypes, BibDatabaseMode mode) {
        InternalEntryTypes entryTypes = getEntryTypes(mode);
        customizedEntryTypes.forEach(entryTypes::addCustomOrModifiedType);
    }

    public void addCustomOrModifiedType(BibEntryType entryType, BibDatabaseMode mode) {
        getEntryTypes(mode).addCustomOrModifiedType(entryType);
    }

    /**
     * Updates the internal list. In case the given entry type equals a standard type, it is removed from the list of customized types.
     * Otherwise, it is stored as customized.
     */
    public void update(BibEntryType entryType, BibDatabaseMode mode) {
        InternalEntryTypes entryTypes = getEntryTypes(mode);
        if (entryTypes.standardTypes.contains(entryType)) {
            // The method to check containment does a deep equals. Thus, different fields lead to a non-containment property
            entryTypes.removeCustomOrModifiedEntryType(entryType);
            return;
        }
        if (!entryTypes.isStandardType(entryType)) {
            entryTypes.addCustomOrModifiedType(entryType);
        }

        // Workaround for UI not supporting OrFields
        Optional<BibEntryType> standardTypeOpt = entryTypes.standardTypes.stream()
                                                                      .filter(InternalEntryTypes.typeEquals(entryType.getType()))
                                                                      .findFirst();
        if (standardTypeOpt.isEmpty()) {
            LOGGER.debug("Standard type not found for {}", entryType.getType());
            entryTypes.addCustomOrModifiedType(entryType);
            return;
        }

        BibEntryType standardType = standardTypeOpt.get();
        Set<Field> standardRequiredFields = standardType.getRequiredFields().stream()
                                                        .map(OrFields::getFields)
                                                        .flatMap(Set::stream)
                                                        .collect(Collectors.toSet());
        Set<BibField> standardOptionalFields = standardType.getOptionalFields();

        Set<Field> entryTypeRequiredFields = entryType.getRequiredFields().stream()
                                                                  .map(OrFields::getFields)
                                                                  .flatMap(Set::stream)
                                                      .collect(Collectors.toSet());
        Set<BibField> entryTypeOptionalFields = entryType.getOptionalFields();

        if (standardRequiredFields.equals(entryTypeRequiredFields) && standardOptionalFields.equals(entryTypeOptionalFields)) {
            entryTypes.removeCustomOrModifiedEntryType(entryType);
            return;
        }
        LOGGER.debug("Different standard type fields for {} and standard {}", entryType, standardType);
        entryTypes.addCustomOrModifiedType(entryType);
    }

    public void removeCustomOrModifiedEntryType(BibEntryType entryType, BibDatabaseMode mode) {
        getEntryTypes(mode).removeCustomOrModifiedEntryType(entryType);
    }

    public void clearAllCustomEntryTypes(BibDatabaseMode mode) {
        getEntryTypes(mode).clearAllCustomEntryTypes();
    }

    /**
     * Checks if the given type is NOT a standard type AND customized inside the entry types manager.
     * There might be also types not known to the entry types manager, which are neither standard nor customized.
     */
    public boolean isCustomType(EntryType type, BibDatabaseMode mode) {
        return !getEntryTypes(mode).isStandardType(type) && enrich(type, mode).isPresent();
    }

    /**
     * Checks if the given type is NOT a standard type AND customized inside the entry types manager.
     * There might be also types not known to the entry types manager, which are neither standard nor customized.
     */
    public boolean isCustomType(BibEntryType type, BibDatabaseMode mode) {
        return !getEntryTypes(mode).isStandardType(type) && getEntryTypes(mode).isCustomOrModifiedType(type);
    }

    /**
     * This method returns the BibEntryType for the entry type.
     *
     * @param mode the mode of the BibDatabase, may be null
     */
    public Optional<BibEntryType> enrich(EntryType type, BibDatabaseMode mode) {
        return getEntryTypes(mode).enrich(type);
    }

    /**
     * This class is used to specify entry types for either BIBTEX and BIBLATEX.
     */
    @VisibleForTesting
    static class InternalEntryTypes {
        @VisibleForTesting
        final Set<BibEntryType> standardTypes;

        // TreeSet needs to be used here, because then, org.jabref.model.entry.BibEntryType.compareTo is used - instead of org.jabref.model.entry.BibEntryType.equals
        private final SortedSet<BibEntryType> customOrModifiedType = new TreeSet<>();

        private InternalEntryTypes(List<BibEntryType> standardTypes) {
            this.standardTypes = new HashSet<>(standardTypes);
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
                LOGGER.debug("Using customized entry type for {}", type.getName());
                return enrichedType;
            } else {
                return standardTypes.stream()
                                    .filter(typeEquals(type))
                                    .findFirst();
            }
        }

        static Predicate<BibEntryType> typeEquals(EntryType toCompare) {
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

        /**
         * Returns all types known to JabRef. This includes the standard types as well as the customized types
         */
        private SortedSet<BibEntryType> getAllTypes() {
            SortedSet<BibEntryType> allTypes = new TreeSet<>(customOrModifiedType);
            allTypes.addAll(standardTypes);
            return allTypes;
        }

        /**
         * Returns all types which are customized (be it a variant of a standard type or a completely new type)
         */
        private SortedSet<BibEntryType> getAllCustomizedTypes() {
            return new TreeSet<>(customOrModifiedType);
        }

        private boolean isCustomOrModifiedType(BibEntryType entryType) {
            boolean contains = customOrModifiedType.contains(entryType);
            if (!contains) {
                return false;
            }
            Optional<BibEntryType> standardType = getStandardType(entryType);
            if (standardType.isEmpty()) {
                // No standard type - and customized, then it is a custom type
                return true;
            }
            // In case of a standard type, we need to check if the fields are different.
            // The TreeSet uses compareTo and not equals, thus we need to get the stored type to do a deep comparison
            return !EntryTypeFactory.nameAndFieldsAreEqual(standardType.get(), entryType);
        }

        private Optional<BibEntryType> getStandardType(BibEntryType entryType) {
            return standardTypes.stream().filter(item -> item.getType().equals(entryType.getType())).findAny();
        }

        private boolean isStandardType(BibEntryType entryType) {
            return getStandardType(entryType).isPresent();
        }

        private boolean isStandardType(EntryType entryType) {
            return standardTypes.stream().anyMatch(item -> item.getType().equals(entryType));
        }
    }
}
