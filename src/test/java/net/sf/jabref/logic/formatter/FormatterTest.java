package net.sf.jabref.logic.formatter;

import java.util.Arrays;
import java.util.Collection;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.CapitalizeFormatter;
import net.sf.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import net.sf.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import net.sf.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import net.sf.jabref.logic.formatter.minifier.MinifyNameListFormatter;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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

    @Test
    public void getDescriptionAlwaysNonEmpty() {
        assertFalse(formatter.getDescription().isEmpty());
    }

    @Test
    public void getExampleInputAlwaysNonEmpty() {
        assertFalse(formatter.getExampleInput().isEmpty());
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> instancesToTest() {
        // all classes implementing {@link net.sf.jabref.logic.formatter.Formatter}
        // sorted alphabetically
        // Alternative: Use reflection - https://github.com/ronmamo/reflections
        // @formatter:off
        return Arrays.asList(
                new Object[]{new CapitalizeFormatter()},
                new Object[]{new ClearFormatter()},
                new Object[]{new HtmlToLatexFormatter()},
                new Object[]{new IdentityFormatter()},
                new Object[]{new LatexCleanupFormatter()},
                new Object[]{new LatexToUnicodeFormatter()},
                new Object[]{new LowerCaseFormatter()},
                new Object[]{new MinifyNameListFormatter()},
                new Object[]{new NormalizeDateFormatter()},
                new Object[]{new NormalizeMonthFormatter()},
                new Object[]{new NormalizeNamesFormatter()},
                new Object[]{new NormalizePagesFormatter()},
                new Object[]{new OrdinalsToSuperscriptFormatter()},
                new Object[]{new ProtectTermsFormatter()},
                new Object[]{new RemoveBracesFormatter()},
                new Object[]{new SentenceCaseFormatter()},
                new Object[]{new TitleCaseFormatter()},
                new Object[]{new UnicodeToLatexFormatter()},
                new Object[]{new UnitsToLatexFormatter()},
                new Object[]{new UpperCaseFormatter()}
        );
        // @formatter:on
    }
}