package org.jabref.logic.openoffice.style;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/// Built-in Writer paragraph-style identifiers for bibliography formatting, independent of citation style families.
@NullMarked
public final class WriterParagraphStyles {

    /// The formatting names specified to and understood by LibreOffice internally via code are often different from their displayed names in the LibreOffice application.
    /// For example, "Hanging Indent" as visible in LO styles list should be specified as "Hanging indent" (note casing) internally. Similarly, "Body Text, Indented" is specified as "Text body indent".
    /// This is also why we don't let users manually input the name of any pre-set format.
    /// The following two lists below are taken from https://github.com/LibreOffice/core/blob/0891df6b21fd95ec7c9614509d92829c0f17c353/sw/qa/python/check_styles.py#L132
    public static final List<String> BIBLIOGRAPHY_TITLE_FORMATS = List.of(
            "Bibliography Heading",
            "Title",
            "Text body",
            "Subtitle",
            "Heading 1",
            "Heading 2",
            "Heading 3",
            "Heading 4"
    );

    public static final List<String> BIBLIOGRAPHY_BODY_FORMATS = List.of(
            "Bibliography 1",
            "Text body",
            "Text body indent",
            "First line indent",
            "Hanging indent",
            "List",
            "List Indent",
            "Marginalia"
    );

    public static final String DEFAULT_BIBLIOGRAPHY_BODY_FORMAT = "Text body";
    public static final String DEFAULT_HANGING_INDENT_BIBLIOGRAPHY_BODY_FORMAT = "Hanging indent";

    private WriterParagraphStyles() {
    }
}
