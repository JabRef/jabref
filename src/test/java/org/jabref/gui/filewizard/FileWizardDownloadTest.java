package org.jabref.gui.filewizard;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.medline.URL;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;


import static org.jabref.gui.filewizard.FileWizardDownloader.*;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Platform;

public class FileWizardDownloadTest {
    // private FileWizardDownloader fwd;
    private BibDatabaseContext databaseContext = Mockito.mock(BibDatabaseContext.class);
    private DialogService dialogService = Mockito.mock(DialogService.class);
    // private Path path = mock(Path.class); //muss noch geändert werden, macht nicht viel sinn path zu mocken
    private Path targetDirectory;
    private Path fileA;
    private Path fileB;
    private Path fileC;

    private BibEntry entryWithMalformedURL = new BibEntry();
    private BibEntry entryWithNoURLToBeFound = new BibEntry();
    private BibEntry entryWithURLToBeFound = new BibEntry();

    private String noSilverBullet;
    private String malformedURL = "lkdj";
    // private java.net.URL malformedURLurl = new java.net.URL("http://arxiv.org/pdf/1207.0408v1");
    private java.net.URL malformedURLurl = null;
    private java.net.URL noSilverBulletURL = null;
    private java.net.URL noURLfound = null;


    @BeforeEach
    void setUp(@TempDir Path folder) throws IOException {
        this.targetDirectory = folder;

        /*
        fileA = folder.resolve("a.pdf");
        fileB = folder.resolve("b.pdf");
        fileC = folder.resolve("c.pdf");

        Files.createFile(fileA);
        Files.createFile(fileB);
        Files.createFile(fileC);

        entryWithURLToBeFound.addFile(new LinkedFile("", fileA, "PDF"));
        entryWithMalformedURL.addFile(new LinkedFile("", fileB, "PDF"));
        entryWithNoURLToBeFound.addFile(new LinkedFile("", fileC, "PDF"));

         */

        // entryWithURLToBeFound.setField(StandardField.URL, noSilverBullet);
        entryWithURLToBeFound.setField(StandardField.DOI, "10.1109/MC.1987.1663532");
        entryWithMalformedURL.setField(StandardField.URL, malformedURL);
        entryWithNoURLToBeFound.setField(StandardField.DOI, "10.1109/ICDSP.2015.7251883");






        // fwd = new FileWizardDownloader(dialogService, Globals.prefs, databaseContext, path);
        // fwd = new FileWizardDownloader(dialogService, Globals.prefs, databaseContext, targetDirectory);
        // FileWizardDownloader spyFWD = spy(fwd);
    }



    @Test
    void testFileWizardDownloaderFromUrlCallsPrepareDownloadTask() {
        /*
        fwd = new FileWizardDownloader(dialogService, Globals.prefs, databaseContext, targetDirectory);
        FileWizardDownloader spyFWD = spy(fwd);
        URLDownload urldownload = new URLDownload(noSilverBulletURL);

        spyFWD.startDownloader(entryWithURLToBeFound);
        spyFWD.fileWizardDownloaderFromURL(databaseContext, noSilverBulletURL, entryWithURLToBeFound, targetDirectory);
        // verify(spyFWD).prepareDownloadTask(path, urldownload);

         */
    }

    @Test
    void testFileWizardDownloaderFromUrlWithMalformedURL() {
        /*
        fwd = new FileWizardDownloader(dialogService, Globals.prefs, databaseContext, targetDirectory);
        FileWizardDownloader spyFWD = spy(fwd);

        // fwd.fileWizardDownloaderFromURL(databaseContext, );

         */
    }

    /**
     * Tests if startDownloader behaves correctly when URL can be found
     * If an URL is found, startDownloader should call fileWizardDownloaderFromURL, and should return true
     */
    @Test
    void testStartDownloaderCouldFindURLReturnsTrue() {
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        FilePreferences filePreferences = mock(FilePreferences.class);
        // when(PreferencesService.getXmpPreferences()).thenReturn(XmpPreferences); // use this variant, as we cannot mock the linkedFileHandler cause it's initialized inside the viewModel
        // when(PreferencesService.getFilePreferences()).thenReturn(XmpPreferences);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        // when(PreferencesService.getXmpPreferences()).thenReturn(XmpPreferences);
        when(Globals.TASK_EXECUTOR).thenReturn(new CurrentThreadTaskExecutor());

        System.out.println("hallo");

        // fwd = new FileWizardDownloader(dialogService, Globals.prefs, databaseContext, targetDirectory);
        // FileWizardDownloader spyFWD = spy(fwd);

        // spyFWD.startDownloader(entryWithURLToBeFound);
        // verify(spyFWD, times(1)).fileWizardSearchURL(entryWithURLToBeFound, Globals.prefs);
        // verify(spyFWD, times(1)).fileWizardDownloaderFromURL(databaseContext, noSilverBulletURL, entryWithURLToBeFound, targetDirectory);
        Assertions.assertTrue(startDownloader(mock(DialogService.class), Globals.prefs, mock(BibDatabaseContext.class), targetDirectory, entryWithURLToBeFound));

    }

    /**
     * Tests if startDownloader behaves correctly when no URL is to be found
     * If no URL is found, startDownloader should not call fileWizardDownloaderFromURL, and should return false
     */
    @Test
    void testStartDownloaderCouldNotFindURLReturnsFalse() {
        /*
        fwd = new FileWizardDownloader(dialogService, Globals.prefs, databaseContext, targetDirectory);
        FileWizardDownloader spyFWD = spy(fwd);

        spyFWD.startDownloader(entryWithNoURLToBeFound);
        verify(spyFWD, times(1)).fileWizardSearchURL(entryWithNoURLToBeFound, Globals.prefs);
        verify(spyFWD, times(0)).fileWizardDownloaderFromURL(databaseContext, noURLfound, entryWithNoURLToBeFound, targetDirectory);

         */
        Assertions.assertFalse(startDownloader(mock(DialogService.class), Globals.prefs, mock(BibDatabaseContext.class), targetDirectory, entryWithNoURLToBeFound));
    }

    /**
     * Tests if after the execution of The downloader an actual file is downloaded
     */
    @Test
    void testStartDownloaderCheckDirectoryForFileWithURLFound() {
        startDownloader(mock(DialogService.class), Globals.prefs, mock(BibDatabaseContext.class), targetDirectory, entryWithURLToBeFound);
        Assertions.assertTrue(new File(entryWithURLToBeFound.getFiles().get(0).getLink()).exists());
    }

    @Test
    void testFileWizardSearchUrlResultReturnsURLReturnsTrue() {
        // String urlFoundOn03112020 = "https://dl.acm.org/doi/pdf/10.1145/1297846.1297973";
        // Assertions.assertEquals(urlFoundOn03112020, fwd.fileWizardSearchURL(entryWithURLToBeFound, Globals.prefs).get());
        Assertions.assertTrue(fileWizardSearchURL(entryWithURLToBeFound, mock(ImportFormatPreferences.class)).isPresent());
        //failes, can not find url, maybe add more info to bibentry

    }

    @Test
    void testFileWizardSearchUrlResultReturnsNoURLReturnsFalse() {
        /*
        TaskExecutor taskExecutor = Globals.TASK_EXECUTOR;
            XmpPreferences xmpPreferences = preferences.getXmpPreferences();
            FilePreferences filePreferences = preferences.getFilePreferences();
            ExternalFileTypes externalFileType = ExternalFileTypes.getInstance();

         */
        Assertions.assertFalse(fileWizardSearchURL(entryWithNoURLToBeFound, mock(ImportFormatPreferences.class)).isPresent());
    }
}
