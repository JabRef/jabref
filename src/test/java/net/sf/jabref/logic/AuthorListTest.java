package net.sf.jabref.logic;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthorListTest {

    @Ignore
    @Test
    public void authorListTest() {
        String authorString = "Olaf von Nilsen, Jr.";
        AuthorList authorList = AuthorList.getAuthorList(authorString);
        for (int i = 0; i < authorList.size(); i++) {
            AuthorList.Author author = authorList.getAuthor(i);
            assertEquals("Olaf", author.getFirst());
            assertEquals("Nilsen", author.getLast());
            assertEquals("Jr.", author.getJr());
            assertEquals("von", author.getVon());
        }
    }
}
