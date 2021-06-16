package org.jabref.logic.protectedterms;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedTermsLoaderTest {

    private ProtectedTermsLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ProtectedTermsLoader(new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    void testGetProtectedTerms() throws URISyntaxException {
        List<ProtectedTermsList> backupList = new ArrayList<>(loader.getProtectedTermsLists());

        for (ProtectedTermsList list : backupList) {
            loader.removeProtectedTermsList(list);
        }
        assertTrue(loader.getProtectedTermsLists().isEmpty());
        String filename = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                               .toFile().getPath();
        loader.addProtectedTermsListFromFile(filename, true);
        assertEquals(Arrays.asList("Einstein"), loader.getProtectedTerms());
    }

    @Test
    void testAddProtectedTermsListFromFile() throws URISyntaxException {
        String filename = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                                          .toURI())
                              .toFile().getPath();
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), loader.getProtectedTermsLists().size());
        loader.addProtectedTermsListFromFile(filename, false);
        assertEquals(ProtectedTermsLoader.getInternalLists().size() + 1, loader.getProtectedTermsLists().size());
    }

    @Test
    void testReadProtectedTermsListFromFileReadsDescription() throws URISyntaxException, FileNotFoundException {
        File file = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                         .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertEquals("Term list", list.getDescription());
    }

    @Test
    void testReadProtectedTermsListFromFileDisabledWorks() throws URISyntaxException, FileNotFoundException {
        File file = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                         .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, false);
        assertFalse(list.isEnabled());
    }

    @Test
    void testReadProtectedTermsListFromFileEnabledWorks() throws URISyntaxException, FileNotFoundException {
        File file = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                         .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertTrue(list.isEnabled());
    }

    @Test
    void testReadProtectedTermsListFromFileIsNotInternalList() throws URISyntaxException, FileNotFoundException {
        File file = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                         .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertFalse(list.isInternalList());
    }

    @Test
    void testReadProtectedTermsListFromFileNoDescriptionGivesDefaultDescription()
            throws URISyntaxException, FileNotFoundException {
        File file = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/unnamedterms.terms")
                                          .toURI())
                         .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertEquals(Localization.lang("The text after the last line starting with # will be used"),
                list.getDescription());
    }

    @Test
    void testNewListsAreIncluded() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void testNewListsAreEnabled() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(),
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        for (ProtectedTermsList list : localLoader.getProtectedTermsLists()) {
            assertTrue(list.isEnabled());
        }
    }

    @Test
    void testInitalizedAllInternalDisabled() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        for (ProtectedTermsList list : localLoader.getProtectedTermsLists()) {
            assertFalse(list.isEnabled());
        }
    }

    @Test
    void testUnknownExternalFileWillNotLoad() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                        Collections.singletonList("someUnlikelyNameThatNeverWillExist"), Collections.emptyList(),
                        Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void testAllDisabledNoWords() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        assertEquals(Collections.emptyList(), localLoader.getProtectedTerms());
    }

    @Test
    void testDoNotLoadTheSameInternalListTwice() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void testAddNewTermListAddsList(@TempDir Path tempDir) {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        localLoader.addNewProtectedTermsList("My new list", tempDir.toFile().getAbsolutePath());
        assertEquals(ProtectedTermsLoader.getInternalLists().size() + 1, localLoader.getProtectedTermsLists().size());
    }

    @Test
    void testAddNewTermListNewListInList(@TempDir Path tempDir) {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list",
                tempDir.toFile().getAbsolutePath());
        assertTrue(localLoader.getProtectedTermsLists().contains(newList));
    }

    @Test
    void testRemoveTermList(@TempDir Path tempDir) {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list", tempDir.toFile().getAbsolutePath());
        assertTrue(localLoader.removeProtectedTermsList(newList));
    }

    @Test
    void testRemoveTermListReduceTheCount(@TempDir Path tempDir) {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list",
                tempDir.toFile().getAbsolutePath());
        localLoader.removeProtectedTermsList(newList);
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void testAddNewTermListSetsCorrectDescription(@TempDir Path tempDir) {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list",
                tempDir.toFile().getAbsolutePath());
        assertEquals("My new list", newList.getDescription());
    }
}
