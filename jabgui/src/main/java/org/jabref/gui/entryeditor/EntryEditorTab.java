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

    /// The entry currently being edited in the editor. Bound by {@link EntryEditorViewModel} to its
    /// currently-edited-entry property for every tab (not only the focused one), so that {@link #visibility()}
    /// can react to entry and entry-type changes. Because it is bound, it must not be set directly.
    private final ObjectProperty<@Nullable BibEntry> currentEntry = new SimpleObjectProperty<>();

    /// The entry (and its type) the tab content was last rendered for. Used to rebuild the content lazily on
    /// focus when the entry or its type changed. Kept separate from {@link #currentEntry} so that pushing a
    /// new entry into the property does not suppress the lazy rebuild in {@link #notifyAboutFocus(BibEntry)}.
    private @Nullable BibEntry renderedEntry;
    private @Nullable EntryType renderedEntryType;

    /// User-controlled visibility gate, injected by {@link EntryEditorTabFactory} from this tab's
    /// {@link EntryEditorTabModel}. Defaults to always-on for tabs created without a model.
    private ObservableValue<Boolean> preferenceDrivenVisibility = new SimpleBooleanProperty(true);

    /// Lazily built combination of {@link #preferenceDrivenVisibility} and {@link #contentDrivenVisibility()}.
    private @Nullable ObservableValue<Boolean> combinedVisibility;

    public void setPreferenceDrivenVisibility(ObservableValue<Boolean> preferenceDrivenVisibility) {
        this.preferenceDrivenVisibility = preferenceDrivenVisibility;
        this.combinedVisibility = null;
    }

    /// Content-driven visibility: tabs that only make sense for certain entries override this to hide
    /// themselves when the current entry has nothing to show.
    protected ObservableValue<Boolean> contentDrivenVisibility() {
        return new SimpleBooleanProperty(true);
    }

    public final ObservableValue<Boolean> visibility() {
        if (combinedVisibility == null) {
            combinedVisibility = EasyBind.combine(
                    preferenceDrivenVisibility,
                    contentDrivenVisibility(),
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

    /// Stable, well-known name of this tab (its English/config identifier, independent of the localized
    /// {@linkplain #getText() display text}).
    public abstract String getName();

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
