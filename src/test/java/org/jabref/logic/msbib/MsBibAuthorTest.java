package org.jabref.logic.msbib;

import org.jabref.model.entry.Author;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MsBibAuthorTest {

    @Test
    public void testGetFirstName() {
        Author author = new Author("Gustav Peter Johann", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("Gustav", msBibAuthor.getFirstName());
    }

    @Test
    public void testGetMiddleName() {
        Author author = new Author("Gustav Peter Johann", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("Peter Johann", msBibAuthor.getMiddleName());
    }

    @Test
    public void testGetNoMiddleName() {

        Author author = new Author("Gustav", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals(null, msBibAuthor.getMiddleName());
    }

    @Test
    public void testGetNoFirstName() {

        Author author = new Author(null, null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals(null, msBibAuthor.getMiddleName());
    }

    @Test
    public void testGetLastName() {
        Author author = new Author("Gustav Peter Johann", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("Bach", msBibAuthor.getLastName());
    }

    @Test
    public void testGetVonAndLastName() {
        Author author = new Author("John", null, "von", "Neumann", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("von Neumann", msBibAuthor.getLastName());
    }
}
