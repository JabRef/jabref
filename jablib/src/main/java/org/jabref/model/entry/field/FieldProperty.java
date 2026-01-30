package org.jabref.model.entry.field;

/// @implNote Introduce a new FieldProperty only if multiple fields with the same property exist.
/// For instance, "gender" exists only in field "Gender", whereas "identifier" is the property of multiple fields.
/// It is confusing to have a FieldProperty for a single field.
/// We accept that some developers might be confused for different handling at [org.jabref.logic.integrity.FieldCheckers#getForField(Field)].
public enum FieldProperty {
    BOOK_NAME,
    DATE,
    EDITOR_TYPE,
    EXTERNAL,
    FILE_EDITOR,
    JOURNAL_NAME,

    // globally unique identifier for the concrete article
    IDENTIFIER,

    LANGUAGE,

    // Field content is text, but should be interpreted as markdown
    // AKA: Field content is not LaTeX
    MARKDOWN,

    MONTH,

    MULTILINE_TEXT,
    NUMERIC,
    PAGINATION,
    PERSON_NAMES,

    SINGLE_ENTRY_LINK,
    MULTIPLE_ENTRY_LINK,

    // Field content should be treated as data
    VERBATIM,

    // Contains a year value
    YEAR,

    YES_NO
}
