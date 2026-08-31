package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryEditorPreferencesTest {

    @Test
    void fieldsOnCustomTabsCoverPlainNamesAndResolvedRegexes() {
        EntryEditorPreferences preferences = EntryEditorPreferences.getDefault();
        preferences.getTabModels().add(new EntryEditorTabModel.CustomizedFieldsTab("Mine", List.of("abstract", "comment-.*")));
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Author")
                .withField(new UserSpecificCommentField("alice"), "note");

        assertEquals(Set.of(StandardField.ABSTRACT, new UserSpecificCommentField("alice")),
                preferences.getFieldsOnCustomTabs(entry));
    }
}
