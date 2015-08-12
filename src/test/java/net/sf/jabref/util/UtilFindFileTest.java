/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.util;

import net.sf.jabref.AssertUtil;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.FileBasedTestCase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Testing Util.findFile for finding files based on regular expressions.
 *
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class UtilFindFileTest extends FileBasedTestCase {

    String findFile(String dir, String file) {
        return UtilFindFiles.findFile(entry, database, dir, file, true);
    }

    /**
     * Test that more than one slash is taken to mean that a relative path is to
     * be returned.
     *
     * @throws IOException
     */
    @Test
    @Ignore(value = "works on windows but not on linux")
    public void testFindFileRelative() throws IOException {

        // Most basic case
        AssertUtil.assertEqualPaths("HipKro03.pdf", findFile(root.getAbsolutePath() + "/test/",
                "[bibtexkey].pdf"));

        // Including directory
        AssertUtil.assertEqualPaths("test/HipKro03.pdf", findFile(root.getAbsolutePath(),
                "test/[bibtexkey].pdf"));

        // No relative paths
        AssertUtil.assertEqualPaths(new File(root, "test/HipKro03.pdf").getCanonicalPath(), findFile(null,
                root.getAbsolutePath() + "/test/" + "[bibtexkey].pdf"));

        // No relative paths
        AssertUtil.assertEqualPaths(new File(root, "test/HipKro03.pdf").getCanonicalPath(), UtilFindFiles.findFile(
                entry, database, root.getAbsolutePath() + "/test/" + "[bibtexkey].pdf"));

    }

    @Test
    @Ignore(value = "works on windows but not on linux")
    public void testFindPdf() throws IOException {
        String pdf = UtilFindFiles.findPdf(entry, "pdf", root.getAbsolutePath());
        AssertUtil.assertEqualPaths("HipKro03 - Hello.pdf", pdf);

        File fullPath = FileUtil.expandFilename(pdf, root.getAbsolutePath());
        Assert.assertNotNull("expanded file must not be null", fullPath);
        Assert.assertTrue(fullPath.exists());
    }

    @Test
    @Ignore(value = "works on windows but not on linux")
    public void testFindPdfInSubfolder() {
        String pdf = UtilFindFiles.findPdf(entry, "pdf", root.getAbsolutePath() + "/pdfs/");

        AssertUtil.assertEqualPaths("sub/HipKro03-sub.pdf", pdf);

        File fullPath = FileUtil.expandFilename(pdf, root.getAbsolutePath() + "/pdfs/");
        Assert.assertTrue(fullPath.exists());
    }

    @Test
    @Ignore(value = "works on windows but not on linux")
    public void testFindAssociatedFiles() throws IOException {
        Collection<BibtexEntry> entries = Collections.singletonList(entry);
        Collection<String> extensions = Arrays.asList("jpg", "pdf");
        Collection<File> dirs = Arrays.asList(new File(root.getAbsoluteFile() + "/pdfs/"), new File(root.getAbsoluteFile() + "/graphicsDir/"));

        Map<BibtexEntry, List<File>> results = Util.findAssociatedFiles(entries, extensions, dirs);

        Assert.assertEquals(2, results.get(entry).size());
        Assert.assertTrue(results.get(entry).contains(new File(root.getAbsoluteFile() + "/graphicsDir/subDir/HipKro03test.jpg")));
        Assert.assertFalse(results.get(entry).contains(new File(root.getAbsoluteFile() + "/graphicsDir/subDir/HipKro03test.png")));
        Assert.assertTrue(results.get(entry).contains(new File(root.getAbsoluteFile() + "/pdfs/sub/HipKro03-sub.pdf")));
    }

    @Test
    @Ignore(value = "works on windows but not on linux")
    public void testFindPdfInMultiple() throws IOException {

        {
            String[] dirsToSearch = new String[]{root.getAbsolutePath(),
                    root.getAbsolutePath() + "/pdfs/"};
            String pdf = UtilFindFiles.findPdf(entry, "pdf", dirsToSearch);
            AssertUtil.assertEqualPaths("HipKro03 - Hello.pdf", pdf);

            File fullPath = FileUtil.expandFilename(pdf, dirsToSearch);
            Assert.assertNotNull(fullPath);
            Assert.assertTrue(fullPath.exists());
            AssertUtil.assertEqualPaths(root.getAbsolutePath() + "/HipKro03 - Hello.pdf", fullPath
                    .getAbsolutePath());

            String tmp = dirsToSearch[1];
            dirsToSearch[1] = dirsToSearch[0];
            dirsToSearch[0] = tmp;

            fullPath = FileUtil.expandFilename(pdf, dirsToSearch);
            Assert.assertNotNull(fullPath);
            Assert.assertTrue(fullPath.exists());
            AssertUtil.assertEqualPaths(root.getAbsolutePath() + "/HipKro03 - Hello.pdf", fullPath
                    .getAbsolutePath());

            fullPath = FileUtil.expandFilename(pdf, new String[]{dirsToSearch[0]});
            Assert.assertNull(fullPath);

            fullPath = FileUtil.expandFilename(pdf, new String[]{dirsToSearch[1]});
            Assert.assertNotNull(fullPath);
            Assert.assertTrue(fullPath.exists());
            AssertUtil.assertEqualPaths(root.getAbsolutePath() + "/HipKro03 - Hello.pdf", fullPath
                    .getAbsolutePath());
        }

        String[] dirsToSearch = new String[]{root.getAbsolutePath() + "/pdfs/",
                root.getAbsolutePath()};
        String pdf = UtilFindFiles.findPdf(entry, "pdf", dirsToSearch);
        AssertUtil.assertEqualPaths("sub/HipKro03-sub.pdf", pdf);

        File fullPath = FileUtil.expandFilename(pdf, dirsToSearch);
        Assert.assertNotNull(fullPath);
        Assert.assertTrue(fullPath.exists());
        AssertUtil.assertEqualPaths(root.getAbsolutePath() + "/pdfs/sub/HipKro03-sub.pdf", fullPath
                .getAbsolutePath());

        String tmp = dirsToSearch[1];
        dirsToSearch[1] = dirsToSearch[0];
        dirsToSearch[0] = tmp;

        fullPath = FileUtil.expandFilename(pdf, dirsToSearch);
        Assert.assertNotNull(fullPath);
        Assert.assertTrue(fullPath.exists());
        AssertUtil.assertEqualPaths(root.getAbsolutePath() + "/pdfs/sub/HipKro03-sub.pdf", fullPath
                .getAbsolutePath());

        fullPath = FileUtil.expandFilename(pdf, new String[]{dirsToSearch[0]});
        Assert.assertNull(fullPath);

        fullPath = FileUtil.expandFilename(pdf, new String[]{dirsToSearch[1]});
        Assert.assertNotNull(fullPath);
        Assert.assertTrue(fullPath.exists());
        AssertUtil.assertEqualPaths(root.getAbsolutePath() + "/pdfs/sub/HipKro03-sub.pdf", fullPath
                .getAbsolutePath());

    }

    @Test
    @Ignore(value = "works on windows but not on linux")
    public void testFindFile() throws IOException {

        // Simple case
        AssertUtil.assertEqualPaths("HipKro03.pdf", UtilFindFiles.findFile(entry, database, root.getAbsolutePath()
                + "/test/", "[bibtexkey].pdf", true));

        // Not found
        Assert.assertNull(UtilFindFiles.findFile(entry, database, root.getAbsolutePath() + "/test/",
                "Not there [bibtexkey].pdf", true));

        // Test current dir
        AssertUtil.assertEqualPaths(new File(new File("."), "build.xml").getCanonicalPath(), UtilFindFiles.findFile(
                entry, database, "./build.xml"));
        AssertUtil.assertEqualPaths("build.xml", UtilFindFiles.findFile(entry, database, ".", "build.xml", true));

        // Test keys in path and regular expression in file
        AssertUtil.assertEqualPaths(new File(root, "/2003/Paper by HipKro03.pdf").getCanonicalPath(), UtilFindFiles
                .findFile(entry, database, root.getAbsolutePath() + "/[year]/.*[bibtexkey].pdf"));

        // Test . and ..
        AssertUtil.assertEqualPaths(new File(root, "/Organization Science/HipKro03 - Hello.pdf")
                .getCanonicalPath(), UtilFindFiles.findFile(entry, database, root.getAbsolutePath()
                + "/[year]/../2003/.././././[journal]\\" + ".*[bibtexkey].*.pdf"));

        // Test Escape
        AssertUtil.assertEqualPaths(new File(root, "/Organization Science/HipKro03 - Hello.pdf")
                .getCanonicalPath(), UtilFindFiles.findFile(entry, database, root.getAbsolutePath() + "/*/"
                + "[bibtexkey] - Hello\\\\.pdf"));

        AssertUtil.assertEqualPaths("TE.ST", UtilFindFiles.findFile(entry, database, root.getAbsolutePath() + "/test/",
                "TE\\\\.ST", true));
        AssertUtil.assertEqualPaths(".TEST", UtilFindFiles.findFile(entry, database, root.getAbsolutePath() + "/test/",
                "\\\\.TEST", true));
        AssertUtil.assertEqualPaths("TEST[", UtilFindFiles.findFile(entry, database, root.getAbsolutePath() + "/test/",
                "TEST\\\\[", true));

        // Test *
        AssertUtil.assertEqualPaths(new File(root, "/Organization Science/HipKro03 - Hello.pdf")
                .getCanonicalPath(), UtilFindFiles.findFile(entry, database, root.getAbsolutePath() + "/*/"
                + "[bibtexkey].+?.pdf"));

        // Test **
        AssertUtil.assertEqualPaths(new File(root, "/pdfs/sub/HipKro03-sub.pdf").getCanonicalPath(), UtilFindFiles
                .findFile(entry, database, root.getAbsolutePath() + "/**/" + "[bibtexkey]-sub.pdf"));

        // Test ** - Find in level itself too
        AssertUtil.assertEqualPaths(new File(root, "/pdfs/sub/HipKro03-sub.pdf").getCanonicalPath(), UtilFindFiles
                .findFile(entry, database, root.getAbsolutePath() + "/pdfs/sub/**/"
                        + "[bibtexkey]-sub.pdf"));

        // Test ** - Find lowest level first (Rest is Depth first)
        AssertUtil.assertEqualPaths(new File(root, "/HipKro03 - Hello.pdf").getCanonicalPath(), UtilFindFiles.findFile(
                entry, database, root.getAbsolutePath() + "/**/" + "[bibtexkey].*Hello.pdf"));
    }
}
