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
package net.sf.jabref.logic.search.rules;

import net.sf.jabref.logic.search.rules.util.SentenceAnalyzer;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class SentenceAnalyzerTest {

    @Test
    public void testGetWords() throws Exception {
        assertEquals(Arrays.asList("a","b"), new SentenceAnalyzer("a b").getWords());
        assertEquals(Arrays.asList("a","b"), new SentenceAnalyzer(" a b ").getWords());
        assertEquals(Collections.singletonList("b "), new SentenceAnalyzer("\"b \" ").getWords());
        assertEquals(Collections.singletonList(" a"), new SentenceAnalyzer(" \\ a").getWords());
    }
    
}