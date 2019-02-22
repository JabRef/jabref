package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.web.WebView;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.logic.bibtex.comparator.PreambleDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

class PreambleChangeViewModel extends DatabaseChangeViewModel {

    private final PreambleDiff change;

    public PreambleChangeViewModel(PreambleDiff change) {
        super(Localization.lang("Changed preamble"));
        this.change = change;
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().setPreamble(change.getNewPreamble());
        undoEdit.addEdit(new UndoablePreambleChange(database.getDatabase(), change.getOriginalPreamble(), change.getNewPreamble()));
    }

    @Override
    public Node description() {
        StringBuilder text = new StringBuilder(34);
        text.append("<FONT SIZE=3><H2>").append(Localization.lang("Changed preamble")).append("</H2>");

        if (StringUtil.isNotBlank(change.getNewPreamble())) {
            text.append("<H3>").append(Localization.lang("Value set externally")).append(":</H3>" + "<CODE>").append(change.getNewPreamble()).append("</CODE>");
        } else {
            text.append("<H3>").append(Localization.lang("Value cleared externally")).append("</H3>");
        }

        if (StringUtil.isNotBlank(change.getOriginalPreamble())) {
            text.append("<H3>").append(Localization.lang("Current value")).append(":</H3>" + "<CODE>").append(change.getOriginalPreamble()).append("</CODE>");
        }

        WebView webView = new WebView();
        webView.getEngine().loadContent(text.toString());
        return webView;
    }
}
