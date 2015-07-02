package net.sf.jabref.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class NameListNormalizerTest {

    @Test
    public void testNormalizeAuthorList() {
        assertEquals("Bilbo, Staci D.", NameListNormalizer.normalizeAuthorList("Staci D Bilbo"));
        assertEquals("Staci D. Bilbo", NameListNormalizer.normalizeAuthorList("Staci D. Bilbo")); // TODO strange behaviour

        assertEquals("Bilbo, Staci D. and Smith, S. H. and Schwarz, Jaclyn M.", NameListNormalizer.normalizeAuthorList("Staci D Bilbo and Smith SH and Jaclyn M Schwarz"));

        assertEquals("Ølver, M. A.", NameListNormalizer.normalizeAuthorList("Ølver MA"));

        assertEquals("Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                NameListNormalizer.normalizeAuthorList("Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y. and Olsen Y. Y."));

        assertEquals("Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                NameListNormalizer.normalizeAuthorList("Ølver MA, GG Øie, Øie GG, Alfredsen JÅÅ, Jo Alfredsen, Olsen Y.Y., Olsen Y. Y."));

        assertEquals("Alver, Morten and Alver, Morten O. and Alfredsen, J. A. and Olsen, Y. Y.",
                NameListNormalizer.normalizeAuthorList("Alver, Morten and Alver, Morten O and Alfredsen, JA and Olsen, Y.Y."));

        assertEquals("Alver, M. A. and Alfredsen, J. A. and Olsen, Y. Y.",
                NameListNormalizer.normalizeAuthorList("Alver, MA; Alfredsen, JA; Olsen Y.Y."));
    }

}