package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.web.WebView;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StringAddChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StringAddChangeViewModel.class);
    private final BibtexString string;

    public StringAddChangeViewModel(BibtexString string) {
        super(Localization.lang("Added string") + ": '" + string.getName() + '\'');
        this.string = string;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        try {
            database.getDatabase().addString(string);
            undoEdit.addEdit(new UndoableInsertString(database.getDatabase(), string));
        } catch (KeyCollisionException ex) {
            LOGGER.warn("Error: could not add string '" + string.getName() + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public Node description() {
        WebView webView = new WebView();
        webView.getEngine().loadContent("<HTML><H2>" + Localization.lang("Added string") + "</H2><H3>" +
                Localization.lang("Label") + ":</H3>" + string.getName() + "<H3>" +
                Localization.lang("Content") + ":</H3>" + string.getContent() + "</HTML>");
        return webView;
    }

}
