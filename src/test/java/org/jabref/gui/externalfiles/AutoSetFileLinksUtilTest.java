package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoSetFileLinksUtilTest {

    private final FilePreferences fileDirPrefs = mock(FilePreferences.class);
    private final AutoLinkPreferences autoLinkPrefs = new AutoLinkPreferences(
            AutoLinkPreferences.CitationKeyDependency.START,
            "",
            false,
            ';');
    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final ExternalFileTypes externalFileTypes = mock(ExternalFileTypes.class);
    private final BibEntry entry = new BibEntry(StandardEntryType.Article);
    private Path path = null;

    @BeforeEach
    public void setUp(@TempDir Path folder) throws Exception {
        path = folder.resolve("CiteKey.pdf");
        Files.createFile(path);
        entry.setCitationKey("CiteKey");
        when(externalFileTypes.getExternalFileTypeSelection()).thenReturn(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes()));
    }

    @Test
    public void testFindAssociatedNotLinkedFilesSuccess() throws Exception {
        // Due to mocking the externalFileType class, the file extension will not be found
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(path.getParent()));
        List<LinkedFile> expected = Collections.singletonList(new LinkedFile("", Path.of("CiteKey.pdf"), ""));
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(expected, actual);
    }

    @Test
    public void testFindAssociatedNotLinkedFilesForEmptySearchDir() throws Exception {
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(Collections.emptyList(), actual);
    }
}
