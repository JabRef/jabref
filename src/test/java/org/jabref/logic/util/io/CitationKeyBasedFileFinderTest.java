package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationKeyBasedFileFinderTest {

    private BibEntry entry;
    private Path rootDir;
    private Path graphicsDir;
    private Path pdfsDir;
    private Path jpgFile;
    private Path pdfFile;

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) throws IOException {
        entry = new BibEntry(StandardEntryType.Article);
        entry.setCitationKey("HipKro03");

        rootDir = temporaryFolder;

        Path subDir = Files.createDirectory(rootDir.resolve("Organization Science"));
        pdfsDir = Files.createDirectory(rootDir.resolve("pdfs"));

        Files.createFile(subDir.resolve("HipKro03 - Hello.pdf"));
        Files.createFile(rootDir.resolve("HipKro03 - Hello.pdf"));

        Path pdfSubSubDir = Files.createDirectory(pdfsDir.resolve("sub"));
        pdfFile = Files.createFile(pdfSubSubDir.resolve("HipKro03-sub.pdf"));

        Files.createDirectory(rootDir.resolve("2002"));
        Path dir2003 = Files.createDirectory(rootDir.resolve("2003"));
        Files.createFile(dir2003.resolve("Paper by HipKro03.pdf"));

        Path dirTest = Files.createDirectory(rootDir.resolve("test"));
        Files.createFile(dirTest.resolve(".TEST"));
        Files.createFile(dirTest.resolve("TEST["));
        Files.createFile(dirTest.resolve("TE.ST"));
        Files.createFile(dirTest.resolve("foo.dat"));

        graphicsDir = Files.createDirectory(rootDir.resolve("graphicsDir"));
        Path graphicsSubDir = Files.createDirectories(graphicsDir.resolve("subDir"));

        jpgFile = Files.createFile(graphicsSubDir.resolve("HipKro03 test.jpg"));
        Files.createFile(graphicsSubDir.resolve("HipKro03 test.png"));
    }

    @Test
    void findAssociatedFilesInSubDirectories() throws Exception {
        List<String> extensions = Arrays.asList("jpg", "pdf");
        List<Path> dirs = Arrays.asList(graphicsDir, pdfsDir);
        FileFinder fileFinder = new CitationKeyBasedFileFinder(false);

        List<Path> results = fileFinder.findAssociatedFiles(entry, dirs, extensions);

        assertEquals(Arrays.asList(jpgFile, pdfFile), results);
    }

    @Test
    void findAssociatedFilesIgnoresFilesStartingWithKeyButContinueWithText() throws Exception {
        Files.createFile(pdfsDir.resolve("HipKro03a - Hello second paper.pdf"));
        FileFinder fileFinder = new CitationKeyBasedFileFinder(false);

        List<Path> results = fileFinder.findAssociatedFiles(entry, Collections.singletonList(pdfsDir), Collections.singletonList("pdf"));

        assertEquals(Collections.singletonList(pdfFile), results);
    }

    @Test
    void findAssociatedFilesFindsFilesStartingWithKey() throws Exception {
        Path secondPdfFile = Files.createFile(pdfsDir.resolve("HipKro03_Hello second paper.pdf"));
        FileFinder fileFinder = new CitationKeyBasedFileFinder(false);

        List<Path> results = fileFinder.findAssociatedFiles(entry, Collections.singletonList(pdfsDir), Collections.singletonList("pdf"));

        assertEquals(Arrays.asList(secondPdfFile, pdfFile), results);
    }

    @Test
    void findAssociatedFilesInNonExistingDirectoryFindsNothing() throws Exception {
        List<String> extensions = Arrays.asList("jpg", "pdf");
        List<Path> dirs = Collections.singletonList(rootDir.resolve("asdfasdf/asdfasdf"));
        CitationKeyBasedFileFinder fileFinder = new CitationKeyBasedFileFinder(false);

        List<Path> results = fileFinder.findAssociatedFiles(entry, dirs, extensions);

        assertEquals(Collections.emptyList(), results);
    }
}
