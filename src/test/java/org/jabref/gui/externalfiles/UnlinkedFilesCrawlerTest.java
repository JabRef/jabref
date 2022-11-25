package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnlinkedFilesCrawlerTest {

    @Test
    public void minimalGitIgnore(@TempDir Path testRoot) throws Exception {
        Files.writeString(testRoot.resolve(".gitignore"), """
                *.png
                """);
        Path subDir = testRoot.resolve("subdir");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("test.png"));

        UnlinkedPDFFileFilter unlinkedPDFFileFilter = mock(UnlinkedPDFFileFilter.class);
        when(unlinkedPDFFileFilter.accept(any(Path.class))).thenReturn(true);

        UnlinkedFilesCrawler unlinkedFilesCrawler = new UnlinkedFilesCrawler(testRoot, unlinkedPDFFileFilter, DateRange.ALL_TIME, ExternalFileSorter.DEFAULT, mock(BibDatabaseContext.class), mock(FilePreferences.class));

        FileNodeViewModel fileNodeViewModel = unlinkedFilesCrawler.searchDirectory(testRoot, unlinkedPDFFileFilter);

        assertEquals(new FileNodeViewModel(testRoot), fileNodeViewModel);
    }
}
