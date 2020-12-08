package org.jabref.gui.filewizard;

import org.jabref.gui.externalfiles.AutoSetFileLinksUtil;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static org.jabref.gui.filewizard.FileWizardLocal.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileWizardLocalTest {

    private final FilePreferences fileDirPrefs = mock(FilePreferences.class);
    private final AutoLinkPreferences autoLinkPrefs =
            new AutoLinkPreferences(AutoLinkPreferences.CitationKeyDependency.START, "", ';');
    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final ExternalFileTypes externalFileTypes = mock(ExternalFileTypes.class);

    private Path targetDir;
    private final List<Path> targetDirAsList = new ArrayList<>();
    private final List<Path> otherDirAsList = new ArrayList<>();

    private Path fileInTarget1;
    private Path fileInTarget2;
    private Path fileInOther1;
    private Path fileInOther2;
    private Path fileInvalid;

    private BibEntry entryWithOneFileInTarget = new BibEntry();
    private BibEntry entryWithOneFileInOther = new BibEntry();
    private BibEntry entryWithOneFileInvalid = new BibEntry();
    private BibEntry entryWithZeroFiles = new BibEntry();
    private BibEntry entryWithMultipleFilesOneInOther = new BibEntry();
    private BibEntry entryWithoutLinkButWithMatchingKey = new BibEntry();
    private BibEntry entryWithoutLinkAndWithoutMatchingKey = new BibEntry();

    @BeforeEach
    void setUp(@TempDir Path targetDir, @TempDir Path otherDir) throws IOException {

        this.targetDir = targetDir;
        targetDirAsList.add(targetDir);
        otherDirAsList.add(otherDir);

        fileInTarget1 = targetDir.resolve("a.pdf");
        fileInTarget2 = targetDir.resolve("b.pdf");

        fileInOther1 = otherDir.resolve("c.pdf");
        fileInOther2 = otherDir.resolve("d.pdf");

        fileInvalid = targetDir.resolve("e.pdf");

        Files.createFile(fileInTarget1);
        Files.createFile(fileInTarget2);
        Files.createFile(fileInOther1);
        Files.createFile(fileInOther2);

        entryWithOneFileInTarget.addFile(new LinkedFile("", fileInTarget1, ""));
        entryWithOneFileInOther.addFile(new LinkedFile("", fileInOther1, ""));
        entryWithOneFileInvalid.addFile(new LinkedFile("", fileInvalid, ""));

        entryWithMultipleFilesOneInOther.addFile(0, new LinkedFile("", fileInvalid, ""));
        entryWithMultipleFilesOneInOther.addFile(1, new LinkedFile("", fileInOther2, ""));

        entryWithoutLinkButWithMatchingKey.setCitationKey("b");
        entryWithoutLinkAndWithoutMatchingKey.setCitationKey("f");

        when(externalFileTypes.getExternalFileTypeSelection()).thenReturn(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes()));
        when(databaseContext.getFileDirectories(any())).thenReturn(Collections.singletonList(fileInTarget2.getParent()));
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryWithOneFileInOtherReturnTrue() {

        assertTrue(copyFileFromDefaultDirectory(entryWithOneFileInOther, targetDir, otherDirAsList));
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryWithOneFileInvalidReturnFalse() {

        assertFalse(copyFileFromDefaultDirectory(entryWithOneFileInvalid, targetDir, otherDirAsList));
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryWithZeroFilesReturnFalse() {

        assertFalse(copyFileFromDefaultDirectory(entryWithZeroFiles, targetDir, otherDirAsList));
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryWithMultipleFilesOneInOtherReturnTrue() {

        assertTrue(copyFileFromDefaultDirectory(entryWithMultipleFilesOneInOther, targetDir, otherDirAsList));
    }

    @Test
    void testFileExistsLocallyWithEntryWithOneFileInTargetReturnTrue() {

        assertTrue(fileExistsLocally(entryWithOneFileInTarget, targetDir, otherDirAsList));
    }

    @Test
    void testFileExistsLocallyWithEntryWithOneFileInOtherReturnTrue() {

        assertTrue(fileExistsLocally(entryWithOneFileInOther, targetDir, otherDirAsList));
    }

    @Test
    void testFileExistsLocallyWithEntryWithOneFileInvalidReturnFalse() {

        assertFalse(fileExistsLocally(entryWithOneFileInvalid, targetDir, otherDirAsList));
    }


    @Test
    void testFileExistsLocallyWithEntryWithZeroFilesReturnFalse() {

        assertFalse(fileExistsLocally(entryWithZeroFiles, targetDir, otherDirAsList));
    }

    @Test
    void testMapToFileInDirWithEntryWithOneFileInTargetReturnFalse() {
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        assertFalse(mapToFileInDirectory(entryWithOneFileInTarget, util));
    }

    @Test
    void testMapToFileInDirWithEntryWithoutLinkButWithMatchingKeyReturnTrue() {
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        assertTrue(mapToFileInDirectory(entryWithoutLinkButWithMatchingKey, util));
    }

    @Test
    void testMapToFileInDirWithEntryWithoutLinkAndWithoutMatchingKeyReturnFalse() {
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        assertFalse(mapToFileInDirectory(entryWithoutLinkAndWithoutMatchingKey, util));
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryWithOneFileInOtherDoesCopy() {
        copyFileFromDefaultDirectory(entryWithOneFileInOther, targetDir, otherDirAsList);
        Path expectedFile = targetDir.resolve("c.pdf");
        LinkedFile expectedLinkedFile = new LinkedFile("", expectedFile, "");
        assertEquals(expectedLinkedFile, entryWithOneFileInOther.getFiles().get(1));
        assertEquals(entryWithOneFileInOther.getFiles().size(), 2);
        assertTrue(new File(entryWithOneFileInOther.getFiles().get(1).getLink()).exists());
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryWithOneFileInvalidDoesNotCopy() {
        Path expectedFile = targetDir.resolve("e.pdf");
        copyFileFromDefaultDirectory(entryWithOneFileInvalid, targetDir, otherDirAsList);
        assertEquals(entryWithOneFileInOther.getFiles().size(), 1);
        assertFalse(new File(String.valueOf(expectedFile)).exists());
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryZeroFilesDoesNotCopy() {
        copyFileFromDefaultDirectory(entryWithZeroFiles, targetDir, otherDirAsList);
        assertEquals(entryWithZeroFiles.getFiles().size(), 0);
    }

    @Test
    void testCopyFileFromDefaultDirectoryWithEntryWithMultipleFilesOneInOtherDoesCopy() {
        copyFileFromDefaultDirectory(entryWithMultipleFilesOneInOther, targetDir, otherDirAsList);
        Path expectedFile = targetDir.resolve("d.pdf");
        LinkedFile expectedLinkedFile = new LinkedFile("", expectedFile, "");
        assertEquals(expectedLinkedFile, entryWithMultipleFilesOneInOther.getFiles().get(2));
        assertEquals(entryWithMultipleFilesOneInOther.getFiles().size(), 3);
        assertTrue(new File(entryWithMultipleFilesOneInOther.getFiles().get(2).getLink()).exists());
    }

    @Test
    void testMapToFileInDirWithEntryWithoutLinkButWithMatchingKeyDoesLink() {
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        mapToFileInDirectory(entryWithoutLinkButWithMatchingKey, util);
        LinkedFile expectedLinkedFile = new LinkedFile("", Path.of("b.pdf"), "");
        assertEquals(expectedLinkedFile, entryWithoutLinkButWithMatchingKey.getFiles().get(0));
        assertEquals(1, entryWithoutLinkButWithMatchingKey.getFiles().size());
        assertTrue(new File(String.valueOf(fileInTarget2)).exists());
    }

    @Test
    void testMapToFileInDirWithEntryWithoutLinkAndWithoutMatchingKeyDoesNotLink() {
        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, fileDirPrefs, autoLinkPrefs, externalFileTypes);
        mapToFileInDirectory(entryWithoutLinkAndWithoutMatchingKey, util);
        assertEquals(entryWithoutLinkButWithMatchingKey.getFiles().size(), 0);
    }
}
