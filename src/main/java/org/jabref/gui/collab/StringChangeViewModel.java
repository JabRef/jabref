package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.web.WebView;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringChangeViewModel.class);
    private final BibtexString string;
    private final String disk;
    private final String label;

    public StringChangeViewModel(BibtexString string, BibtexString tmpString, String disk) {
        super(Localization.lang("Modified string") + ": '" + tmpString.getName() + '\'');
        this.string = string;
        this.label = tmpString.getName();
        this.disk = disk;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            BibtexString bs = new BibtexString(label, disk);
            try {
                database.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(database.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.warn("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
            }
        } else {
            String mem = string.getContent();
            string.setContent(disk);
            undoEdit.addEdit(new UndoableStringChange(string, false, mem, disk));
        }
    }

    @Override
    public Node description() {
        StringBuilder sb = new StringBuilder(46);
        sb.append("<HTML><H2>").append(Localization.lang("Modified string")).append("</H2><H3>")
          .append(Localization.lang("Label")).append(":</H3>").append(label).append("<H3>")
          .append(Localization.lang("New content")).append(":</H3>").append(disk);
        if (string == null) {
            sb.append("<P><I>");
            sb.append(Localization.lang("Cannot merge this change")).append(": ");
            sb.append(Localization.lang("The string has been removed locally")).append("</I>");
        } else {
            sb.append("<H3>");
            sb.append(Localization.lang("Current content")).append(":</H3>");
            sb.append(string.getContent());
        }
        sb.append("</HTML>");

        WebView webView = new WebView();
        webView.getEngine().loadContent(sb.toString());
        return webView;
    }

}
