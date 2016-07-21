package net.sf.jabref.logic.util.io;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jabref.FileBasedTestCase;
import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.support.DevEnvironment;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
/**
 * Testing Util.findFile for finding files based on regular expressions.
 *
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class UtilFindFileTest extends FileBasedTestCase {

    @Test
    public void testFindAssociatedFiles() {

        Collection<BibEntry> entries = Collections.singletonList(entry);
        Collection<String> extensions = Arrays.asList("jpg", "pdf");
        Collection<File> dirs = Arrays.asList(rootDir.resolve("graphicsDir").toFile(),
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
    public void testFindAssociatedFilesException() {
        Collection<String> extensions = Arrays.asList("jpg", "pdf");
        Collection<File> dirs = Arrays.asList(rootDir.resolve("asdfasdf/asdfasdf").toFile());
        Set<File> results = FileFinder.findFiles(extensions, dirs);

        assertEquals(0, results.size());
    }

    @Ignore("Fails on CI Server")
    @Test(expected = NullPointerException.class)
    public void testFindFilesNUllPointerException() {

        assumeFalse(DevEnvironment.isCIServer());
        FileFinder.findFiles(null, null);
    }
}
