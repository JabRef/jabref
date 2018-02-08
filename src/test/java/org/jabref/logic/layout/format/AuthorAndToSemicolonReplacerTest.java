package org.jabref.logic.layout.format;

import java.util.Arrays;
import java.util.Collection;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class AuthorAndToSemicolonReplacerTest {

    @Parameter(value = 0)
    public String input;

    @Parameter(value = 1)
    public String expected;

    @Parameters(name = "{index}: format(\"{0}\")=\"{1}\"")
    public static Collection<Object[]> data() {

        return Arrays.asList(new Object[][] {
            {"",""},
            {"Someone, Van Something", "Someone, Van Something"},
            {"John Smith and Black Brown, Peter", "John Smith; Black Brown, Peter"},
            {"von Neumann, John and Smith, John and Black Brown, Peter", "von Neumann, John; Smith, John; Black Brown, Peter"},
            {"John von Neumann and John Smith and Peter Black Brown","John von Neumann; John Smith; Peter Black Brown"},
        });
    }

    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorAndToSemicolonReplacer();

        assertEquals(expected, a.format(input));
    }
}
