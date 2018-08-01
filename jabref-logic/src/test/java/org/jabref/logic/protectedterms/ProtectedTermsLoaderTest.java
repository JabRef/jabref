package org.jabref.logic.protectedterms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ProtectedTermsLoaderTest {

    private ProtectedTermsLoader loader;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Before
    public void setUp() {
        loader = new ProtectedTermsLoader(new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));

    }

    @Test
    public void testGetProtectedTerms() throws URISyntaxException {
        List<ProtectedTermsList> backupList = new ArrayList<>(loader.getProtectedTermsLists());

        for (ProtectedTermsList list : backupList) {
            loader.removeProtectedTermsList(list);
        }
        assertTrue(loader.getProtectedTermsLists().isEmpty());
        String filename = Paths.get(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                .toFile().getPath();
        loader.addProtectedTermsListFromFile(filename, true);
        assertEquals(Arrays.asList("Einstein"), loader.getProtectedTerms());
    }

    @Test
    public void testAddProtectedTermsListFromFile() throws URISyntaxException {
        String filename = Paths
                .get(ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                        .toURI())
                .toFile().getPath();
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), loader.getProtectedTermsLists().size());
        loader.addProtectedTermsListFromFile(filename, false);
        assertEquals(ProtectedTermsLoader.getInternalLists().size() + 1, loader.getProtectedTermsLists().size());
    }

    @Test
    public void testReadProtectedTermsListFromFileReadsDescription() throws URISyntaxException, FileNotFoundException {
        File file = Paths.get(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertEquals("Term list", list.getDescription());
    }

    @Test
    public void testReadProtectedTermsListFromFileDisabledWorks() throws URISyntaxException, FileNotFoundException {
        File file = Paths.get(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, false);
        assertFalse(list.isEnabled());
    }

    @Test
    public void testReadProtectedTermsListFromFileEnabledWorks() throws URISyntaxException, FileNotFoundException {
        File file = Paths.get(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertTrue(list.isEnabled());
    }

    @Test
    public void testReadProtectedTermsListFromFileIsNotInternalList() throws URISyntaxException, FileNotFoundException {
        File file = Paths.get(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms").toURI())
                .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertFalse(list.isInternalList());
    }

    @Test
    public void testReadProtectedTermsListFromFileNoDescriptionGivesDefaultDescription()
            throws URISyntaxException, FileNotFoundException {
        File file = Paths.get(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/unnamedterms.terms")
                        .toURI())
                .toFile();
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertEquals(Localization.lang("The text after the last line starting with # will be used"),
                list.getDescription());
    }

    @Test
    public void testNewListsAreIncluded() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    public void testNewListsAreEnabled() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        for (ProtectedTermsList list : localLoader.getProtectedTermsLists()) {
            assertTrue(list.isEnabled());
        }
    }

    @Test
    public void testInitalizedAllInternalDisabled() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        for (ProtectedTermsList list : localLoader.getProtectedTermsLists()) {
            assertFalse(list.isEnabled());
        }
    }

    @Test
    public void testUnknownExternalFileWillNotLoad() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                        Collections.singletonList("someUnlikelyNameThatNeverWillExist"), Collections.emptyList(),
                        Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    public void testAllDisabledNoWords() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        assertEquals(Collections.emptyList(), localLoader.getProtectedTerms());
    }

    @Test
    public void testDoNotLoadTheSameInternalListTwice() {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    public void testAddNewTermListAddsList() throws IOException {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        localLoader.addNewProtectedTermsList("My new list", temporaryFolder.newFile().getAbsolutePath());
        assertEquals(ProtectedTermsLoader.getInternalLists().size() + 1, localLoader.getProtectedTermsLists().size());
    }

    @Test
    public void testAddNewTermListNewListInList() throws IOException {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list",
                temporaryFolder.newFile().getAbsolutePath());
        assertTrue(localLoader.getProtectedTermsLists().contains(newList));
    }

    @Test
    public void testRemoveTermList() throws IOException {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list", temporaryFolder.newFile().getAbsolutePath());
        assertTrue(localLoader.removeProtectedTermsList(newList));
    }

    @Test
    public void testRemoveTermListReduceTheCount() throws IOException {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list",
                temporaryFolder.newFile().getAbsolutePath());
        localLoader.removeProtectedTermsList(newList);
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    public void testAddNewTermListSetsCorrectDescription() throws IOException {

        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(Collections.emptyList(), Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(), Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list",
                temporaryFolder.newFile().getAbsolutePath());
        assertEquals("My new list", newList.getDescription());
    }
}
