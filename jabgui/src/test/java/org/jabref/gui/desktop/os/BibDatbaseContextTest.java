package org.jabref.gui.desktop.os;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BibDatbaseContextTest {

    private FilePreferences fileDirPrefs;

    @BeforeEach
    void setUp() {
        fileDirPrefs = mock(FilePreferences.class);
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
    }

    @Test
    void getUserFileDirectoryIfAllAreEmpty() {
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        Path userDirJabRef = NativeDesktop.get().getDefaultFileChooserDirectory();

        when(fileDirPrefs.getMainFileDirectory()).thenReturn(Optional.of(userDirJabRef));
        BibDatabaseContext database = new BibDatabaseContext.Builder().build();
        database.setDatabasePath(Path.of("biblio.bib"));
        assertEquals(List.of(userDirJabRef), database.getFileDirectories(fileDirPrefs));
    }
}
