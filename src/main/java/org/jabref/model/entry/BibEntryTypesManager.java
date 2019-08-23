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
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;

public class BibEntryTypesManager {
    public static final String ENTRYTYPE_FLAG = "jabref-entrytype: ";
    private final InternalEntryTypes BIBTEX = new InternalEntryTypes(Stream.concat(BibtexEntryTypeDefinitions.ALL.stream(), IEEETranEntryTypeDefinitions.ALL.stream()).collect(Collectors.toList()));
    private final InternalEntryTypes BIBLATEX = new InternalEntryTypes(BiblatexEntryTypeDefinitions.ALL);

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
        builder.append(FieldFactory.serializeFieldsList(entryType.getOptionalFields().stream().map(BibField::getField).collect(Collectors.toSet())));
        builder.append("]");
        return builder.toString();
    }

    /**
     * Returns true if the type is a custom type, or if it is a standard type which has customized fields
     */
    public boolean isCustomizedType(BibEntryType type, BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBLATEX ? BIBLATEX.isCustomizedType(type) : BIBTEX.isCustomizedType(type);
    }

    /**
     * Sets the given custom entry types for BibTeX and biblatex mode
     */
    public void addCustomizedEntryTypes(List<BibEntryType> customBibtexEntryTypes, List<BibEntryType> customBiblatexEntryTypes) {
        customBibtexEntryTypes.forEach(type -> addCustomizedEntryType(type, BibDatabaseMode.BIBTEX));
        customBiblatexEntryTypes.forEach(type -> addCustomizedEntryType(type, BibDatabaseMode.BIBLATEX));
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
                                  .collect(Collectors.toList());
        }
    }

    public void addCustomizedEntryType(BibEntryType entryType, BibDatabaseMode mode) {
        if (BibDatabaseMode.BIBLATEX == mode) {
            BIBLATEX.addCustomizedType(entryType);
        } else if (BibDatabaseMode.BIBTEX == mode) {
            BIBTEX.addCustomizedType(entryType);
        }
    }

    public Collection<BibEntryType> getAllTypes(BibDatabaseMode type) {
        return type == BibDatabaseMode.BIBLATEX ? BIBLATEX.getAllTypes() : BIBTEX.getAllTypes();
    }

    public boolean isCustomType(EntryType type, BibDatabaseMode mode) {
        return getAllCustomTypes(mode).stream().anyMatch(customType -> customType.getType().equals(type));
    }

    /**
     * This method returns the BibtexEntryType for the entry type.
     */
    public Optional<BibEntryType> enrich(EntryType type, BibDatabaseMode mode) {
        return mode == BibDatabaseMode.BIBLATEX ? BIBLATEX.enrich(type) : BIBTEX.enrich(type);
    }

    /**
     * This class is used to specify entry types for either BIBTEX and BIBLATEX.
     */
    static class InternalEntryTypes {
        private final SortedSet<BibEntryType> customizedTypes = new TreeSet<>();
        private final SortedSet<BibEntryType> standardTypes;

        public InternalEntryTypes(List<BibEntryType> standardTypes) {
            this.standardTypes = new TreeSet<>(standardTypes);
        }

        /**
         * This method returns the BibtexEntryType for the name of a type,
         * or null if it does not exist.
         */
        public Optional<BibEntryType> enrich(EntryType type) {
            Optional<BibEntryType> enrichedType = customizedTypes.stream()
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

        private void addCustomizedType(BibEntryType type) {
            customizedTypes.remove(type);
            customizedTypes.add(type);
        }

        public SortedSet<BibEntryType> getAllTypes() {
            SortedSet<BibEntryType> allTypes = new TreeSet<>(customizedTypes);
            allTypes.addAll(standardTypes);
            return allTypes;
        }

        public boolean isCustomizedType(BibEntryType entryType) {
            return customizedTypes.stream().anyMatch(customizedType -> customizedType.getType().equals(entryType.getType()));
        }
    }
}
