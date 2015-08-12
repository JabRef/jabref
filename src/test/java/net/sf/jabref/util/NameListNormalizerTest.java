/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.util;

import static org.junit.Assert.assertEquals;

import net.sf.jabref.logic.util.NameListNormalizer;
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