package org.jabref.model.entry.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        return fields.stream()
                     .map(Field::getName)
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
                     .map(Field::getName)
                     .collect(Collectors.joining(DELIMITER));
    }

    public static Field parseField(String fieldName) {
        return OptionalUtil.<Field>orElse(OptionalUtil.<Field>orElse(OptionalUtil.<Field>orElse(
                InternalField.fromName(fieldName),
                StandardField.fromName(fieldName)),
                SpecialField.fromName(fieldName)),
                IEEEField.fromName(fieldName))
                .orElse(new UnknownField(fieldName));
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
     * Returns a  List with all standard fields and including some common internal fields
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
        fields.addAll(EnumSet.allOf(IEEEField.class));
        fields.addAll(EnumSet.allOf(InternalField.class));
        fields.addAll(EnumSet.allOf(SpecialField.class));
        fields.addAll(EnumSet.allOf(StandardField.class));
        return fields;
    }

    /**
     * These are the fields JabRef always displays as default {@link org.jabref.preferences.JabRefPreferences#setLanguageDependentDefaultValues()}
     *
     * A user can change them. The change is currently stored in the preferences only and not explicitly exposed as
     * separate preferences object
     */
    public static List<Field> getDefaultGeneralFields() {
        List<Field> defaultGeneralFields = new ArrayList<>(Arrays.asList(StandardField.DOI, StandardField.CROSSREF, StandardField.KEYWORDS, StandardField.EPRINT, StandardField.URL, StandardField.FILE, StandardField.GROUPS, StandardField.OWNER, StandardField.TIMESTAMP));
        defaultGeneralFields.addAll(EnumSet.allOf(SpecialField.class));
        return defaultGeneralFields;
    }

    // TODO: This should ideally be user configurable! Move somewhere more appropriate in the future
    public static boolean isMultiLineField(final Field field, List<Field> nonWrappableFields) {
        // Treat unknown fields as multi-line fields
        return (field instanceof UnknownField) || nonWrappableFields.contains(field) || field.equals(StandardField.ABSTRACT) || field.equals(StandardField.COMMENT) || field.equals(StandardField.REVIEW);
    }
}
