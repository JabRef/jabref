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
import org.jabref.model.metadata.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoSetFileLinksUtilTest {

    private final FilePreferences fileDirPrefs = mock(FilePreferences.class);
    private final AutoLinkPreferences autoLinkPrefs = new AutoLinkPreferences(false, "", true, ';');
    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final ExternalFileTypes externalFileTypes = mock(ExternalFileTypes.class);
    private final BibEntry entry = new BibEntry(StandardEntryType.Article);

    @BeforeEach
    public void setUp(@TempDir Path folder) throws Exception {
        Path path = folder.resolve("CiteKey.pdf");
        Files.createFile(path);
        entry.setCiteKey("CiteKey");
        when(databaseContext.getFileDirectoriesAsPaths(any())).thenReturn(Collections.singletonList(path.getParent()));
        when(externalFileTypes.getExternalFileTypeSelection()).thenReturn(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes()));
    }

    @Test
    public void test() throws Exception {
        //Due to mocking the externalFileType class, the file extension will not be found

        List<LinkedFile> expected = Collections.singletonList(new LinkedFile("", "CiteKey.pdf", ""));

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        List<LinkedFile> actual = util.findAssociatedNotLinkedFiles(entry);
        assertEquals(expected, actual);
    }
}
