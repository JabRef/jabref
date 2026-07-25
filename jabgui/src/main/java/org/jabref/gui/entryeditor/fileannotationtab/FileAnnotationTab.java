package org.jabref.gui.entryeditor.fileannotationtab;

import javafx.scene.Parent;
import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class FileAnnotationTab extends EntryEditorTab {

    private final StateManager stateManager;

    public FileAnnotationTab(StateManager stateManager,
                             GuiPreferences preferences) {
        this.stateManager = stateManager;

        // Visible only when the current entry has a linked file to annotate.
        setContentDrivenVisibility(EasyBind.map(
                currentEntryProperty(),
                entry -> (entry != null) && entry.getField(StandardField.FILE).isPresent()));

        setText(EntryEditorTabModel.BuiltIn.FILE_ANNOTATIONS.displayName());
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (stateManager.activeTabProperty().get().isPresent()) {
            FileAnnotationCache cache = stateManager.activeTabProperty().get().get().getAnnotationCache();
            Parent content = ViewLoader.view(new FileAnnotationTabView(entry, cache))
                                       .load()
                                       .getView();
            setContent(content);
        } else {
            setContent(null);
        }
    }
}
