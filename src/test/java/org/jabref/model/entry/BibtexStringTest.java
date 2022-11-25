package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BibtexStringTest {

    @Test
    public void initalizationWorksCorrectly() {
        // Instantiate
        BibtexString bs = new BibtexString("AAA", "An alternative action");
        assertEquals("AAA", bs.getName());
        assertEquals("An alternative action", bs.getContent());
        assertEquals(BibtexString.Type.OTHER, bs.getType());
    }

    @Test
    public void idIsUpdatedAtSetId() {
        // Instantiate
        BibtexString bs = new BibtexString("AAA", "An alternative action");
        bs.setId("ID");
        assertEquals("ID", bs.getId());
    }

    @Test
    public void cloningDoesNotChangeContents() {
        BibtexString bs = new BibtexString("AAA", "An alternative action");
        bs.setId("ID");

        BibtexString bs2 = (BibtexString) bs.clone();

        assertEquals(bs.getId(), bs2.getId());
        assertEquals(bs.getName(), bs2.getName());
        assertEquals(bs.getContent(), bs2.getContent());
        assertEquals(bs.getType(), bs2.getType());
    }

    @Test
    public void clonedBibtexStringEqualsOriginalString() {
        BibtexString bibtexString = new BibtexString("AAA", "An alternative action");
        bibtexString.setId("ID");

        BibtexString clone = (BibtexString) bibtexString.clone();

        assertEquals(bibtexString, clone);
    }

    @Test
    public void usingTheIdGeneratorDoesNotHitTheOriginalId() {
        // Instantiate
        BibtexString bs = new BibtexString("AAA", "An alternative action");
        bs.setId("ID");
        BibtexString bs2 = (BibtexString) bs.clone();
        bs2.setId(IdGenerator.next());
        assertNotEquals(bs.getId(), bs2.getId());
    }

    @Test
    public void settingFieldsInACloneWorks() {
        // Instantiate
        BibtexString bs = new BibtexString("AAA", "An alternative action");
        bs.setId("ID");
        BibtexString bs2 = (BibtexString) bs.clone();

        bs2.setId(IdGenerator.next());
        bs2.setName("aOG");
        assertEquals(BibtexString.Type.AUTHOR, bs2.getType());

        bs2.setContent("Oscar Gustafsson");
        assertEquals("aOG", bs2.getName());
        assertEquals("Oscar Gustafsson", bs2.getContent());
    }

    @Test
    public void modifyingACloneDoesNotModifyTheOriginalEntry() {
        // Instantiate
        BibtexString original = new BibtexString("AAA", "An alternative action");
        original.setId("ID");

        BibtexString clone = (BibtexString) original.clone();
        clone.setId(IdGenerator.next());
        clone.setName("aOG");
        clone.setContent("Oscar Gustafsson");

        assertEquals("AAA", original.getName());
        assertEquals("An alternative action", original.getContent());
    }

    @Test
    public void getContentNeverReturnsNull() {
        BibtexString bs = new BibtexString("SomeName", null);
        assertNotNull(bs.getContent());
    }

    @Test
    public void authorTypeCorrectlyDetermined() {
        // Source of the example: https://docs.jabref.org/fields/strings
        BibtexString bibtexString = new BibtexString("aKopp", "KoppOliver");
        assertEquals(BibtexString.Type.AUTHOR, bibtexString.getType());
    }

    @Test
    public void institutionTypeCorrectlyDetermined() {
        // Source of the example: https://docs.jabref.org/fields/strings
        BibtexString bibtexString = new BibtexString("iMIT", "{Massachusetts Institute of Technology ({MIT})}");
        assertEquals(BibtexString.Type.INSTITUTION, bibtexString.getType());
    }

    @Test
    public void otherTypeCorrectlyDeterminedForLowerCase() {
        // Source of the example: https://docs.jabref.org/fields/strings
        BibtexString bibtexString = new BibtexString("anct", "Anecdote");
        assertEquals(BibtexString.Type.OTHER, bibtexString.getType());
    }

    @Test
    public void otherTypeCorrectlyDeterminedForUpperCase() {
        // Source of the example: https://docs.jabref.org/fields/strings
        BibtexString bibtexString = new BibtexString("lTOSCA", "Topology and Orchestration Specification for Cloud Applications");
        assertEquals(BibtexString.Type.OTHER, bibtexString.getType());
    }
}
