package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.BibtexTestData;
import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CiteKeyBasedFileFinderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private BibEntry entry;
    private Path rootDir;
    @Mock
    private ImportFormatPreferences prefs;

    @Before
    public void setUp() throws IOException {
        when(prefs.getFieldContentParserPreferences()).thenReturn(new FieldContentParserPreferences());

        BibDatabase database = BibtexTestData.getBibtexDatabase(prefs);
        entry = database.getEntries().iterator().next();

        rootDir = temporaryFolder.getRoot().toPath();

        Path subDir = Files.createDirectory(rootDir.resolve("Organization Science"));
        Path pdfSubDir = Files.createDirectory(rootDir.resolve("pdfs"));

        Files.createFile(subDir.resolve("HipKro03 - Hello.pdf"));
        Files.createFile(rootDir.resolve("HipKro03 - Hello.pdf"));

        Path pdfSubSubDir = Files.createDirectory(pdfSubDir.resolve("sub"));
        Files.createFile(pdfSubSubDir.resolve("HipKro03-sub.pdf"));

        Files.createDirectory(rootDir.resolve("2002"));
        Path dir2003 = Files.createDirectory(rootDir.resolve("2003"));
        Files.createFile(dir2003.resolve("Paper by HipKro03.pdf"));

        Path dirTest = Files.createDirectory(rootDir.resolve("test"));
        Files.createFile(dirTest.resolve(".TEST"));
        Files.createFile(dirTest.resolve("TEST["));
        Files.createFile(dirTest.resolve("TE.ST"));
        Files.createFile(dirTest.resolve("foo.dat"));

        Path graphicsDir = Files.createDirectory(rootDir.resolve("graphicsDir"));
        Path graphicsSubDir = Files.createDirectories(graphicsDir.resolve("subDir"));

        Files.createFile(graphicsSubDir.resolve("HipKro03test.jpg"));
        Files.createFile(graphicsSubDir.resolve("HipKro03test.png"));

    }

    @Test
    public void testFindAssociatedFiles() {
        List<String> extensions = Arrays.asList("jpg", "pdf");
        List<Path> dirs = Arrays.asList(rootDir.resolve("graphicsDir"), rootDir.resolve("pdfs"));
        FileFinder fileFinder = new CiteKeyBasedFileFinder(false);
        List<Path> results = fileFinder.findAssociatedFiles(entry, dirs, extensions);

        Path jpgFile = rootDir.resolve(Paths.get("graphicsDir", "subDir", "HipKro03test.jpg"));
        Path pdfFile = rootDir.resolve(Paths.get("pdfs", "sub", "HipKro03-sub.pdf"));

        assertEquals(Arrays.asList(jpgFile, pdfFile), results.stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void findFilesByExtensionInNonExistingDirectoryFindsNothing() {
        List<String> extensions = Arrays.asList("jpg", "pdf");
        List<Path> dirs = Collections.singletonList(rootDir.resolve("asdfasdf/asdfasdf"));
        CiteKeyBasedFileFinder fileFinder = new CiteKeyBasedFileFinder(false);
        Set<Path> results = fileFinder.findFilesByExtension(dirs, extensions);

        assertEquals(Collections.emptySet(), results);
    }

    @Test(expected = NullPointerException.class)
    public void findFilesByExtensionWithNullThrowsException() {
        CiteKeyBasedFileFinder fileFinder = new CiteKeyBasedFileFinder(false);
        fileFinder.findFilesByExtension(Collections.emptyList(), null);
    }

}
