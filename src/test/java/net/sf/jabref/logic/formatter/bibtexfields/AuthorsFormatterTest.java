package net.sf.jabref.logic.formatter.bibtexfields;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public class AuthorsFormatterTest {
    private AuthorsFormatter formatter;

    @Before
    public void setUp() {
        formatter = new AuthorsFormatter();
    }

    @Parameterized.Parameters(name = "{index}: {1} -> {0}")
    public static Collection<Object[]> authorList() {
        return Arrays.asList(

                new Object[] {"Bilbo, Staci D.", "Staci D Bilbo"},

                // First case, where AuthorList is better than the original AuthorsFormatter
                new Object[] {"Bilbo, Staci D.", "Staci D. Bilbo"},

                new Object[] {"Bilbo, Staci D. and Smith, S. H. and Schwarz, Jaclyn M.",
                        "Staci D Bilbo and Smith SH and Jaclyn M Schwarz"},

                new Object[] {"Ølver, M. A.", "Ølver MA"},

                new Object[] {
                        "Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                        "Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y. and Olsen Y. Y."},

                new Object[] {
                        "Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                        "Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y., Olsen Y. Y."},

                new Object[] {"Alver, Morten and Alver, Morten O. and Alfredsen, J. A. and Olsen, Y. Y.",
                        "Alver, Morten and Alver, Morten O and Alfredsen, JA and Olsen, Y.Y."},

                new Object[] {"Alver, M. A. and Alfredsen, J. A. and Olsen, Y. Y.",
                        "Alver, MA; Alfredsen, JA; Olsen Y.Y."},

                // Second case, where AuthorList is better than the original AuthorsFormatter
                new Object[] {"Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido",
                        "Kolb, Stefan and J{\\\"o}rg Lenhard and Wirtz, Guido"});
    }


    @Parameter(value = 0)
    public String expectedNames;

    @Parameter(value = 1)
    public String namesToNormalize;


    @Test
    public void testNormalizeAuthorList() {
        Assert.assertEquals(expectedNames, formatter.format(namesToNormalize));
    }

}