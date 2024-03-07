package org.jabref.model.entry.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.model.entry.types.EntryType;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

public class FieldFactory {

    /**
     * Character separating field names that are to be used in sequence as fallbacks for a single column
     * (e.g. "author/editor" to use editor where author is not set):
     */
    private static final String FIELD_OR_SEPARATOR = "/";
    private static final String DELIMITER = ";";

    public static String serializeOrFields(Field... fields) {
        return serializeOrFields(new OrFields(fields));
    }

    public static String serializeOrFields(OrFields fields) {
        return fields.getFields().stream()
                     .map(field -> {
                         if (field instanceof UnknownField unknownField) {
                             // In case a user has put a user-defined field, the casing of that field is kept
                             return unknownField.getDisplayName();
                         } else {
                             // In all fields known to JabRef, the name is used - JabRef knows better than the user how to case the field
                             return field.getName();
                         }
                     })
                     .collect(Collectors.joining(FIELD_OR_SEPARATOR));
    }

    public static String serializeOrFieldsList(Set<OrFields> fields) {
        return fields.stream().map(FieldFactory::serializeOrFields).collect(Collectors.joining(DELIMITER));
    }

    public static List<Field> getNotTextFieldNames() {
        return Arrays.asList(StandardField.DOI, StandardField.FILE, StandardField.URL, StandardField.URI, StandardField.ISBN, StandardField.ISSN, StandardField.MONTH, StandardField.DATE, StandardField.YEAR);
    }

    public static List<Field> getIdentifierFieldNames() {
        return Arrays.asList(StandardField.DOI, StandardField.EPRINT, StandardField.PMID);
    }

    public static OrFields parseOrFields(String fieldNames) {
        Set<Field> fields = Arrays.stream(fieldNames.split(FieldFactory.FIELD_OR_SEPARATOR))
                                  .filter(StringUtil::isNotBlank)
                                  .map(FieldFactory::parseField)
                                  .collect(Collectors.toCollection(LinkedHashSet::new));
        return new OrFields(fields);
    }

    public static Set<OrFields> parseOrFieldsList(String fieldNames) {
        return Arrays.stream(fieldNames.split(FieldFactory.DELIMITER))
                     .filter(StringUtil::isNotBlank)
                     .map(FieldFactory::parseOrFields)
                     .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<Field> parseFieldList(String fieldNames) {
        return Arrays.stream(fieldNames.split(FieldFactory.DELIMITER))
                     .filter(StringUtil::isNotBlank)
                     .map(FieldFactory::parseField)
                     .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String serializeFieldsList(Collection<Field> fields) {
        return fields.stream()
                     .map(field -> {
                         if (field instanceof UnknownField unknownField) {
                             // In case a user has put a user-defined field, the casing of that field is kept
                             return unknownField.getDisplayName();
                         } else {
                             // In all fields known to JabRef, the name is used - JabRef knows better than the user how to case the field
                             return field.getName();
                         }
                     })
                     .collect(Collectors.joining(DELIMITER));
    }

    /**
     * Type T is an entry type and is used to direct the mapping to the Java field class.
     * This somehow acts as filter, BibLaTeX "APA" entry type has field "article", but we want to have StandardField (if not explicitly requested otherwise)
     */
    public static <T extends EntryType> Field parseField(T type, String fieldName) {
        // Check if the field name starts with "comment-" which indicates it's a UserSpecificCommentField
        if (fieldName.startsWith("comment-")) {
            String username = fieldName.substring("comment-".length());
            return new UserSpecificCommentField(username);
        }
        return OptionalUtil.<Field>orElse(
              OptionalUtil.<Field>orElse(
               OptionalUtil.<Field>orElse(
                OptionalUtil.<Field>orElse(
                 OptionalUtil.<Field>orElse(
                   OptionalUtil.<Field>orElse(
              InternalField.fromName(fieldName),
              StandardField.fromName(fieldName)),
              SpecialField.fromName(fieldName)),
              IEEEField.fromName(fieldName)),
              BiblatexSoftwareField.fromName(type, fieldName)),
              BiblatexApaField.fromName(type, fieldName)),
              AMSField.fromName(type, fieldName))
              .orElse(UnknownField.fromDisplayName(fieldName));
    }

    public static Field parseField(String fieldName) {
        return parseField(null, fieldName);
    }

    public static Set<Field> getKeyFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.SINGLE_ENTRY_LINK) || field.getProperties().contains(FieldProperty.MULTIPLE_ENTRY_LINK));
    }

    public static boolean isInternalField(Field field) {
        return field.getName().startsWith("__");
    }

    public static Set<Field> getJournalNameFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.JOURNAL_NAME));
    }

    /**
     * Returns a Set with all standard fields and including some common internal fields
     */
    public static Set<Field> getCommonFields() {
        EnumSet<StandardField> allFields = EnumSet.allOf(StandardField.class);

        LinkedHashSet<Field> publicAndInternalFields = new LinkedHashSet<>(allFields.size() + 3);
        publicAndInternalFields.add(InternalField.INTERNAL_ALL_FIELD);
        publicAndInternalFields.add(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD);
        publicAndInternalFields.add(InternalField.KEY_FIELD);
        publicAndInternalFields.addAll(allFields);

        return publicAndInternalFields;
    }

    /**
     * Returns a sorted Set of Fields (by {@link Field#getDisplayName} with all fields without internal ones
     */
    public static Set<Field> getAllFieldsWithOutInternal() {
        Set<Field> fields = new TreeSet<>(Comparator.comparing(Field::getDisplayName));
        fields.addAll(getAllFields());
        fields.removeAll(EnumSet.allOf(InternalField.class));

        return fields;
    }

    /**
     * Returns a  List with all standard fields and the citation key field
     */
    public static Set<Field> getStandardFieldsWithCitationKey() {
        EnumSet<StandardField> allFields = EnumSet.allOf(StandardField.class);

        LinkedHashSet<Field> standardFieldsWithBibtexKey = new LinkedHashSet<>(allFields.size() + 1);
        standardFieldsWithBibtexKey.add(InternalField.KEY_FIELD);
        standardFieldsWithBibtexKey.addAll(allFields);

        return standardFieldsWithBibtexKey;
    }

    public static Set<Field> getBookNameFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.BOOK_NAME));
    }

    public static Set<Field> getPersonNameFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.PERSON_NAMES));
    }

    private static Set<Field> getFieldsFiltered(Predicate<Field> selector) {
        return getAllFields().stream()
                             .filter(selector)
                             .collect(Collectors.toSet());
    }

    private static Set<Field> getAllFields() {
        Set<Field> fields = new HashSet<>();
        fields.addAll(EnumSet.allOf(BiblatexApaField.class));
        fields.addAll(EnumSet.allOf(BiblatexSoftwareField.class));
        fields.addAll(EnumSet.allOf(IEEEField.class));
        fields.addAll(EnumSet.allOf(InternalField.class));
        fields.addAll(EnumSet.allOf(SpecialField.class));
        fields.addAll(EnumSet.allOf(StandardField.class));
        fields.removeIf(field -> field instanceof UserSpecificCommentField);
        return fields;
    }

    /**
     * These are the fields JabRef always displays as default {@link org.jabref.preferences.JabRefPreferences#setLanguageDependentDefaultValues()}
     * <p>
     * A user can change them. The change is currently stored in the preferences only and not explicitly exposed as
     * separate preferences object
     */
    public static List<Field> getDefaultGeneralFields() {
        List<Field> defaultGeneralFields = new ArrayList<>(Arrays.asList(StandardField.DOI, StandardField.CROSSREF, StandardField.KEYWORDS, StandardField.EPRINT, StandardField.URL, StandardField.FILE, StandardField.GROUPS, StandardField.OWNER, StandardField.TIMESTAMP));
        defaultGeneralFields.addAll(EnumSet.allOf(SpecialField.class));
        return defaultGeneralFields;
    }

    // TODO: This should ideally be user configurable! (https://github.com/JabRef/jabref/issues/9840)
    // TODO: Move somewhere more appropriate in the future
    public static boolean isMultiLineField(final Field field, List<Field> nonWrappableFields) {
        return field.getProperties().contains(FieldProperty.MULTILINE_TEXT) || nonWrappableFields.contains(field);
    }
}
