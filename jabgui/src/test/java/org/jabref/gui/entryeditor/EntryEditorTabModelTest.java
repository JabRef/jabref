package org.jabref.gui.entryeditor;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryEditorTabModelTest {

    private final BibEntry entry = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.AUTHOR, "Author")
            .withField(StandardField.COMMENT, "general comment")
            .withField(new UserSpecificCommentField("alice"), "alice's comment")
            .withField(new UserSpecificCommentField("bob"), "bob's comment");

    private static List<Field> resolve(BibEntry entry, String... patterns) {
        return List.copyOf(new EntryEditorTabModel.CustomizedFieldsTab("Test", List.of(patterns)).resolveFields(entry));
    }

    @Test
    void plainFieldNamesAreAlwaysShownEvenWhenUnset() {
        assertEquals(List.of(StandardField.AUTHOR, StandardField.URL), resolve(entry, "author", "url"));
    }

    @Test
    void unknownPlainFieldNameResolvesToUnknownField() {
        assertEquals(List.of(new UnknownField("myfield")), resolve(entry, "myfield"));
    }

    @Test
    void regexPatternCapturesMatchingSetFields() {
        assertEquals(
                List.of(new UserSpecificCommentField("alice"), new UserSpecificCommentField("bob")),
                resolve(entry, "comment-.*"));
    }

    @Test
    void patternOrderIsKept() {
        assertEquals(
                List.of(StandardField.AUTHOR, new UserSpecificCommentField("alice"), new UserSpecificCommentField("bob"), StandardField.COMMENT),
                resolve(entry, "author", "comment-.*", "comment"));
    }

    @Test
    void invalidRegexResolvesToNothing() {
        assertEquals(List.of(StandardField.AUTHOR), resolve(entry, "author", "comment-["));
    }
}
