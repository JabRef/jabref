package org.jabref.gui.edit.automaticfiededitor.editfieldvalue;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.logic.l10n.Localization;

public class EditFieldValueTabView extends AbstractAutomaticFieldEditorTabView {
    @Override
    public String getTabName() {
        return Localization.lang("Edit field value");
    }
}
