package tests.net.sf.jabref.gui;

import java.awt.event.ActionEvent;
import java.io.File;

import junit.framework.TestCase;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.EntryEditor;
import net.sf.jabref.FieldEditor;
import net.sf.jabref.FieldTextArea;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.autocompleter.AbstractAutoCompleter;
import net.sf.jabref.autocompleter.AutoCompleterFactory;
import net.sf.jabref.autocompleter.DefaultAutoCompleter;
import net.sf.jabref.autocompleter.NameFieldAutoCompleter;
import net.sf.jabref.imports.ParserResult;
import tests.net.sf.jabref.testutils.TestUtils;

/**
 * 
 * @author kahlert, cordes
 * 
 */
public class AutoCompleterTest extends TestCase {

	private static final String OTHER_FIELD = "title";
	private static final String AUTHOR_FIELD = "author";
	private static final String CROSSREF_FIELD = "crossref";
	private static final String ENTIRE_FIELD = "journal";

	public static final String PATH_TO_TEST_BIBTEX = "src/tests/net/sf/jabref/bibtexFiles/test.bib";

	public void testAutoCompleterFactory() {
		AbstractAutoCompleter autoCompleter = AutoCompleterFactory.getFor(AUTHOR_FIELD);
		assertTrue(autoCompleter instanceof NameFieldAutoCompleter);

		autoCompleter = AutoCompleterFactory.getFor(OTHER_FIELD);
		assertTrue(autoCompleter instanceof DefaultAutoCompleter);
	}

	public void testDefaultAutoCompleter() {
		AbstractAutoCompleter autoCompleter = AutoCompleterFactory.getFor(OTHER_FIELD);
		for (BibtexEntry entry : getDatabse().getEntries()) {
			autoCompleter.addBibtexEntry(entry);
		}
		assertEquals("authentication", autoCompleter.complete("authentication")[0]);
		assertEquals(1, autoCompleter.complete("authentication").length);
		assertEquals("authentication", autoCompleter.complete("aut")[0]);
		assertEquals(1, autoCompleter.complete("aut").length);
		assertEquals("context", autoCompleter.complete("con")[0]);
		assertEquals(1, autoCompleter.complete("con").length);
		assertEquals(0, autoCompleter.complete("osta").length);
		assertEquals(0, autoCompleter.complete("osta").length);
	}

	public void testCrossRefCompleter() {
		AbstractAutoCompleter autoCompleter = AutoCompleterFactory.getFor(CROSSREF_FIELD);
		for (BibtexEntry entry : getDatabse().getEntries()) {
			autoCompleter.addBibtexEntry(entry);
		}
		assertEquals("1102917", autoCompleter.complete("1102917")[0]);
		assertEquals(1, autoCompleter.complete("1102917").length);
		assertEquals("1102917", autoCompleter.complete("11029")[0]);
		assertEquals(1, autoCompleter.complete("11029").length);
		assertEquals(0, autoCompleter.complete("osta").length);
		assertEquals(0, autoCompleter.complete("osta").length);
	}

	public void testEntireFieldCompleter() {
		AbstractAutoCompleter autoCompleter = AutoCompleterFactory.getFor(ENTIRE_FIELD);
		for (BibtexEntry entry : getDatabse().getEntries()) {
			autoCompleter.addBibtexEntry(entry);
		}
		assertEquals("Personal Ubiquitous Comput.", autoCompleter.complete("Personal Ubiquitous Comput.")[0]);
		assertEquals(1, autoCompleter.complete("Personal Ubiquitous Comput.").length);
		assertEquals("Personal Ubiquitous Comput.", autoCompleter.complete("Pers")[0]);
		assertEquals(1, autoCompleter.complete("Pers").length);
		assertEquals(0, autoCompleter.complete("osta").length);
		assertEquals(0, autoCompleter.complete("osta").length);
	}

	public void testNameFieldCompleter() {
		AbstractAutoCompleter autoCompleter = AutoCompleterFactory.getFor(AUTHOR_FIELD);
		for (BibtexEntry entry : getDatabse().getEntries()) {
			autoCompleter.addBibtexEntry(entry);
		}
		assertEquals("Kostakos, V.", autoCompleter.complete("Kostakos")[0]);
		assertEquals(2, autoCompleter.complete("Kostakos").length);
		assertEquals("Kostakos, V.", autoCompleter.complete("Kosta")[0]);
		assertEquals(2, autoCompleter.complete("Kosta").length);
		assertEquals("Kostakos, Vassilis", autoCompleter.complete("Kostakos, Va")[0]);
		assertEquals(1, autoCompleter.complete("Kostakos, Va").length);
		assertEquals("Vassilis Kostakos", autoCompleter.complete("Va")[0]);
		assertEquals(1, autoCompleter.complete("Va").length);
		assertEquals(0, autoCompleter.complete("osta").length);
		assertEquals(0, autoCompleter.complete("osta").length);

		assertEquals("Eric von Hippel", autoCompleter.complete("Eric")[0]);
		assertEquals(1, autoCompleter.complete("Eric").length);
		assertEquals("von Hippel, E.", autoCompleter.complete("von")[0]);
		assertEquals(2, autoCompleter.complete("von").length);

		assertEquals("Reagle, Jr., J. M.", autoCompleter.complete("Reagle")[0]);
		assertEquals(2, autoCompleter.complete("Reagle").length);
		assertEquals("Reagle, Jr., Joseph M.", autoCompleter.complete("Reagle, Jr., Jo")[0]);
		assertEquals(1, autoCompleter.complete("Reagle, Jr., Jo").length);
		assertEquals("Joseph M. Reagle, Jr.", autoCompleter.complete("Joseph")[0]);
		assertEquals(1, autoCompleter.complete("Joseph").length);

		assertEquals("van den Huevel, Jr., J. A.", autoCompleter.complete("van den")[0]);
		assertEquals(2, autoCompleter.complete("van den").length);
		assertEquals("Johan A van den Huevel, Jr.", autoCompleter.complete("Joh")[0]);
		assertEquals(1, autoCompleter.complete("Joh").length);

		assertEquals("Jr. Sherry, John F.", autoCompleter.complete("Jr. S")[0]);
		assertEquals(1, autoCompleter.complete("Jr.").length);
		assertEquals("Sherry, John F., J.", autoCompleter.complete("Sherry")[0]);
		assertEquals(2, autoCompleter.complete("Sherry").length);
	}

	public void testEntryEditorForNameFieldAutoCompleter() {
		// construct an EntryEditor ...
		JabRef jabref = TestUtils.getInitializedJabRef();
		BibtexEntry bibtexEntry = new BibtexEntry();
		bibtexEntry.setField("author", "Brigitte Laurant");
		FieldEditor authorTextField = new FieldTextArea("author", "Hans Meiser");
		EntryEditor editor = new EntryEditor(jabref.jrf, jabref.jrf.basePanel(), bibtexEntry);
		// perform action ...
		editor.storeFieldAction.actionPerformed(new ActionEvent(authorTextField, 0, ""));
		// test content of stored words in autocompleter ...
		AbstractAutoCompleter autoCompleter = jabref.jrf.basePanel().getAutoCompleter("author");
		assertTrue(autoCompleter.indexContainsWord("Hans Meiser"));
		assertTrue(autoCompleter.indexContainsWord("Meiser, Hans"));

		TestUtils.closeJabRef();
	}

	public void testEntryEditorForFieldAnotherAutoCompleter() {
		// construct an EntryEditor ...
		JabRef jabref = TestUtils.getInitializedJabRef();
		BibtexEntry bibtexEntry = new BibtexEntry();
		bibtexEntry.setField("journal", "Testtext");
		FieldEditor authorTextField = new FieldTextArea("journal", "New Testtext");
		EntryEditor editor = new EntryEditor(jabref.jrf, jabref.jrf.basePanel(), bibtexEntry);
		// perform action ...
		editor.storeFieldAction.actionPerformed(new ActionEvent(authorTextField, 0, ""));
		// test content of stored words in autocompleter ...
		AbstractAutoCompleter autoCompleter = jabref.jrf.basePanel().getAutoCompleter("journal");
		assertTrue(autoCompleter.indexContainsWord("New Testtext"));

		TestUtils.closeJabRef();
	}

	private BibtexDatabase getDatabse() {
		Globals.prefs = JabRefPreferences.getInstance();
		File fileToLoad = new File(PATH_TO_TEST_BIBTEX);
		ParserResult pr = JabRef.openBibFile(fileToLoad.getPath(), true);
		BibtexDatabase filledDatabase = pr.getDatabase();
		return filledDatabase;
	}
}
