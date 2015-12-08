package net.sf.jabref.gui.entryeditor;

import net.sf.jabref.JabRef;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.fieldeditors.TextArea;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.testutils.TestUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.event.ActionEvent;

public class EntryEditorTest {

    @Test
    @Ignore
    public void changingAuthorUpdatesAutoCompleter() {
        TestUtils.initJabRef();
        BibtexEntry bibtexEntry = new BibtexEntry();
        bibtexEntry.setField("author", "Brigitte Laurant");
        FieldEditor authorTextField = new TextArea("author", "Hans Meiser");
        EntryEditor editor = new EntryEditor(JabRef.jrf, JabRef.jrf.getCurrentBasePanel(), bibtexEntry);

        // perform action ...
        editor.storeFieldAction.actionPerformed(new ActionEvent(authorTextField, 0, ""));

        // test content of stored words in autocompleter ...
        AutoCompleter<String> autoCompleter = JabRef.jrf.getCurrentBasePanel().getAutoCompleters().get("author");

        String[] result = autoCompleter.complete("Hans");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Hans Meiser", result[0]);

        TestUtils.closeJabRef();
    }

    @Test
    @Ignore
    public void changingSomeFieldUpdatesAutoCompleter() {
        TestUtils.initJabRef();
        BibtexEntry bibtexEntry = new BibtexEntry();
        bibtexEntry.setField("journal", "Testtext");
        FieldEditor authorTextField = new TextArea("journal", "New Testtext");
        EntryEditor editor = new EntryEditor(JabRef.jrf, JabRef.jrf.getCurrentBasePanel(), bibtexEntry);

        // perform action ...
        editor.storeFieldAction.actionPerformed(new ActionEvent(authorTextField, 0, ""));

        // test content of stored words in autocompleter ...
        AutoCompleter<String> autoCompleter = JabRef.jrf.getCurrentBasePanel().getAutoCompleters().get("journal");

        String[] result = autoCompleter.complete("Ne");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("New Testtext", result[0]);

        TestUtils.closeJabRef();
    }


}
