package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.util.OptionalUtil;

public class FieldFactory {

    /**
     * Character separating field names that are to be used in sequence as fallbacks for a single column
     * (e.g. "author/editor" to use editor where author is not set):
     */
    public static final String FIELD_SEPARATOR = "/";

    public static String orFields(Field... fields) {
        return Arrays.stream(fields)
                     .map(Field::getName)
                     .collect(Collectors.joining(FIELD_SEPARATOR));
    }

    public static List<Field> getNotTextFieldNames() {
        return Arrays.asList(StandardField.DOI, StandardField.FILE, StandardField.URL, StandardField.URI, StandardField.ISBN, StandardField.ISSN, StandardField.MONTH, StandardField.DATE, StandardField.YEAR);
    }

    public static List<Field> getIdentifierFieldNames() {
        return Arrays.asList(StandardField.DOI, StandardField.EPRINT, StandardField.PMID);
    }

    public static Set<Field> parseFields(String fieldNames) {
        // OR fields
        if (fieldNames.contains(FieldFactory.FIELD_SEPARATOR)) {
            return Arrays.stream(fieldNames.split(FieldFactory.FIELD_SEPARATOR))
                         .map(FieldFactory::parseField)
                         .collect(Collectors.toSet());
        } else {
            return Collections.singleton(parseField(fieldNames));
        }
    }

    public static Field parseField(String fieldName) {
        return OptionalUtil.orElse(OptionalUtil.orElse(OptionalUtil.<Field>orElse(
                InternalField.fromName(fieldName),
                StandardField.fromName(fieldName)),
                SpecialField.fromName(fieldName)),
                IEEEField.fromName(fieldName))
                           .orElse(new UnknownField(fieldName));
    }
}
