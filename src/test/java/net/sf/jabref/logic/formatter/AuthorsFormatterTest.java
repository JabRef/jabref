package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.formatter.bibtexfields.AuthorsFormatter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuthorsFormatterTest {
    private AuthorsFormatter formatter;

    @Before
    public void setUp() {
        formatter = new AuthorsFormatter();
    }

    @After
    public void teardown() {
        formatter = null;
    }

    @Test
    public void returnsFormatterName() {
        Assert.assertNotNull(formatter.getName());
        Assert.assertNotEquals("", formatter.getName());
    }

    @Test
    public void testNormalizeAuthorList() {
        expectCorrect("Staci D Bilbo", "Bilbo, Staci D.");
        expectCorrect("Staci D. Bilbo", "Staci D. Bilbo"); // TODO strange behaviour

        expectCorrect("Staci D Bilbo and Smith SH and Jaclyn M Schwarz", "Bilbo, Staci D. and Smith, S. H. and Schwarz, Jaclyn M.");

        expectCorrect("Ølver MA", "Ølver, M. A.");

        expectCorrect("Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y. and Olsen Y. Y.",
                "Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.");

        expectCorrect("Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y., Olsen Y. Y.",
                "Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.");

        expectCorrect("Alver, Morten and Alver, Morten O and Alfredsen, JA and Olsen, Y.Y.", "Alver, Morten and Alver, Morten O. and Alfredsen, J. A. and Olsen, Y. Y.");

        expectCorrect("Alver, MA; Alfredsen, JA; Olsen Y.Y.", "Alver, M. A. and Alfredsen, J. A. and Olsen, Y. Y.");

        // TODO: expectCorrect("Kolb, Stefan and J{\\\"o}rg Lenhard and Wirtz, Guido", "Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido");
    }

    private void expectCorrect(String input, String expected) {
        Assert.assertEquals(expected, formatter.format(input));
    }
}