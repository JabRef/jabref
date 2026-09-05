package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Set;

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

    @Test
    void fieldsOnCustomTabsUnitesCustomTabsAndIgnoresBuiltInTabs() {
        List<EntryEditorTabModel> tabModels = List.of(
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true),
                new EntryEditorTabModel.CustomizedFieldsTab("One", List.of("author", "url")),
                new EntryEditorTabModel.CustomizedFieldsTab("Two", List.of("comment-.*")));
        assertEquals(
                Set.of(StandardField.AUTHOR, StandardField.URL,
                        new UserSpecificCommentField("alice"), new UserSpecificCommentField("bob")),
                EntryEditorTabModel.fieldsOnCustomTabs(tabModels, entry));
    }

    @Test
    void regexCapturesFieldOnlyOnceItHasAValue() {
        List<EntryEditorTabModel> tabModels = List.of(
                new EntryEditorTabModel.CustomizedFieldsTab("Notes", List.of("note.*")));
        BibEntry withoutNote = new BibEntry(StandardEntryType.Article);
        assertEquals(Set.of(), EntryEditorTabModel.fieldsOnCustomTabs(tabModels, withoutNote));
        // First typed character sets the field; from then on it belongs to the custom tab
        // (and the Main tab must drop it, even when it was chip-added there).
        BibEntry withNote = new BibEntry(StandardEntryType.Article).withField(StandardField.NOTE, "x");
        assertEquals(Set.of(StandardField.NOTE), EntryEditorTabModel.fieldsOnCustomTabs(tabModels, withNote));
    }
}
