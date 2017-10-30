package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoSetFileLinksUtilTest {

    private final FileDirectoryPreferences fileDirPrefs = mock(FileDirectoryPreferences.class);
    private final AutoLinkPreferences autoLinkPrefs = new AutoLinkPreferences(false, "", true, ';');
    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final ExternalFileTypes externalFileTypes = mock(ExternalFileTypes.class);
    private final BibEntry entry = new BibEntry("article");
    private Path file;

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        entry.setCiteKey("CiteKey");
        file = folder.newFile("CiteKey.pdf").toPath();
        when(databaseContext.getFileDirectoriesAsPaths(any())).thenReturn(Collections.singletonList(folder.getRoot().toPath()));
        when(externalFileTypes.getExternalFileTypeSelection()).thenReturn(new TreeSet<>(externalFileTypes.getDefaultExternalFileTypes()));

    }

    @Test
    public void test() {
        //Due to mocking the externalFileType class, the file extension will not be found
        List<LinkedFile> expected = Collections.singletonList(new LinkedFile("", file.toString(), ""));

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil();
        List<LinkedFile> actual = util.findassociatedNotLinkedFiles(entry, databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        assertEquals(expected, actual);
    }

}
