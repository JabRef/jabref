package org.jabref.gui.bibtexextractor;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 */
public class ExtractBibtexDialog extends BaseDialog<Void> {

    private final JabRefFrame frame;
    private TextArea textArea;

    public ExtractBibtexDialog(JabRefFrame frame) {
        super();
        this.setTitle(Localization.lang("Input text to parse"));
        this.frame = frame;

       initialize();
    }

    private void initialize(){
        VBox container = new VBox(20);
        container.setPrefWidth(600);
        getDialogPane().setContent(container);
    }
}
