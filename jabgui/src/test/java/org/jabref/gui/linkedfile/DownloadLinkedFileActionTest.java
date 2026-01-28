package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadLinkedFileActionTest {

    @TempDir
    private Path tempFolder;

    private BibEntry entry;

    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);

    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws IOException {
        entry = new BibEntry()
                .withCitationKey("asdf");

        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
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
    void replacesLinkedFiles(@TempDir Path tempFolder) throws MalformedURLException {
        String url = "http://arxiv.org/pdf/1207.0408v1";

        LinkedFile linkedFile = new LinkedFile(URLUtil.create(url), "");
        when(databaseContext.getFirstExistingFileDir(any())).thenReturn(Optional.of(tempFolder));
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(filePreferences.shouldKeepDownloadUrl()).thenReturn(true);

        DownloadLinkedFileAction downloadLinkedFileAction = new DownloadLinkedFileAction(
                databaseContext,
                entry,
                linkedFile,
                linkedFile.getLink(),
                dialogService,
                preferences.getExternalApplicationsPreferences(),
                preferences.getFilePreferences(),
                new CurrentThreadTaskExecutor());
        downloadLinkedFileAction.execute();

        assertEquals(List.of(new LinkedFile("", tempFolder.resolve("asdf.pdf"), "PDF", url)), entry.getFiles());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void doesntReplaceSourceURL(boolean keepHtml) throws IOException {
        String url = "http://arxiv.org/pdf/1207.0408v1";

        LinkedFile linkedFile = new LinkedFile(URLUtil.create(url), "");
        when(databaseContext.getFirstExistingFileDir(any())).thenReturn(Optional.of(tempFolder));
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(filePreferences.shouldKeepDownloadUrl()).thenReturn(true);

        DownloadLinkedFileAction downloadLinkedFileAction = new DownloadLinkedFileAction(
                databaseContext,
                entry,
                linkedFile,
                linkedFile.getLink(),
                dialogService,
                preferences.getExternalApplicationsPreferences(),
                preferences.getFilePreferences(),
                new CurrentThreadTaskExecutor());
        downloadLinkedFileAction.execute();

        assertEquals(List.of(new LinkedFile("", tempFolder.resolve("asdf.pdf"), "PDF", url)), entry.getFiles());

        linkedFile = entry.getFiles().getFirst();

        Path downloadedFile = Path.of(linkedFile.getLink());

        // Verify that re-downloading the file after the first download doesn't modify the entry
        Files.delete(downloadedFile);

        DownloadLinkedFileAction downloadLinkedFileAction2 = new DownloadLinkedFileAction(
                databaseContext,
                entry,
                linkedFile,
                linkedFile.getSourceUrl(),
                dialogService,
                preferences.getExternalApplicationsPreferences(),
                preferences.getFilePreferences(),
                new CurrentThreadTaskExecutor(),
                Path.of(linkedFile.getLink()).getFileName().toString(),
                keepHtml);
        downloadLinkedFileAction2.execute();

        assertEquals(List.of(new LinkedFile("", tempFolder.resolve("asdf.pdf"), "PDF", url)), entry.getFiles());
    }
}
