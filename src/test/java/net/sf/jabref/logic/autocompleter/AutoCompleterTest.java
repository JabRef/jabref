package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * @author kahlert, cordes
 */
public class AutoCompleterTest {

    private static final String OTHER_FIELD = "title";
    private static final String AUTHOR_FIELD = "author";
    private static final String CROSSREF_FIELD = "crossref";
    private static final String ENTIRE_FIELD = "journal";

    public static final String PATH_TO_TEST_BIBTEX = "src/test/resources/net/sf/jabref/bibtexFiles/test.bib";

    @Test
    public void testAutoCompleterFactory() {
        Globals.prefs = JabRefPreferences.getInstance();
        AutoCompleter<String> autoCompleter = AutoCompleterFactory.getFor(AutoCompleterTest.AUTHOR_FIELD);
        Assert.assertTrue(autoCompleter instanceof NameFieldAutoCompleter);

        autoCompleter = AutoCompleterFactory.getFor(AutoCompleterTest.OTHER_FIELD);
        Assert.assertTrue(autoCompleter instanceof DefaultAutoCompleter);
    }

    @Test
    public void testDefaultAutoCompleter() {
        AutoCompleter<String> autoCompleter = AutoCompleterFactory.getFor(AutoCompleterTest.OTHER_FIELD);
        for (BibtexEntry entry : getDatabase().getEntries()) {
            autoCompleter.addBibtexEntry(entry);
        }
        Assert.assertEquals("authentication", autoCompleter.complete("authentication")[0]);
        Assert.assertEquals(1, autoCompleter.complete("authentication").length);
        Assert.assertEquals("authentication", autoCompleter.complete("aut")[0]);
        Assert.assertEquals(2, autoCompleter.complete("aut").length); // 1 for case-sensitive search, 2 for case insensitive search (Authornames also included)
        Assert.assertEquals(1, autoCompleter.complete("Aut").length); // "Aut" triggers case-sensitive search, now only "Authornames" is returned
        Assert.assertEquals("context", autoCompleter.complete("con")[0]);
        Assert.assertEquals(1, autoCompleter.complete("con").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);
    }

    @Test
    public void testCrossRefCompleter() {
        AutoCompleter<String> autoCompleter = AutoCompleterFactory.getFor(AutoCompleterTest.CROSSREF_FIELD);
        for (BibtexEntry entry : getDatabase().getEntries()) {
            autoCompleter.addBibtexEntry(entry);
        }
        Assert.assertEquals("1102917", autoCompleter.complete("1102917")[0]);
        Assert.assertEquals(1, autoCompleter.complete("1102917").length);
        Assert.assertEquals("1102917", autoCompleter.complete("11029")[0]);
        Assert.assertEquals(1, autoCompleter.complete("11029").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);
    }

    @Test
    public void testEntireFieldCompleter() {
        AutoCompleter<String> autoCompleter = AutoCompleterFactory.getFor(AutoCompleterTest.ENTIRE_FIELD);
        for (BibtexEntry entry : getDatabase().getEntries()) {
            autoCompleter.addBibtexEntry(entry);
        }
        Assert.assertEquals("Personal Ubiquitous Comput.", autoCompleter.complete("Personal Ubiquitous Comput.")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Personal Ubiquitous Comput.").length);
        Assert.assertEquals("Personal Ubiquitous Comput.", autoCompleter.complete("Pers")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Pers").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);
    }

    @Test
    public void testNameFieldCompleter() {
        Globals.prefs = JabRefPreferences.getInstance();
        AutoCompleter<String> autoCompleter = AutoCompleterFactory.getFor(AutoCompleterTest.AUTHOR_FIELD);
        for (BibtexEntry entry : getDatabase().getEntries()) {
            autoCompleter.addBibtexEntry(entry);
        }

        // tweak preferences to match test cases
        boolean oldAutocomplete = Globals.prefs.getBoolean("autoComplete");
        Globals.prefs.putBoolean("autoComplete", Boolean.TRUE);
        boolean oldAutoCompFF = Globals.prefs.getBoolean("autoCompLF");
        Globals.prefs.putBoolean("autoCompFF", Boolean.FALSE);
        boolean oldAutoCompLF = Globals.prefs.getBoolean("autoCompLF");
        Globals.prefs.putBoolean("autoCompLF", Boolean.FALSE);
        String oldACFM = Globals.prefs.get(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE);
        Globals.prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE_BOTH);

        Assert.assertEquals("Kostakos, V.", autoCompleter.complete("Kostakos")[0]);
        Assert.assertEquals(2, autoCompleter.complete("Kostakos").length);
        Assert.assertEquals("Kostakos, V.", autoCompleter.complete("Kosta")[0]);
        Assert.assertEquals(2, autoCompleter.complete("Kosta").length);
        Assert.assertEquals("Kostakos, Vassilis", autoCompleter.complete("Kostakos, Va")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Kostakos, Va").length);
        Assert.assertEquals("Vassilis Kostakos", autoCompleter.complete("Va")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Va").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);
        Assert.assertEquals(0, autoCompleter.complete("osta").length);

        Assert.assertEquals("Eric von Hippel", autoCompleter.complete("Eric")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Eric").length);
        Assert.assertEquals("von Hippel, E.", autoCompleter.complete("von")[0]);
        Assert.assertEquals(2, autoCompleter.complete("von").length);

        Assert.assertEquals("Reagle, Jr., J. M.", autoCompleter.complete("Reagle")[0]);
        Assert.assertEquals(2, autoCompleter.complete("Reagle").length);
        Assert.assertEquals("Reagle, Jr., Joseph M.", autoCompleter.complete("Reagle, Jr., Jo")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Reagle, Jr., Jo").length);
        Assert.assertEquals("Joseph M. Reagle, Jr.", autoCompleter.complete("Joseph")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Joseph").length);

        Assert.assertEquals("van den Huevel, Jr., J. A.", autoCompleter.complete("van den")[0]);
        Assert.assertEquals(2, autoCompleter.complete("van den").length);
        Assert.assertEquals("Johan A van den Huevel, Jr.", autoCompleter.complete("Joh")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Joh").length);

        Assert.assertEquals("Jr. Sherry, John F.", autoCompleter.complete("Jr. S")[0]);
        Assert.assertEquals(1, autoCompleter.complete("Jr.").length);
        Assert.assertEquals("Sherry, John F., J.", autoCompleter.complete("Sherry")[0]);
        Assert.assertEquals(2, autoCompleter.complete("Sherry").length);

        // restore settings
        Globals.prefs.putBoolean("autoComplete", oldAutocomplete);
        Globals.prefs.putBoolean("autoCompFF", oldAutoCompFF);
        Globals.prefs.putBoolean("autoCompLF", oldAutoCompLF);
        Globals.prefs.put(JabRefPreferences.AUTOCOMPLETE_FIRSTNAME_MODE, oldACFM);
    }

    private BibtexDatabase getDatabase() {
        Globals.prefs = JabRefPreferences.getInstance();
        File fileToLoad = new File(AutoCompleterTest.PATH_TO_TEST_BIBTEX);
        ParserResult pr = JabRef.openBibFile(fileToLoad.getPath(), true);
        return pr.getDatabase();
    }
}
