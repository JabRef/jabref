package net.sf.jabref.model.entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class AuthorListParameterTest {

    @Parameters(name = "{index}: parse({0})={1}")
    public static Collection<Object[]> data() {

        return Arrays.asList(new Object[][] {
            { "Doe, John", authorList(new Author("John", "J.", null, "Doe", null)) },
            { "von Berlichingen zu Hornberg, Johann Gottfried",
                    authorList(new Author("Johann Gottfried", "J. G.", "von", "Berlichingen zu Hornberg", null)) },
            //{ "Robert and Sons, Inc.", authorList(new Author(null, null, null, "Robert and Sons, Inc.", null)) },
                //{ "al-Ṣāliḥ, Abdallāh", authorList(new Author("Abdallāh", "A.", null, "al-Ṣāliḥ", null)) },
                {"de la Vallée Poussin, Jean Charles Gabriel",
                        authorList(new Author("Jean Charles Gabriel", "J. C. G.", "de la", "Vallée Poussin", null))},
                {"de la Vallée Poussin, J. C. G.",
                        authorList(new Author("J. C. G.", "J. C. G.", "de la", "Vallée Poussin", null))},
            { "{K}ent-{B}oswell, E. S.", authorList(new Author("E. S.", "E. S.", null, "{K}ent-{B}oswell", null)) },
        });
    }

    public static AuthorList authorList(Author author) {
        return new AuthorList(Arrays.asList(author));
    }

    @Parameter(value = 0)
    public String authorsString;

    @Parameter(value = 1)
    public AuthorList authorsParsed;

    @Test
    public void parseCorrectly() {
        AuthorListParser parser = new AuthorListParser();
        assertEquals(authorsParsed, parser.parse(authorsString));
    }
}
