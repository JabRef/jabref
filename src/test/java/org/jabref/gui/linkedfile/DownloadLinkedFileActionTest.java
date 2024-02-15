package org.jabref.gui.linkedfile;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadLinkedFileActionTest {
    private BibEntry entry;

    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final PreferencesService preferences = mock(PreferencesService.class);

    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws Exception {
        entry = new BibEntry()
                .withCitationKey("asdf");

        when(filePreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));
        Path tempFile = tempFolder.resolve("temporaryFile");
        Files.createFile(tempFile);

        // Check if there exists a system-wide cookie handler
        CookieManager cookieManager;
        if (CookieHandler.getDefault() == null) {
            cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);
        } else {
            cookieManager = (CookieManager) CookieHandler.getDefault();
        }
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    @Test
    void replacesLinkedFiles(@TempDir Path tempFolder) throws Exception {
        String url = "http://arxiv.org/pdf/1207.0408v1";

        LinkedFile linkedFile = new LinkedFile(new URL(url), "");
        when(databaseContext.getFirstExistingFileDir(any())).thenReturn(Optional.of(tempFolder));
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");

        DownloadLinkedFileAction downloadLinkedFileAction = new DownloadLinkedFileAction(
                databaseContext,
                entry,
                linkedFile,
                linkedFile.getLink(),
                dialogService,
                preferences.getFilePreferences(),
                new CurrentThreadTaskExecutor());
        downloadLinkedFileAction.execute();

        assertEquals(List.of(new LinkedFile("", tempFolder.resolve("asdf.pdf"), "PDF", url)), entry.getFiles());
    }
}
