package org.jabref.logic.protectedterms;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
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
    void getProtectedTerms() throws URISyntaxException {
        List<ProtectedTermsList> backupList = new ArrayList<>(loader.getProtectedTermsLists());

        for (ProtectedTermsList list : backupList) {
            loader.removeProtectedTermsList(list);
        }
        assertTrue(loader.getProtectedTermsLists().isEmpty());
        Path path = Path.of(ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                                                            .toURI());
        loader.addProtectedTermsListFromFile(path, true);
        assertEquals(List.of("Einstein"), loader.getProtectedTerms());
    }

    @Test
    void addProtectedTermsListFromFile() throws URISyntaxException {
        Path path = Path.of(ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                                                            .toURI());
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), loader.getProtectedTermsLists().size());
        loader.addProtectedTermsListFromFile(path, false);
        assertEquals(ProtectedTermsLoader.getInternalLists().size() + 1, loader.getProtectedTermsLists().size());
    }

    @Test
    void readProtectedTermsListFromFileReadsDescription() throws URISyntaxException {
        Path file = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                                          .toURI());

        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertEquals("Term list", list.getDescription());
    }

    @Test
    void readProtectedTermsListFromFileDisabledWorks() throws URISyntaxException {
        Path file = Path.of(ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                                                      .toURI());

        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, false);
        assertFalse(list.isEnabled());
    }

    @Test
    void readProtectedTermsListFromFileEnabledWorks() throws URISyntaxException {
        Path file = Path.of(ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                                                      .toURI());

        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertTrue(list.isEnabled());
    }

    @Test
    void readProtectedTermsListFromFileIsNotInternalList() throws URISyntaxException {
        Path file = Path.of(ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/namedterms.terms")
                                                      .toURI());

        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertFalse(list.isInternalList());
    }

    @Test
    void readProtectedTermsListFromFileNoDescriptionGivesDefaultDescription()
            throws URISyntaxException {
        Path file = Path.of(
                ProtectedTermsLoader.class.getResource("/org/jabref/logic/protectedterms/unnamedterms.terms")
                                          .toURI());
        ProtectedTermsList list = ProtectedTermsLoader.readProtectedTermsListFromFile(file, true);
        assertEquals(Localization.lang("The text after the last line starting with # will be used"),
                list.getDescription());
    }

    @Test
    void newListsAreIncluded() {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void newListsAreEnabled() {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));
        for (ProtectedTermsList list : localLoader.getProtectedTermsLists()) {
            assertTrue(list.isEnabled());
        }
    }

    @Test
    void initalizedAllInternalDisabled() {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                Collections.emptyList(),
                Collections.emptyList(),
                ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList()));
        for (ProtectedTermsList list : localLoader.getProtectedTermsLists()) {
            assertFalse(list.isEnabled());
        }
    }

    @Test
    void unknownExternalFileWillNotLoad() {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                ProtectedTermsLoader.getInternalLists(),
                Collections.singletonList("someUnlikelyNameThatNeverWillExist"),
                Collections.emptyList(),
                Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void allDisabledNoWords() {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                Collections.emptyList(),
                Collections.emptyList(),
                ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList()));
        assertEquals(Collections.emptyList(), localLoader.getProtectedTerms());
    }

    @Test
    void doNotLoadTheSameInternalListTwice() {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(
                        ProtectedTermsLoader.getInternalLists(),
                        Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(),
                        Collections.emptyList()));
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void addNewTermListAddsList(@TempDir Path tempDir) {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                Collections.emptyList(),
                Collections.emptyList(),
                ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList()));
        localLoader.addNewProtectedTermsList("My new list", tempDir.resolve("MyNewList.terms").toAbsolutePath().toString());
        assertEquals(ProtectedTermsLoader.getInternalLists().size() + 1, localLoader.getProtectedTermsLists().size());
    }

    @Test
    void addNewTermListNewListInList(@TempDir Path tempDir) {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(),
                        Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list", tempDir.resolve("MyNewList.terms")
                                                                                                .toAbsolutePath()
                                                                                                .toString());
        assertTrue(localLoader.getProtectedTermsLists().contains(newList));
    }

    @Test
    void removeTermList(@TempDir Path tempDir) {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        ProtectedTermsLoader.getInternalLists(),
                        Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list", tempDir.resolve("MyNewList.terms").toAbsolutePath().toString());
        assertTrue(localLoader.removeProtectedTermsList(newList));
    }

    @Test
    void removeTermListReduceTheCount(@TempDir Path tempDir) {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                Collections.emptyList(),
                Collections.emptyList(),
                ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list", tempDir.resolve("MyNewList.terms").toAbsolutePath().toString());
        localLoader.removeProtectedTermsList(newList);
        assertEquals(ProtectedTermsLoader.getInternalLists().size(), localLoader.getProtectedTermsLists().size());
    }

    @Test
    void addNewTermListSetsCorrectDescription(@TempDir Path tempDir) {
        ProtectedTermsLoader localLoader = new ProtectedTermsLoader(new ProtectedTermsPreferences(
                Collections.emptyList(),
                Collections.emptyList(),
                ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList()));
        ProtectedTermsList newList = localLoader.addNewProtectedTermsList("My new list", tempDir.resolve("MyNewList.terms").toAbsolutePath().toString());
        assertEquals("My new list", newList.getDescription());
    }
}
