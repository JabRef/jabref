package net.sf.jabref.logic.util.io;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.jabref.FileBasedTestCase;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Testing Util.findFile for finding files based on regular expressions.
 *
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class UtilFindFileTest extends FileBasedTestCase {


    @Test
    @Ignore(value = "works on windows but not on linux")
    public void testFindAssociatedFiles() {
        Collection<BibEntry> entries = Collections.singletonList(entry);
        Collection<String> extensions = Arrays.asList("jpg", "pdf");
        Collection<File> dirs = Arrays.asList(new File(root.getAbsoluteFile() + "/pdfs/"),
                new File(root.getAbsoluteFile() + "/graphicsDir/"));

        Map<BibEntry, List<File>> results = FileUtil.findAssociatedFiles(entries, extensions, dirs);

        Assert.assertEquals(2, results.get(entry).size());
        Assert.assertTrue(
                results.get(entry).contains(new File(root.getAbsoluteFile() + "/graphicsDir/subDir/HipKro03test.jpg")));
        Assert.assertFalse(
                results.get(entry).contains(new File(root.getAbsoluteFile() + "/graphicsDir/subDir/HipKro03test.png")));
        Assert.assertTrue(results.get(entry).contains(new File(root.getAbsoluteFile() + "/pdfs/sub/HipKro03-sub.pdf")));
    }


}
