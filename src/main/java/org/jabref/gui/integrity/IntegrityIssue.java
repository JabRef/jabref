package org.jabref.gui.integrity;

import java.util.Arrays;
import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

public enum IntegrityIssue {
    CITATION_KEY_DEVIATES_FROM_GENERATED_KEY(InternalField.KEY_FIELD, "citation key deviates from generated key"),
    BIBTEX_FIELD_ONLY_KEY(InternalField.KEY_FIELD, "bibtex field only"),
    BIBTEX_FIELD_ONLY_CROSS_REF(StandardField.CROSSREF, "bibtex field only"),
    INCORRECT_FORMAT(StandardField.ISSN, "incorrect format"),
    JOURNAL_NOT_FOUND_IN_ABBREVIATION_LIST(StandardField.JOURNAL, "journal not found in the abbreviation list"),
    NO_INTEGER_AS_VALUE_FOR_EDITION_ALLOWED(StandardField.EDITION, "no integer as value for edition allowed"),
    REFERENCED_CITATION_KEY_DOES_NOT_EXIST(StandardField.CROSSREF, "referenced citation key does ont exist"),
    NON_ASCII_ENCODED_CHARACTER_FOUND(StandardField.ABSTRACT, "Non-Ascii encoded character found"),
    SHOULD_CONTAIN_A_VALID_PAGE_NUMBER_RANGE(StandardField.PAGES, "should contain a valid page number range"),
    CAPITAL_LETTER_ARE_NOT_MASKED_USING_CURLY_BRACKETS(StandardField.TITLE, "capital letters are not masked using curly brackets {}");

    private final Field field;
    private final String text;

    IntegrityIssue(Field field, String text) {
        this.field = field;
        this.text = text;
    }

    public Field getField() {
        return field;
    }

    public String getText() {
        return text;
    }

    public static Optional<IntegrityIssue> fromField(Field field) {
        return Arrays.stream(IntegrityIssue.values())
                     .filter(symbol -> symbol.getField().equals(field))
                     .findFirst();
    }
}
