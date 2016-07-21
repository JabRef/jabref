package net.sf.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import net.sf.jabref.BibtexTestData;
import net.sf.jabref.Globals;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.support.DevEnvironment;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;


public class FileBasedTestCase {

    private BibDatabase database;
    private BibEntry entry;
    private Path rootDir;

    private String oldPdfDirectory;
    private boolean oldUseRegExp;
    private boolean oldAutoLinkExcatKeyOnly;


    @Before
    public void setUp() throws IOException {
        Globals.prefs = JabRefPreferences.getInstance();
        oldUseRegExp = Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY);
        oldAutoLinkExcatKeyOnly = Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY);
        oldPdfDirectory = Globals.prefs.get("pdfDirectory");

        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, false);
        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, false);

        database = BibtexTestData.getBibtexDatabase();
        entry = database.getEntries().iterator().next();

        rootDir = Files.createTempDirectory("UtilFindFileTest");

        Globals.prefs.put("pdfDirectory", rootDir.toAbsolutePath().toString());

        Path subDir = Files.createDirectory(rootDir.resolve("Organization Science"));
        Path pdfSubDir = Files.createDirectory(rootDir.resolve("pdfs"));

        assertTrue(Files.exists(Files.createFile(subDir.resolve("HipKro03 - Hello.pdf"))));
        assertTrue(Files.exists(Files.createFile(rootDir.resolve("HipKro03 - Hello.pdf"))));

        Path pdfSubSubDir = Files.createDirectory(pdfSubDir.resolve("sub"));
        assertTrue(Files.exists(Files.createFile(pdfSubSubDir.resolve("HipKro03-sub.pdf"))));

        Files.createDirectory(rootDir.resolve("2002"));
        Path dir2003 = Files.createDirectory(rootDir.resolve("2003"));
        assertTrue(Files.exists(Files.createFile(dir2003.resolve("Paper by HipKro03.pdf"))));

        Path dirTest = Files.createDirectory(rootDir.resolve("test"));
        assertTrue(Files.exists(Files.createFile(dirTest.resolve(".TEST"))));
        assertTrue(Files.exists(Files.createFile(dirTest.resolve("TEST["))));
        assertTrue(Files.exists(Files.createFile(dirTest.resolve("TE.ST"))));
        assertTrue(Files.exists(Files.createFile(dirTest.resolve("foo.dat"))));

        Path graphicsDir = Files.createDirectory(rootDir.resolve("graphicsDir"));
        Path graphicsSubDir = Files.createDirectories(graphicsDir.resolve("subDir"));

        assertTrue(Files.exists(Files.createFile(graphicsSubDir.resolve("HipKro03test.jpg"))));
        assertTrue(Files.exists(Files.createFile(graphicsSubDir.resolve("HipKro03test.png"))));

    }

    @Test
    public void testFindAssociatedFiles() {

        List<BibEntry> entries = Collections.singletonList(entry);
        List<String> extensions = Arrays.asList("jpg", "pdf");
        List<File> dirs = Arrays.asList(rootDir.resolve("graphicsDir").toFile(),
                rootDir.resolve("pdfs").toFile());

        Map<BibEntry, List<File>> results = FileUtil.findAssociatedFiles(entries, extensions, dirs, Globals.prefs);

        assertEquals(2, results.get(entry).size());
        assertTrue(results.get(entry)
                .contains(rootDir.resolve(Paths.get("graphicsDir", "subDir", "HipKro03test.jpg")).toFile()));
        assertFalse(results.get(entry)
                .contains(rootDir.resolve(Paths.get("graphicsDir", "subDir", "HipKro03test.png")).toFile()));
        assertTrue(results.get(entry).contains(rootDir.resolve(Paths.get("pdfs", "sub", "HipKro03-sub.pdf")).toFile()));
    }

    @Test
    public void testFindFilesException() {
        List<String> extensions = Arrays.asList("jpg", "pdf");
        List<File> dirs = Arrays.asList(rootDir.resolve("asdfasdf/asdfasdf").toFile());
        Set<File> results = FileFinder.findFiles(extensions, dirs);

        assertEquals(0, results.size());
    }

    @Ignore("Fails on CI Server")
    @Test(expected = NullPointerException.class)
    public void testFindFilesNullPointerException() {

        assumeFalse(DevEnvironment.isCIServer());
        FileFinder.findFiles(null, null);
    }
    @After
    public void tearDown() {

        try (Stream<Path> path = Files.walk(rootDir)) {
            // reverse; files before dirs
            for (Path p : path.sorted((a, b) -> b.compareTo(a)).toArray(Path[]::new)) {
                Files.delete(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, oldAutoLinkExcatKeyOnly);
        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, oldUseRegExp);
        Globals.prefs.put("pdfDirectory", oldPdfDirectory);
        // TODO: This is not a great way to do this, sure ;-)

    }

}
