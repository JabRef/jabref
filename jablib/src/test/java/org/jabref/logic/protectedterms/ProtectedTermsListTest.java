package org.jabref.logic.protectedterms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedTermsListTest {

    private ProtectedTermsList internalList;
    private ProtectedTermsList externalList;

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) throws IOException {
        Path path = temporaryFolder.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(path);
        String tempFileName = path.toString();

        internalList = new ProtectedTermsList("Name", new ArrayList<>(Arrays.asList("AAA", "BBB")), "location", true);
        externalList = new ProtectedTermsList("Namely", new ArrayList<>(Arrays.asList("AAA", "BBB")), tempFileName);
    }

    @Test
    void protectedTermsListStringListOfStringStringBoolean() {
        assertTrue(internalList.isInternalList());
    }

    @Test
    void protectedTermsListStringListOfStringString() {
        assertFalse(externalList.isInternalList());
    }

    @Test
    void getDescription() {
        assertEquals("Name", internalList.getDescription());
    }

    @Test
    void getTermList() {
        assertEquals(Arrays.asList("AAA", "BBB"), internalList.getTermList());
    }

    @Test
    void getLocation() {
        assertEquals("location", internalList.getLocation());
    }

    @Test
    void getTermListing() {
        assertEquals("AAA\nBBB", internalList.getTermListing());
    }

    @Test
    void compareTo() {
        assertEquals(-2, internalList.compareTo(externalList));
    }

    @Test
    void setEnabledIsEnabled() {
        assertFalse(internalList.isEnabled());
        internalList.setEnabled(true);
        assertTrue(internalList.isEnabled());
    }

    @Test
    void notEnabledByDefault() {
        assertFalse(internalList.isEnabled());
    }

    @Test
    void canNotAddTermToInternalList() {
        assertFalse(internalList.addProtectedTerm("CCC"));
    }

    @Test
    void termNotAddedToInternalList() {
        internalList.addProtectedTerm("CCC");
        assertFalse(internalList.getTermList().contains("CCC"));
    }

    @Test
    void canAddTermToExternalList() {
        assertTrue(externalList.addProtectedTerm("CCC"));
    }

    @Test
    void termAddedToExternalList() {
        externalList.addProtectedTerm("CCC");
        assertTrue(externalList.getTermList().contains("CCC"));
    }
}
