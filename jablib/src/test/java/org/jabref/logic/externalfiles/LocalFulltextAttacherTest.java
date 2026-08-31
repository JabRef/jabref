package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalFulltextAttacherTest {

    @TempDir
    private Path tempDir;

    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TaskExecutor taskExecutor = new CurrentThreadTaskExecutor();
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
        // No file directory is configured, so copyOrMoveToDefaultDirectory returns false without throwing.
        when(filePreferences.getUserAndHost()).thenReturn("");
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
    }

    @Test
    void doesNotAttachWhenFileCannotBeMovedIntoLibraryDirectory() throws IOException {
        Path pdf = tempDir.resolve("fulltext.pdf");
        Files.createFile(pdf);
        URL fileUrl = pdf.toUri().toURL();
        BibEntry entry = new BibEntry().withCitationKey("Smith2024");

        LocalFulltextAttacher.attach(fileUrl, entry, databaseContext, filePreferences, taskExecutor, notificationService);

        assertEquals(List.of(), entry.getFiles());
        verify(notificationService).notify(anyString());
    }
}
