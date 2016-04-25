package net.sf.jabref.gui.entryeditor;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.fieldeditors.TextArea;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.testutils.TestUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class EntryEditorTest {

    @Test
    @Ignore
    public void changingAuthorUpdatesAutoCompleter() {
        TestUtils.initJabRef();
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField("author", "Brigitte Laurant");
        FieldEditor authorTextField = new TextArea("author", "Hans Meiser");
        EntryEditor editor = new EntryEditor(JabRefGUI.getMainFrame(), JabRefGUI.getMainFrame().getCurrentBasePanel(), bibEntry);

        // perform action ...
        editor.getStoreFieldAction().actionPerformed(new ActionEvent(authorTextField, 0, ""));

        // test content of stored words in autocompleter ...
        AutoCompleter<String> autoCompleter = JabRefGUI.getMainFrame().getCurrentBasePanel().getAutoCompleters().get("author");

        List<String> result = autoCompleter.complete("Hans");
        Assert.assertEquals(Arrays.asList("Hans Meiser"), result);

        TestUtils.closeJabRef();
    }

    @Test
    @Ignore
    public void changingSomeFieldUpdatesAutoCompleter() {
        TestUtils.initJabRef();
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField("journal", "Testtext");
        FieldEditor authorTextField = new TextArea("journal", "New Testtext");
        EntryEditor editor = new EntryEditor(JabRefGUI.getMainFrame(), JabRefGUI.getMainFrame().getCurrentBasePanel(), bibEntry);

        // perform action ...
        editor.getStoreFieldAction().actionPerformed(new ActionEvent(authorTextField, 0, ""));

        // test content of stored words in autocompleter ...
        AutoCompleter<String> autoCompleter = JabRefGUI.getMainFrame().getCurrentBasePanel().getAutoCompleters().get("journal");

        List<String> result = autoCompleter.complete("Ne");
        Assert.assertEquals(Arrays.asList("New Testtext"), result);

        TestUtils.closeJabRef();
    }


}
