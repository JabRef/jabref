package org.jabref.logic.integrity;

import java.util.Arrays;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;

/**
 * Represents various integrity issues that can occur in a bibliography entry.
 * Each issue has an associated message describing the problem, and some may provide
 * a suggested fix.
 */
public enum IntegrityIssue {
    ABBREVIATION_DETECTED(Localization.lang("abbreviation detected")),
    BIBLATEX_FIELD_ONLY(Localization.lang("biblatex field only")),
    BIBTEX_FIELD_ONLY(Localization.lang("BibTeX field only")),
    BOOKTITLE_ENDS_WITH_CONFERENCE_ON(Localization.lang("booktitle ends with 'conference on'")),
    CAPITAL_LETTER_ARE_NOT_MASKED_USING_CURLY_BRACKETS(Localization.lang("capital letters are not masked using curly brackets {}"), Localization.lang("Mask letters")),
    CITATION_KEY_DEVIATES_FROM_GENERATED_KEY(Localization.lang("Citation key deviates from generated key")),
    DOI_IS_INVALID(Localization.lang("DOI is invalid")),
    DUPLICATE_CITATION_KEY(Localization.lang("Duplicate citation key")),
    EMPTY_CITATION_KEY(Localization.lang("empty citation key")),
    ENTRY_TYPE_IS_ONLY_DEFINED_FOR_BIBLATEX_BUT_NOT_FOR_BIBTEX(Localization.lang("Entry type %0 is only defined for Biblatex but not for BibTeX")),
    HTML_ENCODED_CHARACTER_FOUND(Localization.lang("HTML encoded character found")),
    INCORRECT_CONTROL_DIGIT(Localization.lang("incorrect control digit")),
    INCORRECT_FORMAT(Localization.lang("incorrect format")),
    INVALID_CITATION_KEY(Localization.lang("Invalid citation key")),
    JOURNAL_NOT_FOUND_IN_ABBREVIATION_LIST(Localization.lang("journal not found in abbreviation list")),
    LAST_FOUR_NONPUNCTUATION_CHARACTERS_SHOULD_BE_NUMERALS(Localization.lang("last four nonpunctuation characters should be numerals")),
    LINK_SHOULD_REFER_TO_A_CORRECT_FILE_PATH(Localization.lang("link should refer to a correct file path")),
    NAMES_ARE_NOT_IN_THE_STANDARD_FORMAT(Localization.lang("Names are not in the standard %0 format.")),
    NON_ASCII_ENCODED_CHARACTER_FOUND(Localization.lang("Non-ASCII encoded character found")),
    NO_INTEGER_AS_VALUE_FOR_EDITION_ALLOWED(Localization.lang("no integer as values for edition allowed")),
    ODD_NUMBER_OF_UNESCAPED(Localization.lang("odd number of unescaped '#'")),
    REFERENCED_CITATION_KEY_DOES_NOT_EXIST(Localization.lang("Referenced citation key '%0' does not exist")),
    SAME_DOI_USED_IN_MULTIPLE_ENTRIES(Localization.lang("Same DOI used in multiple entries")),
    SHOULD_BE_AN_INTEGER_OR_NORMALIZED(Localization.lang("should be an integer or normalized")),
    SHOULD_BE_NORMALIZED(Localization.lang("should be normalized")),
    SHOULD_CONTAIN_A_FOUR_DIGIT_NUMBER(Localization.lang("should contain a four digit number")),
    SHOULD_CONTAIN_A_VALID_PAGE_NUMBER_RANGE(Localization.lang("should contain a valid page number range")),
    SHOULD_CONTAIN_AN_INTEGER_OR_A_LITERAL(Localization.lang("should contain an integer or a literal")),
    SHOULD_HAVE_THE_FIRST_CHARACTER_CAPITALIZED(Localization.lang("should have the first letter capitalized")),
    SHOULD_HAVE_THE_FIRST_LETTER_CAPITALIZED(Localization.lang("should have the first letter capitalized")),
    UNEXPECTED_CLOSING_CURLY_BRACKET(Localization.lang("unexpected closing curly bracket")),
    UNEXPECTED_OPENING_CURLY_BRACKET(Localization.lang("unexpected opening curly bracket")),
    VALUE_IS_NOT_IN_UNICODES_NORMALIZATION_FORM_CANONICAL_COMPOSITION_NFC_FORMAT(Localization.lang("Value is not in Unicode's Normalization Form \"Canonical Composition\" (NFC) format"));

    private final String text;
    private final String fix;

    IntegrityIssue(String text, String fix) {
        this.text = text;
        this.fix = fix;
    }

    IntegrityIssue(String text) {
        this.text = text;
        this.fix = null;
    }

    public String getText() {
        return text;
    }

    public String getText(Object... args) {
        return text.replace("%0", "%s").formatted(args);
    }

    public Optional<String> getFix() {
        return Optional.ofNullable(fix);
    }

    public static Optional<IntegrityIssue> fromMessage(IntegrityMessage message) {
        return Arrays.stream(IntegrityIssue.values())
                     .filter(symbol -> symbol.getText().equals(message.message()))
                     .findFirst();
    }
}
