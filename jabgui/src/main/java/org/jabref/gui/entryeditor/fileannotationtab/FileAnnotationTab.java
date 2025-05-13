package org.jabref.gui.entryeditor.fileannotationtab;

import javafx.scene.Parent;
import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;

public class FileAnnotationTab extends EntryEditorTab {

    public static final String NAME = "File annotations";
    private final StateManager stateManager;

    public FileAnnotationTab(StateManager stateManager) {
        this.stateManager = stateManager;

        setText(Localization.lang("File annotations"));
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entry.getField(StandardField.FILE).isPresent();
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
