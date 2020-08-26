package org.jabref.model.entry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;

public class BibEntryTypesManager {
    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    private final InternalEntryTypes BIBTEX = new InternalEntryTypes(Stream.concat(BibtexEntryTypeDefinitions.ALL.stream(), IEEETranEntryTypeDefinitions.ALL.stream()).collect(Collectors.toList()));
    private final InternalEntryTypes BIBLATEX = new InternalEntryTypes(Stream.concat(BiblatexEntryTypeDefinitions.ALL.stream(), BiblatexSoftwareEntryTypeDefinitions.ALL.stream()).collect(Collectors.toList()));

    public BibEntryTypesManager() {
    }

    public static Optional<BibEntryType> parse(String comment) {
        String rest = comment.substring(ENTRYTYPE_FLAG.length());
        int indexEndOfName = rest.indexOf(':');
        if (indexEndOfName < 0) {
            return Optional.empty();
        }
        String fieldsDescription = rest.substring(indexEndOfName + 2);

        int indexEndOfRequiredFields = fieldsDescription.indexOf(']');
        int indexEndOfOptionalFields = fieldsDescription.indexOf(']', indexEndOfRequiredFields + 1);
        if ((indexEndOfRequiredFields < 4) || (indexEndOfOptionalFields < (indexEndOfRequiredFields + 6))) {
            return Optional.empty();
        }
        EntryType type = EntryTypeFactory.parse(rest.substring(0, indexEndOfName));
        String reqFields = fieldsDescription.substring(4, indexEndOfRequiredFields);
        String optFields = fieldsDescription.substring(indexEndOfRequiredFields + 6, indexEndOfOptionalFields);

        BibEntryTypeBuilder entryTypeBuilder = new BibEntryTypeBuilder()
                .withType(type)
                .withImportantFields(FieldFactory.parseFieldList(optFields))
                .withRequiredFields(FieldFactory.parseOrFieldsList(reqFields));
        return Optional.of(entryTypeBuilder.build());
    }

    public static String serialize(BibEntryType entryType) {
        StringBuilder builder = new StringBuilder();
        builder.append(ENTRYTYPE_FLAG);
        builder.append(entryType.getType().getName());
        builder.append(": req[");
        builder.append(FieldFactory.serializeOrFieldsList(entryType.getRequiredFields()));
        builder.append("] opt[");
        builder.append(FieldFactory.serializeFieldsList(
                entryType.getOptionalFields()
                         .stream()
                         .map(BibField::getField)
                         .collect(Collectors.toList())));
        builder.append("]");
        return builder.toString();
    }

    /**
     * Returns true if the type is a custom type, or if it is a standard type which has customized fields
     */
    public boolean isCustomOrModifiedType(BibEntryType type, BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBLATEX ? BIBLATEX.isCustomOrModifiedType(type) : BIBTEX.isCustomOrModifiedType(type);
    }

    /**
     * Sets the given custom entry types for BibTeX and biblatex mode
     */
    public void addCustomOrModifiedTypes(List<BibEntryType> customizedBibtexEntryTypes, List<BibEntryType> customizedBiblatexEntryTypes) {
        customizedBibtexEntryTypes.forEach(type -> addCustomOrModifiedType(type, BibDatabaseMode.BIBTEX));
        customizedBiblatexEntryTypes.forEach(type -> addCustomOrModifiedType(type, BibDatabaseMode.BIBLATEX));
    }

    /**
     * For a given database mode, determine all custom entry types, i.e. types that are not overwritten standard types but real custom types.
     * For example, a modified "article" type will not be included in the list, but an entry type like "MyCustomType" will be included.
     *
     * @param mode the BibDatabaseMode to be checked
     * @return the list of all found custom types
     */
    public List<BibEntryType> getAllCustomTypes(BibDatabaseMode mode) {
        Collection<BibEntryType> customizedTypes = getAllTypes(mode);
        if (mode == BibDatabaseMode.BIBTEX) {
            return customizedTypes.stream()
                                  .filter(entryType -> BibtexEntryTypeDefinitions.ALL.stream().noneMatch(bibtexType -> bibtexType.getType().equals(entryType.getType())))
                                  .filter(entryType -> IEEETranEntryTypeDefinitions.ALL.stream().noneMatch(ieeeType -> ieeeType.getType().equals(entryType.getType())))
                                  .collect(Collectors.toList());
        } else {
            return customizedTypes.stream()
                                  .filter(entryType -> BiblatexEntryTypeDefinitions.ALL.stream().noneMatch(biblatexType -> biblatexType.getType().equals(entryType.getType())))
                                  .filter(entryType -> BiblatexSoftwareEntryTypeDefinitions.ALL.stream().noneMatch(biblatexSoftware -> biblatexSoftware.getType().equals(entryType.getType())))
                                  .collect(Collectors.toList());
        }
    }

    public void addCustomOrModifiedType(BibEntryType entryType, BibDatabaseMode mode) {
        if (BibDatabaseMode.BIBLATEX == mode) {
            BIBLATEX.addCustomOrModifiedType(entryType);
        } else if (BibDatabaseMode.BIBTEX == mode) {
            BIBTEX.addCustomOrModifiedType(entryType);
        }
    }

    public void removeCustomOrModifiedEntryType(BibEntryType entryType, BibDatabaseMode mode) {
        if (BibDatabaseMode.BIBLATEX == mode) {
            BIBLATEX.removeCustomOrModifiedEntryType(entryType);
        } else if (BibDatabaseMode.BIBTEX == mode) {
            BIBTEX.removeCustomOrModifiedEntryType(entryType);
        }
    }

    public void clearAllCustomEntryTypes(BibDatabaseMode mode) {
        if (BibDatabaseMode.BIBLATEX == mode) {
            BIBLATEX.clearAllCustomEntryTypes();
        } else if (BibDatabaseMode.BIBTEX == mode) {
            BIBTEX.clearAllCustomEntryTypes();
        }
    }

    public Collection<BibEntryType> getAllTypes(BibDatabaseMode type) {
        return type == BibDatabaseMode.BIBLATEX ? BIBLATEX.getAllTypes() : BIBTEX.getAllTypes();
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
        return mode == BibDatabaseMode.BIBLATEX ? BIBLATEX.enrich(type) : BIBTEX.enrich(type);
    }

    public boolean isDifferentCustomOrModifiedType(BibEntryType type, BibDatabaseMode mode) {
        Optional<BibEntryType> currentlyStoredType = enrich(type.getType(), mode);
        if (currentlyStoredType.isEmpty()) {
            // new customization
            return true;
        } else {
            // different customization
            return !EntryTypeFactory.isEqualNameAndFieldBased(type, currentlyStoredType.get());
        }
    }

    /**
     * This class is used to specify entry types for either BIBTEX and BIBLATEX.
     */
    static class InternalEntryTypes {
        private final SortedSet<BibEntryType> customOrModifiedType = new TreeSet<>();
        private final SortedSet<BibEntryType> standardTypes;

        public InternalEntryTypes(List<BibEntryType> standardTypes) {
            this.standardTypes = new TreeSet<>(standardTypes);
        }

        /**
         * This method returns the BibtexEntryType for the name of a type,
         * or null if it does not exist.
         */
        public Optional<BibEntryType> enrich(EntryType type) {
            Optional<BibEntryType> enrichedType = customOrModifiedType.stream()
                                                                      .filter(customizedType -> customizedType.getType().equals(type))
                                                                      .findFirst();
            if (enrichedType.isPresent()) {
                return enrichedType;
            } else {
                return standardTypes.stream()
                                    .filter(customizedType -> customizedType.getType().equals(type))
                                    .findFirst();
            }
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

        public SortedSet<BibEntryType> getAllTypes() {
            SortedSet<BibEntryType> allTypes = new TreeSet<>(customOrModifiedType);
            allTypes.addAll(standardTypes);
            return allTypes;
        }

        public boolean isCustomOrModifiedType(BibEntryType entryType) {
            return customOrModifiedType.stream()
                                       .anyMatch(customizedType -> customizedType.equals(entryType));
        }
    }
}
