package net.sf.jabref.logic.formatter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.bibtexfields.*;
import net.sf.jabref.logic.formatter.casechanger.*;
import net.sf.jabref.logic.formatter.minifier.AuthorsMinifier;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class FormatterTest {

    public Formatter formatter;


    public FormatterTest(Formatter formatter) {
        this.formatter = formatter;
    }

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void getNameReturnsNotNull() {
        assertNotNull(formatter.getName());
    }

    @Test
    public void getNameReturnsNotEmpty() {
        assertNotEquals("", formatter.getName());
    }

    @Test
    public void getKeyReturnsNotNull() {
        assertNotNull(formatter.getKey());
    }

    @Test
    public void getKeyReturnsNotEmpty() {
        assertNotEquals("", formatter.getKey());
    }

    @Test(expected = NullPointerException.class)
    public void formatOfNullThrowsException() {
        formatter.format(null);
    }

    @Test
    public void formatOfEmptyStringReturnsEmpty() {
        assertEquals("", formatter.format(""));
    }

    @Test
    public void formatNotReturnsNull() {
        assertNotNull(formatter.format("string"));
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(
                new Object[]{new AuthorsFormatter()},
                new Object[]{new UpperEachFirstCaseChanger()},
                new Object[]{new UpperCaseChanger()},
                new Object[]{new MonthFormatter()},
                new Object[]{new LatexFormatter()},
                new Object[]{new IdentityFormatter()},
                new Object[]{new UpperFirstCaseChanger()},
                new Object[]{new AuthorsMinifier()},
                new Object[]{new DateFormatter()},
                new Object[]{new TitleCaseChanger()},
                new Object[]{new CaseKeeper()},
                new Object[]{new PageNumbersFormatter()},
                new Object[]{new LowerCaseChanger()},
                new Object[]{new TrimFormatter()},
                new Object[]{new HTMLToLatexFormatter()},
                new Object[]{new SuperscriptFormatter()},
                new Object[]{new UnitFormatter()},
                new Object[]{new RemoveBracesFormatter()}
        );
    }
}