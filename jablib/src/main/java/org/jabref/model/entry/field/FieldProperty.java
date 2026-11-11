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

    /// Enables numeric-aware sorting and causes JabRef to treat the field as non-LaTeX content.
    /// This is a JabRef property, not a BibLaTeX datatype: `edition` accepts integers or
    /// literal values, and `number` and `pagetotal` are literal fields. See the [BibLaTeX
    /// manual, section 2.2.2](https://mirrors.ctan.org/macros/latex/contrib/biblatex/doc/biblatex.pdf).
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
