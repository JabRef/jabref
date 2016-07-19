package net.sf.jabref;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertTrue;

/**
 * A base class for Testing in JabRef that comes along with some useful
 * functions.
 */
public class FileBasedTestCase {

    protected BibDatabase database;
    protected BibEntry entry;
    protected Path rootDir;

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
