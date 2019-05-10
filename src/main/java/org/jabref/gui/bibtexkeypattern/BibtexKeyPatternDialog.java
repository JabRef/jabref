package org.jabref.gui.bibtexkeypattern;

import javafx.scene.control.ButtonType;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.metadata.MetaData;

public class BibtexKeyPatternDialog extends BaseDialog<Void> {

    private final MetaData metaData;
    private final BasePanel panel;
    private final BibtexKeyPatternPanel bibtexKeyPatternPanel;

    public BibtexKeyPatternDialog(BasePanel panel) {
        this.bibtexKeyPatternPanel = new BibtexKeyPatternPanel(panel);
        this.panel = panel;
        this.metaData = panel.getBibDatabaseContext().getMetaData();
        AbstractBibtexKeyPattern keyPattern = metaData.getCiteKeyPattern(Globals.prefs.getKeyPattern());
        bibtexKeyPatternPanel.setValues(keyPattern);
        init();
    }

    private void init() {

        this.setTitle(Localization.lang("BibTeX key patterns"));

        this.getDialogPane().setContent(bibtexKeyPatternPanel.getPanel());
        this.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        this.setResultConverter(button -> {
            if (button == ButtonType.APPLY) {
                metaData.setCiteKeyPattern(bibtexKeyPatternPanel.getKeyPatternAsDatabaseBibtexKeyPattern());
                panel.markNonUndoableBaseChanged();
            }

            return null;
        });

    }

}
