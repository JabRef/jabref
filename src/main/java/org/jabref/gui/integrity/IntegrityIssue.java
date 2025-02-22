package org.jabref.gui.integrity;

import java.util.Arrays;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

public enum IntegrityIssue {
    CITATION_KEY_DEVIATES_FROM_GENERATED_KEY(InternalField.KEY_FIELD, Localization.lang("Citation key deviates from generated key")),
    BIBTEX_FIELD_ONLY_KEY(InternalField.KEY_FIELD, Localization.lang("bibtex field only"), Localization.lang("Remove field")),
    BIBTEX_FIELD_ONLY_CROSS_REF(StandardField.CROSSREF, Localization.lang("bibtex field only"), Localization.lang("Remove field")),
    INCORRECT_FORMAT_DATE(StandardField.DATE, Localization.lang("incorrect format")),
    INCORRECT_FORMAT_ISSN(StandardField.ISSN, Localization.lang("incorrect format")),
    INCORRECT_CONTROL_DIGIT_ISSN(StandardField.ISSN, Localization.lang("incorrect control digit")),
    INCORRECT_FORMAT_ISBN(StandardField.ISBN, Localization.lang("incorrect format")),
    INCORRECT_CONTROL_DIGIT_ISBN(StandardField.ISBN, Localization.lang("incorrect control digit")),
    JOURNAL_NOT_FOUND_IN_ABBREVIATION_LIST(StandardField.JOURNAL, Localization.lang("journal not found in abbreviation list")),
    NO_INTEGER_AS_VALUE_FOR_EDITION_ALLOWED(StandardField.EDITION, Localization.lang("no integer as values for edition allowed")),
    SHOULD_CONTAIN_AN_INTEGER_OR_A_LITERAL(StandardField.EDITION, Localization.lang("should contain an integer or a literal")),
    SHOULD_HAVE_THE_FIRST_LETTER_CAPITALIZED(StandardField.EDITION, Localization.lang("should have the first letter capitalized")),
    REFERENCED_CITATION_KEY_DOES_NOT_EXIST(StandardField.CROSSREF, Localization.lang("Referenced citation key does not exist")),
    NON_ASCII_ENCODED_CHARACTER_FOUND(StandardField.ABSTRACT, Localization.lang("Non-ASCII encoded character found")),
    SHOULD_CONTAIN_A_VALID_PAGE_NUMBER_RANGE(StandardField.PAGES, Localization.lang("should contain a valid page number range")),
    CAPITAL_LETTER_ARE_NOT_MASKED_USING_CURLY_BRACKETS(StandardField.TITLE, Localization.lang("capital letters are not masked using curly brackets {}"), Localization.lang("Mask letters"));

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
