package org.jabref.gui.edit.automaticfiededitor.renamefield;

import org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabView;
import org.jabref.gui.edit.automaticfiededitor.AutomaticFieldEditorTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class RenameFieldTabView extends AbstractAutomaticFieldEditorTabView implements AutomaticFieldEditorTab {
    public RenameFieldTabView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Rename field");
    }
}
