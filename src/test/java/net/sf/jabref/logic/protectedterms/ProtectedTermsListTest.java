package net.sf.jabref.logic.protectedterms;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ProtectedTermsListTest {

    private ProtectedTermsList internalList;
    private ProtectedTermsList externalList;


    @Before
    public void setUp() {
        internalList = new ProtectedTermsList("Name", Arrays.asList("AAA", "BBB"), "location", true);
        externalList = new ProtectedTermsList("Namely", Arrays.asList("AAA", "BBB"), "location");
    }

    @Test
    public void testProtectedTermsListStringListOfStringStringBoolean() {
        assertTrue(internalList.isInternalList());
    }

    @Test
    public void testProtectedTermsListStringListOfStringString() {
        assertFalse(externalList.isInternalList());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Name", internalList.getDescription());
    }

    @Test
    public void testGetTermList() {
        assertEquals(Arrays.asList("AAA", "BBB"), internalList.getTermList());
    }

    @Test
    public void testGetLocation() {
        assertEquals("location", internalList.getLocation());
    }

    @Test
    public void testGetTermListing() {
        assertEquals("AAA\nBBB", internalList.getTermListing());
    }

    @Test
    public void testCompareTo() {
        assertEquals(-2, internalList.compareTo(externalList));
    }

    @Test
    public void testSetEnabledIsEnabled() {
        assertFalse(internalList.isEnabled());
        internalList.setEnabled(true);
        assertTrue(internalList.isEnabled());
    }

    @Test
    public void testNotEnabledByDefault() {
        assertFalse(internalList.isEnabled());
    }

}
