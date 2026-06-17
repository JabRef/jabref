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

    /// Content-availability fallback for tabs that are shown for every entry.
    private static final ObservableValue<Boolean> ALWAYS_VISIBLE = new SimpleBooleanProperty(true);

    /// The entry currently being edited in the editor. Bound by {@link EntryEditorViewModel} to its
    /// currently-edited-entry property for every tab (not only the focused one), so that {@link #shouldShow()}
    /// can react to entry and entry-type changes. Because it is bound, it must not be set directly.
    private final ObjectProperty<@Nullable BibEntry> currentEntry = new SimpleObjectProperty<>();

    /// User-controlled visibility gate, injected by {@link EntryEditorTabFactory} from this tab's
    /// {@link EntryEditorTabModel}. Defaults to always-on for tabs created without a model.
    private ObservableValue<Boolean> visibilityGate = new SimpleBooleanProperty(true);

    /// Lazily built combination of {@link #visibilityGate} and {@link #contentVisibility()}.
    private @Nullable ObservableValue<Boolean> shouldShow;

    /// The entry (and its type) the tab content was last built for. Used to rebuild the content lazily on
    /// focus when the entry or its type changed. Kept separate from {@link #currentEntry} so that pushing a
    /// new entry into the property does not suppress the lazy rebuild in {@link #notifyAboutFocus(BibEntry)}.
    private @Nullable BibEntry boundEntry;
    private @Nullable EntryType boundEntryType;

    public ObjectProperty<@Nullable BibEntry> currentEntryProperty() {
        return currentEntry;
    }

    public @Nullable BibEntry getCurrentEntry() {
        return currentEntry.get();
    }

    /// Sets the user-controlled visibility gate for this tab (its {@link EntryEditorTabModel} visibility,
    /// or another preference for tabs whose toggle lives elsewhere). Called once by the factory after
    /// construction, before the editor first reads {@link #shouldShow()}.
    public void setVisibilityGate(ObservableValue<Boolean> visibilityGate) {
        this.visibilityGate = visibilityGate;
        this.shouldShow = null;
    }

    /// Content-driven visibility: whether the current entry actually has something to show in this tab
    /// (e.g. matching fields, a MathSciNet id, an active fulltext search). Defaults to always available;
    /// tabs that only appear for certain entries override this. It must not consult tab-visibility
    /// preferences — that is the job of the {@link #setVisibilityGate(ObservableValue) visibility gate}.
    protected ObservableValue<Boolean> contentVisibility() {
        return ALWAYS_VISIBLE;
    }

    /// Whether this tab should be shown for the current entry: the user visibility gate AND content
    /// availability. The entry editor observes this value and adds or removes the tab accordingly, so the
    /// editor re-renders without being told.
    public final ObservableValue<Boolean> shouldShow() {
        if (shouldShow == null) {
            shouldShow = EasyBind.combine(visibilityGate, contentVisibility(), (gate, content) -> gate && content);
        }
        return shouldShow;
    }

    /// Stable, well-known name of this tab (its English/config identifier, independent of the localized
    /// {@linkplain #getText() display text}).
    public abstract String getName();

    /// Updates the view with the contents of the given entry.
    protected abstract void bindToEntry(BibEntry entry);

    /// The tab just got the focus. Override this method if you want to perform a special action on focus (like selecting
    /// the first field in the editor)
    protected void handleFocus() {
        // Do nothing by default
    }

    /// Notifies the tab that it got focus and should display the given entry.
    public void notifyAboutFocus(BibEntry entry) {
        // currentEntry is bound to the view model and updates itself; we only react to a changed entry/type here.
        if (!entry.equals(boundEntry) || !entry.getType().equals(boundEntryType)) {
            // bindToEntry is intentionally lazy: content rebuilds only on focus, not on every entry push to currentEntryProperty().
            LOGGER.trace("Tab got focus with different entry (or entry type) {}", entry);
            LOGGER.trace("Different entry: {}", !entry.equals(boundEntry));
            LOGGER.trace("Different entry type: {}", !entry.getType().equals(boundEntryType));
            boundEntry = entry;
            boundEntryType = entry.getType();
            bindToEntry(entry);
        }
        handleFocus();
    }
}
