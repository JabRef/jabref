package net.sf.jabref.logic;

import net.sf.jabref.export.layout.format.CreateDocBookAuthors;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthorListTest {

    @Test
    public void authorListTest() {
        String authorString = "Olaf von Nilsen, Jr.";
        AuthorList authorList = AuthorList.getAuthorList(authorString);
        for (int i = 0; i < authorList.size(); i++) {
            AuthorList.Author author = authorList.getAuthor(i);
            assertEquals("Jr.", author.getFirst());
            assertEquals("Olaf von Nilsen", author.getLast());
            assertEquals(null, author.getJr());
            assertEquals(null, author.getVon());
        }

        assertEquals("<author><firstname>Jr.</firstname><surname>Olaf von Nilsen</surname></author>", new CreateDocBookAuthors().format(authorString));
    }
}
