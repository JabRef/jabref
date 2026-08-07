package org.jabref.gui.entryeditor;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntryEditorTab extends Tab {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryEditorTab.class);

    private static final ObservableValue<Boolean> ALWAYS_VISIBLE = new SimpleBooleanProperty(true);

    /// The entry currently being edited in the editor. Bound by {@link EntryEditorViewModel}.
    private final ObjectProperty<@Nullable BibEntry> currentEntry = new SimpleObjectProperty<>();

    /// The entry (and its type) the tab content was last rendered for.
    /// Tab is lazily rebuild in {@link #notifyAboutFocus(BibEntry)}.
    private @Nullable BibEntry renderedEntry;
    private @Nullable EntryType renderedEntryType;

    /// User-controlled visibility gate, injected by {@link EntryEditorTabFactory}.
    private ObservableValue<Boolean> preferenceDrivenVisibility = ALWAYS_VISIBLE;

    /// Content-driven visibility gate for tabs that only make sense for certain entries.
    private ObservableValue<Boolean> contentDrivenVisibility = ALWAYS_VISIBLE;

    /// Lazily built combination of {@link #preferenceDrivenVisibility} and {@link #contentDrivenVisibility}.
    private @Nullable ObservableValue<Boolean> combinedVisibility;

    public void setPreferenceDrivenVisibility(ObservableValue<Boolean> preferenceDrivenVisibility) {
        this.preferenceDrivenVisibility = preferenceDrivenVisibility;
        this.combinedVisibility = null;
    }

    protected void setContentDrivenVisibility(ObservableValue<Boolean> contentDrivenVisibility) {
        this.contentDrivenVisibility = contentDrivenVisibility;
        this.combinedVisibility = null;
    }

    public final ObservableValue<Boolean> visibility() {
        // Built once and cached; JavaFX Application Thread only (no synchronization on the lazy init).
        if (combinedVisibility == null) {
            combinedVisibility = EasyBind.combine(
                    preferenceDrivenVisibility,
                    contentDrivenVisibility,
                    (preference, content) -> preference && content
            );
        }

        return combinedVisibility;
    }

    public @Nullable BibEntry getCurrentEntry() {
        return currentEntry.get();
    }

    public ObjectProperty<@Nullable BibEntry> currentEntryProperty() {
        return currentEntry;
    }

    protected abstract void bindToEntry(BibEntry entry);

    /// Override to perform a special action on focus (like selecting the first field in the editor).
    protected void handleFocus() {
    }

    public void notifyAboutFocus(BibEntry entry) {
        // currentEntry is bound to the view model and updates on its own; rebuild content only when the
        // entry or its type actually changed (intentionally lazy: not on every push to the property).
        if (!entry.equals(renderedEntry) || !entry.getType().equals(renderedEntryType)) {
            LOGGER.trace("Tab got focus with different entry (or entry type) {}", entry);
            LOGGER.trace("Different entry: {}", !entry.equals(renderedEntry));
            LOGGER.trace("Different entry type: {}", !entry.getType().equals(renderedEntryType));
            renderedEntry = entry;
            renderedEntryType = entry.getType();
            bindToEntry(entry);
        }
        handleFocus();
    }
}
