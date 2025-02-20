package org.jabref.gui.integrity;

import java.util.Arrays;
import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

public enum IntegrityIssue {
    CITATION_KEY_DEVIATES_FROM_GENERATED_KEY(InternalField.KEY_FIELD, "citation key deviates from generated key", "Generate Citation Key"),
    BIBTEX_FIELD_ONLY_KEY(InternalField.KEY_FIELD, "bibtex field only", "Remove Field"),
    BIBTEX_FIELD_ONLY_CROSS_REF(StandardField.CROSSREF, "bibtex field only", "Remove Field"),
    INCORRECT_FORMAT_DATE(StandardField.DATE, "incorrect format"),
    INCORRECT_FORMAT_ISSN(StandardField.ISSN, "incorrect format"),
    INCORRECT_CONTROL_DIGIT_ISSN(StandardField.ISSN, "incorrect control digit"),
    INCORRECT_FORMAT_ISBN(StandardField.ISBN, "incorrect format"),
    INCORRECT_CONTROL_DIGIT_ISBN(StandardField.ISBN, "incorrect control digit"),
    JOURNAL_NOT_FOUND_IN_ABBREVIATION_LIST(StandardField.JOURNAL, "journal not found in the abbreviation list"),
    NO_INTEGER_AS_VALUE_FOR_EDITION_ALLOWED(StandardField.EDITION, "no integer as value for edition allowed"),
    SHOULD_CONTAIN_AN_INTEGER_OR_A_LITERAL(StandardField.EDITION, "should contain an integer or a literal"),
    SHOULD_HAVE_THE_FIRST_LETTER_CAPITALIZED(StandardField.EDITION, "should have the first letter capitalized"),
    REFERENCED_CITATION_KEY_DOES_NOT_EXIST(StandardField.CROSSREF, "referenced citation key does ont exist"),
    NON_ASCII_ENCODED_CHARACTER_FOUND(StandardField.ABSTRACT, "Non-Ascii encoded character found"),
    SHOULD_CONTAIN_A_VALID_PAGE_NUMBER_RANGE(StandardField.PAGES, "should contain a valid page number range"),
    CAPITAL_LETTER_ARE_NOT_MASKED_USING_CURLY_BRACKETS(StandardField.TITLE, "capital letters are not masked using curly brackets {}", "Mask letters");

    private final Field field;
    private final String text;
    private final String fix;

    IntegrityIssue(Field field, String text, String fix) {
        this.field = field;
        this.text = text;
        this.fix = fix;
    }

    IntegrityIssue(Field field, String text) {
        this.field = field;
        this.text = text;
        this.fix = null;
    }

    public Field getField() {
        return field;
    }

    public String getText() {
        return text;
    }

    public String getFix() {
        return fix;
    }

    public static Optional<IntegrityIssue> fromField(Field field) {
        return Arrays.stream(IntegrityIssue.values())
                     .filter(symbol -> symbol.getField().equals(field))
                     .findFirst();
    }
}
