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

import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals(2, results.get(entry).size());
        Assert.assertTrue(results.get(entry)
                .contains(rootDir.resolve(Paths.get("graphicsDir", "subDir", "HipKro03test.jpg")).toFile()));
        Assert.assertFalse(results.get(entry)
                .contains(rootDir.resolve(Paths.get("graphicsDir", "subDir", "HipKro03test.png")).toFile()));
        Assert.assertTrue(
                results.get(entry).contains(rootDir.resolve(Paths.get("pdfs", "sub", "HipKro03-sub.pdf")).toFile()));
    }

    @Test
    public void testFindAssociatedFilesException() {
        Collection<String> extensions = Arrays.asList("jpg", "pdf");
        Collection<File> dirs = Arrays.asList(new File("asdfasdf/asdfasdf"));
        Set<File> results = FileFinder.findFiles(extensions, dirs);

        Assert.assertEquals(0, results.size());
    }

    @Test(expected = NullPointerException.class)
    public void testFindFilesNUllPointerException() {
        FileFinder.findFiles(null, null);
    }
}
