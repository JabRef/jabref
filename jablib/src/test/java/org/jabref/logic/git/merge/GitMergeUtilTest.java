package org.jabref.logic.git.merge;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitMergeUtilTest {
    @Test
    void replaceEntriesReplacesMatchingByCitationKey() {
        BibEntry entryA = new BibEntry(StandardEntryType.Article)
                .withCitationKey("keyA")
                .withField(StandardField.AUTHOR, "original author A");

        BibEntry entryB = new BibEntry(StandardEntryType.Book)
                .withCitationKey("keyB")
                .withField(StandardField.AUTHOR, "original author B");

        BibDatabase originalDatabase = new BibDatabase();
        originalDatabase.insertEntry(entryA);
        originalDatabase.insertEntry(entryB);
        BibDatabaseContext remoteContext = new BibDatabaseContext(originalDatabase, new MetaData());

        BibEntry resolvedA = new BibEntry(StandardEntryType.Article)
                .withCitationKey("keyA")
                .withField(StandardField.AUTHOR, "resolved author A");

        BibDatabaseContext result = GitMergeUtil.replaceEntries(remoteContext, List.of(resolvedA));

        List<BibEntry> finalEntries = result.getDatabase().getEntries();
        BibEntry resultA = finalEntries.stream().filter(e -> "keyA".equals(e.getCitationKey().orElse(""))).findFirst().orElseThrow();
        BibEntry resultB = finalEntries.stream().filter(e -> "keyB".equals(e.getCitationKey().orElse(""))).findFirst().orElseThrow();

        assertEquals("resolved author A", resultA.getField(StandardField.AUTHOR).orElse(""));
        assertEquals("original author B", resultB.getField(StandardField.AUTHOR).orElse(""));
    }

    @Test
    void replaceEntriesIgnoresResolvedWithoutCitationKey() {
        BibEntry original = new BibEntry(StandardEntryType.Article)
                .withCitationKey("key1")
                .withField(StandardField.TITLE, "Original Title");

        BibDatabaseContext remote = new BibDatabaseContext();
        remote.getDatabase().insertEntry(original);

        // Resolved entry without citation key (invalid)
        BibEntry resolved = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "New Title");

        BibDatabaseContext result = GitMergeUtil.replaceEntries(remote, List.of(resolved));
        assertEquals("Original Title", result.getDatabase().getEntries().getFirst().getField(StandardField.TITLE).orElse(""));
    }
}
