package org.jabref.logic.git.diff;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitCommitMessageGeneratorTest {

    @Test
    void unchangedLibraryGeneratesNoMessage() {
        Optional<String> message = generate(
                List.of(article("Smith2024", "Smith", "Git basics")),
                List.of(article("Smith2024", "Smith", "Git basics")));

        assertEquals(Optional.empty(), message);
    }

    @Test
    void addedEntriesAreCounted() {
        Optional<String> message = generate(
                List.of(),
                List.of(article("Brown2022", "Brown", "Branching"),
                        article("Lee2021", "Lee", "Rebasing"),
                        article("Kim2020", "Kim", "Tagging")));

        assertEquals(Optional.of("Add 3 entries"), message);
    }

    // [utest->req~git.commit.generated-message~1]
    @Test
    void addedAndModifiedEntriesAreSummarized() {
        Optional<String> message = generate(
                List.of(article("Smith2024", "Smith", "Git basics"),
                        article("Jones2023", "Jones", "Merging")),
                List.of(article("Smith2024", "Smith", "Git basics, second edition"),
                        article("Jones2023", "Jones", "Merging libraries"),
                        article("Brown2022", "Brown", "Branching"),
                        article("Lee2021", "Lee", "Rebasing"),
                        article("Kim2020", "Kim", "Tagging")));

        assertEquals(Optional.of("Add 3 entries, modify 2 entries"), message);
    }

    @Test
    void singleDeletedEntryUsesSingular() {
        Optional<String> message = generate(
                List.of(article("Smith2024", "Smith", "Git basics"),
                        article("Jones2023", "Jones", "Merging")),
                List.of(article("Smith2024", "Smith", "Git basics")));

        assertEquals(Optional.of("Delete 1 entry"), message);
    }

    @Test
    void deletingAllEntriesIsReported() {
        Optional<String> message = generate(
                List.of(article("Smith2024", "Smith", "Git basics"),
                        article("Jones2023", "Jones", "Merging")),
                List.of());

        assertEquals(Optional.of("Delete 2 entries"), message);
    }

    // [utest->req~git.commit.generated-message~1]
    @Test
    void singleChangedFieldIsNamed() {
        Optional<String> message = generate(
                List.of(article("Smith2024", "Smith", "Git basics")),
                List.of(article("Smith2024", "Smith, John", "Git basics")));

        assertEquals(Optional.of("Modify author of Smith2024"), message);
    }

    @Test
    void severalChangedFieldsOfOneEntryAreCounted() {
        Optional<String> message = generate(
                List.of(article("Smith2024", "Smith", "Git basics")),
                List.of(article("Smith2024", "Smith, John", "Git basics, second edition")));

        assertEquals(Optional.of("Modify 1 entry"), message);
    }

    @Test
    void addingEntryToGroupNamesGroupsField() {
        Optional<String> message = generate(
                List.of(article("Smith2024", "Smith", "Git basics")),
                List.of(article("Smith2024", "Smith", "Git basics").withField(StandardField.GROUPS, "To read")));

        assertEquals(Optional.of("Modify groups of Smith2024"), message);
    }

    // [utest->req~git.commit.generated-message~1]
    @Test
    void changedPreambleAloneGeneratesNoMessage() {
        BibDatabase workingTreeDatabase = new BibDatabase(List.of(article("Smith2024", "Smith", "Git basics")));
        workingTreeDatabase.setPreamble("Preamble added after the last commit");

        Optional<String> message = GitCommitMessageGenerator.generate(
                new BibDatabaseContext(new BibDatabase(List.of(article("Smith2024", "Smith", "Git basics")))),
                new BibDatabaseContext(workingTreeDatabase));

        assertEquals(Optional.empty(), message);
    }

    @Test
    void renamedCitationKeyNamesOldAndNewKey() {
        Optional<String> message = generate(
                List.of(detailedArticle("Jones2023")),
                List.of(detailedArticle("Jones2024")));

        assertEquals(Optional.of("Rename citation key Jones2023 to Jones2024"), message);
    }

    @Test
    void addedCitationKeyIsCountedAsModification() {
        Optional<String> message = generate(
                List.of(detailedArticleWithoutKey()),
                List.of(detailedArticle("Jones2024")));

        assertEquals(Optional.of("Modify 1 entry"), message);
    }

    @Test
    void removedCitationKeyIsCountedAsModification() {
        Optional<String> message = generate(
                List.of(detailedArticle("Jones2023")),
                List.of(detailedArticleWithoutKey()));

        assertEquals(Optional.of("Modify 1 entry"), message);
    }

    private static Optional<String> generate(List<BibEntry> headEntries, List<BibEntry> workingTreeEntries) {
        return GitCommitMessageGenerator.generate(
                new BibDatabaseContext(new BibDatabase(headEntries)),
                new BibDatabaseContext(new BibDatabase(workingTreeEntries)));
    }

    private static BibEntry article(String citationKey, String author, String title) {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey(citationKey)
                .withField(StandardField.AUTHOR, author)
                .withField(StandardField.TITLE, title);
    }

    /// Has enough fields that changing only the citation key still scores
    /// above `BibDatabaseDiff`'s match threshold
    private static BibEntry detailedArticle(String citationKey) {
        return detailedArticleWithoutKey().withCitationKey(citationKey);
    }

    private static BibEntry detailedArticleWithoutKey() {
        return new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Jones")
                .withField(StandardField.TITLE, "Merging")
                .withField(StandardField.JOURNAL, "Journal of Version Control")
                .withField(StandardField.YEAR, "2023");
    }
}
