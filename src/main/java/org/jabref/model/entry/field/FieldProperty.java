package org.jabref.model.entry.field;

/**
 * @implNote Introduce a new FieldProperty only if multiple fields with the same property exist. For instance, "gender" exists only in field "Gender", whereas "identifier" is the property of multiple fields
 */
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

    YES_NO
}
