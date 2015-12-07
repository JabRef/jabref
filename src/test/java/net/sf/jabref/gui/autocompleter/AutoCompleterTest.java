package net.sf.jabref.gui.autocompleter;

import net.sf.jabref.JabRef;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.fieldeditors.TextArea;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.testutils.TestUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.event.ActionEvent;

public class AutoCompleterTest {

    @Test
    @Ignore(value = "works on windows and ubuntu, but not on travis")
    public void testEntryEditorForNameFieldAutoCompleter() {
        // construct an EntryEditor ...
        TestUtils.initJabRef();
        BibtexEntry bibtexEntry = new BibtexEntry();
        bibtexEntry.setField("author", "Brigitte Laurant");
        FieldEditor authorTextField = new TextArea("author", "Hans Meiser");
        EntryEditor editor = new EntryEditor(JabRef.jrf, JabRef.jrf.getCurrentBasePanel(), bibtexEntry);
        // perform action ...
        editor.storeFieldAction.actionPerformed(new ActionEvent(authorTextField, 0, ""));
        // test content of stored words in autocompleter ...
        AutoCompleter<String> autoCompleter = JabRef.jrf.getCurrentBasePanel().getAutoCompleters().get("author");

        // TODO: Use other asserts here, we should check that the autocompleter successfully completes "Hans Meiser" and not look at its index.
        //Assert.assertTrue(autoCompleter.indexContainsWord("Hans Meiser"));
        //Assert.assertTrue(autoCompleter.indexContainsWord("Meiser, Hans"));

        TestUtils.closeJabRef();
    }

    @Test
    @Ignore(value = "works on windows and ubuntu, but not on travis")
    public void testEntryEditorForFieldAnotherAutoCompleter() {
        // construct an EntryEditor ...
        TestUtils.initJabRef();
        BibtexEntry bibtexEntry = new BibtexEntry();
        bibtexEntry.setField("journal", "Testtext");
        FieldEditor authorTextField = new TextArea("journal", "New Testtext");
        EntryEditor editor = new EntryEditor(JabRef.jrf, JabRef.jrf.getCurrentBasePanel(), bibtexEntry);
        // perform action ...
        editor.storeFieldAction.actionPerformed(new ActionEvent(authorTextField, 0, ""));
        // test content of stored words in autocompleter ...
        AutoCompleter<String> autoCompleter = JabRef.jrf.getCurrentBasePanel().getAutoCompleters().get("journal");

        // TODO: Use other asserts here, we should check that the autocompleter successfully completes the journal and not look at its index.
        //Assert.assertTrue(autoCompleter.indexContainsWord("New Testtext"));

        TestUtils.closeJabRef();
    }


}
