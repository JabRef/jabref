package org.jabref.gui.entryeditor.fileannotationtab;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.entryeditor.NamedEntryEditorTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.views.ViewLoader;

public class FileAnnotationTab extends EntryEditorTab implements NamedEntryEditorTab {

    public static final String NAME = "File annotations";

    private final StateManager stateManager;
    private final EntryEditorPreferences entryEditorPreferences;

    private final ObservableValue<Boolean> shouldShow;

    public FileAnnotationTab(StateManager stateManager,
                             GuiPreferences preferences) {
        this.stateManager = stateManager;
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();

        this.shouldShow = Bindings.createBooleanBinding(
                this::computeShouldShow,
                currentEntryProperty(),
                entryEditorPreferences.getTabModels(),
                stateManager.activeTabProperty());

        setText(Localization.lang("File annotations"));
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
    }

    @Override
    public ObservableValue<Boolean> shouldShow() {
        return shouldShow;
    }

    private boolean computeShouldShow() {
        BibEntry entry = getCurrentEntry();
        if (entry == null || !entryEditorPreferences.isStaticTabVisible(EntryEditorTabModel.StaticTab.FILE_ANNOTATIONS)) {
            return false;
        }
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

    @Override
    public String getName() {
        return NAME;
    }
}
