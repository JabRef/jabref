package org.jabref.gui.edit.automaticfiededitor.renamefield;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorTab;
import org.jabref.logic.l10n.Localization;

public class RenameFieldTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {
    @Override
    public String getTabName() {
        return Localization.lang("Rename field");
    }
}
