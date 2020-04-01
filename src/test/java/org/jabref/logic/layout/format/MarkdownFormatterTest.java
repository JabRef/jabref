package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFormatterTest {

    private MarkdownFormatter markdownFormatter;

    @BeforeEach
    void setUp() {
        markdownFormatter = new MarkdownFormatter();
    }

    @Test
    void formatWhenFormattingPlainTextThenReturnsTextWrappedInParagraph() {
        assertEquals("<p>Hello World</p>", markdownFormatter.format("Hello World"));
    }

    @Test
    void formatWhenFormattingComplexMarkupThenReturnsOnlyOneLine() {
        assertFalse(markdownFormatter.format("Markup\n\n* list item one\n* list item 2\n\n rest").contains("\n"));
    }

    @Test
    void formatWhenFormattingEmptyStringThenReturnsEmptyString() {
        assertEquals("", markdownFormatter.format(""));
    }

    @Test
    void formatWhenFormattingNullThenThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> markdownFormatter.format(null));
        assertEquals("Field Text should not be null, when handed to formatter", exception.getMessage());
    }

    @Test
    void formatWhenMarkupContainingStrikethroughThenContainsMatchingDel() {
        // Only test strikethrough extension
        assertTrue(markdownFormatter.format("a ~~b~~ b").contains("<del>b</del>"));
    }

    @Test
    void formatWhenMarkupContainingTaskListThenContainsFormattedTaskList() {
        String actual = markdownFormatter.format("Some text\n" +
                "* [ ] open task\n" +
                "* [x] closed task\n\n" +
                "some other text");
        // Only test list items
        assertTrue(actual.contains("<li class=\"task-list-item\"><input type=\"checkbox\" class=\"task-list-item-checkbox\" disabled=\"disabled\" readonly=\"readonly\" />&nbsp;open task</li>"));
        assertTrue(actual.contains("<li class=\"task-list-item\"><input type=\"checkbox\" class=\"task-list-item-checkbox\" checked=\"checked\" disabled=\"disabled\" readonly=\"readonly\" />&nbsp;closed task</li>"));
    }
}
