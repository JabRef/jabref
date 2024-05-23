package org.jabref.logic.msbib;

import org.jabref.model.entry.Author;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MsBibAuthorTest {

    @Test
    public void getGivenNameName() {
        Author author = new Author("Gustav Peter Johann", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("Gustav", msBibAuthor.getFirstName());
    }

    @Test
    public void getMiddleName() {
        Author author = new Author("Gustav Peter Johann", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("Peter Johann", msBibAuthor.getMiddleName());
    }

    @Test
    public void getNoMiddleName() {
        Author author = new Author("Gustav", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertNull(msBibAuthor.getMiddleName());
    }

    @Test
    public void getNoFirstName() {
        Author author = new Author(null, null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertNull(msBibAuthor.getMiddleName());
    }

    @Test
    public void getFamilyNameName() {
        Author author = new Author("Gustav Peter Johann", null, null, "Bach", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("Bach", msBibAuthor.getLastName());
    }

    @Test
    public void getNamePrefixAndLastName() {
        Author author = new Author("John", null, "von", "Neumann", null);
        MsBibAuthor msBibAuthor = new MsBibAuthor(author);
        assertEquals("von Neumann", msBibAuthor.getLastName());
    }
}
