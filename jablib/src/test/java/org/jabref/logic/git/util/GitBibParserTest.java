package org.jabref.logic.git.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitBibParserTest {
    private Git git;
    private Path library;
    private RevCommit commit;

    private final PersonIdent alice = new PersonIdent("Alice", "alice@example.org");
    private final String bibContent = """
        @article{test2025,
          author = {Alice},
          title = {Test Title},
          year = {2025}
        }
        """;

    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        git = Git.init()
                 .setDirectory(tempDir.toFile())
                 .setInitialBranch("main")
                 .call();

        library = tempDir.resolve("library.bib");
        commit = writeAndCommit(bibContent, "Initial commit", alice, library, git);
    }

    @Test
    void parsesBibContentFromCommit() throws Exception {
        String rawBib = GitFileReader.readFileFromCommit(git, commit, Path.of("library.bib"));

        BibDatabaseContext context = GitBibParser.parseBibFromGit(rawBib, importFormatPreferences);

        List<BibEntry> entries = context.getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.get(0);
        assertEquals(Optional.of("Alice"), entry.getField(FieldFactory.parseField("author")));
        assertEquals(Optional.of("Test Title"), entry.getField(FieldFactory.parseField("title")));
        assertEquals(Optional.of("2025"), entry.getField(FieldFactory.parseField("year")));
        assertEquals(Optional.of("test2025"), entry.getCitationKey());
    }

    private RevCommit writeAndCommit(String content, String message, PersonIdent author, Path library, Git git) throws Exception {
        Files.writeString(library, content, StandardCharsets.UTF_8);
        git.add().addFilepattern(library.getFileName().toString()).call();
        return git.commit().setAuthor(author).setMessage(message).call();
    }
}
