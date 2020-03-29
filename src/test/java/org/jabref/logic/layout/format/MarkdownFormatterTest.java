package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MarkdownFormatterTest {

    private MarkdownFormatter markdownFormatter;

    @BeforeEach
    void setUp() {
        markdownFormatter = new MarkdownFormatter();
    }

    @Test
    void formatWhenFormattingPlainTextThenReturnsTextWrappedInParagraph() {
        assertThat(markdownFormatter.format("Hello World")).isEqualTo("<p>Hello World</p>");
    }

    @Test
    void formatWhenFormattingComplexMarkupThenReturnsOnlyOneLine() {
        String source = "Markup\n\n* list item one\n* list item 2\n\n rest";
        assertThat(markdownFormatter.format(source))
                .contains("Markup<br />")
                .contains("<li>list item one</li>")
                .contains("<li>list item 2</li>")
                .contains("> rest")
                .doesNotContain("\n");
    }

    @Test
    void formatWhenFormattingEmptyStringThenReturnsEmptyString() {
        assertThat(markdownFormatter.format("")).isEqualTo("");
    }

    @Test
    void formatWhenFormattingNullThenThrowsException() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> markdownFormatter.format(null))
                                                             .withMessageContaining("Field Text should not be null, when handed to formatter")
                                                             .withNoCause();
    }

    @Test
    void formatWhenMarkupContainingStrikethroughThenContainsMatchingDel() {
        assertThat(markdownFormatter.format("a ~~b~~ b")).contains("<del>b</del>");
    }

    @Test
    void formatWhenMarkupContainingTaskListThenContainsFormattedTaskList() {
        assertThat(markdownFormatter.format("Some text\n" +
                "* [ ] open task\n" +
                "* [x] closed task\n\n" +
                "some other text"))
                .contains("<li class=\"task-list-item\"><input type=\"checkbox\" class=\"task-list-item-checkbox\" disabled=\"disabled\" readonly=\"readonly\" />&nbsp;open task</li>")
                .contains("<li class=\"task-list-item\"><input type=\"checkbox\" class=\"task-list-item-checkbox\" checked=\"checked\" disabled=\"disabled\" readonly=\"readonly\" />&nbsp;closed task</li>");
    }
}
