package org.jabref.gui.entryeditor.fileannotationtab;

import javafx.scene.Parent;
import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;

public class FileAnnotationTab extends EntryEditorTab {

    public static final String NAME = "File annotations";

    private final StateManager stateManager;
    private final EntryEditorPreferences entryEditorPreferences;

    public FileAnnotationTab(StateManager stateManager,
                             GuiPreferences preferences) {
        this.stateManager = stateManager;
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();

        setText(Localization.lang("File annotations"));
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        if (!entryEditorPreferences.shouldShowFileAnnotationsTab()) {
            return entry.getField(StandardField.FILE).isPresent();
        }

        return entry.getField(StandardField.FILE).isPresent()
                && stateManager.activeTabProperty().get()
                               .map(tab -> tab.getAnnotationCache()
                                              .getFromCache(entry)
                                              .values()
                                              .stream()
                                              .anyMatch(list -> !list.isEmpty()))
                               .orElse(false);
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
