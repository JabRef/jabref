package org.jabref.gui.entryeditor;

import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.NullMarked;

/// A user-defined tab showing the fields resolved from a [EntryEditorTabModel.CustomizedFieldsTab]
/// (configured in the entry editor preferences).
@NullMarked
public class UserDefinedFieldsTab extends FieldsEditorTab {

    private final EntryEditorTabModel.CustomizedFieldsTab model;

    /// Replaces the base class's content-driven visibility: a regex pattern's matches change when fields
    /// are set or cleared on the *same* entry (e.g. from the Main or Source tab), which the base class's
    /// entry-switch-only evaluation would miss.
    private final BooleanProperty hasResolvedFields = new SimpleBooleanProperty();

    /// The entry whose event bus this tab is subscribed to, for live refresh of regex-captured fields.
    private Optional<BibEntry> subscribedEntry = Optional.empty();

    /// Set while a refresh is queued on the FX thread (events may arrive from background threads,
    /// e.g. fetchers), so bursts of field changes coalesce into one refresh instead of one each.
    private final AtomicBoolean refreshQueued = new AtomicBoolean();

    public UserDefinedFieldsTab(EntryEditorTabModel.CustomizedFieldsTab model,
                                UndoAction undoAction,
                                RedoAction redoAction,
                                GuiPreferences preferences,
                                JournalAbbreviationRepository journalAbbreviationRepository,
                                StateManager stateManager,
                                PreviewPanel previewPanel) {
        super(
                false,
                undoAction,
                redoAction,
                preferences,
                journalAbbreviationRepository,
                stateManager,
                previewPanel);

        this.model = model;

        setContentDrivenVisibility(hasResolvedFields);
        EasyBind.subscribe(currentEntryProperty(), entry ->
                hasResolvedFields.set((entry != null) && !model.resolveFields(entry).isEmpty()));

        setText(model.name());
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        return model.resolveFields(entry);
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (subscribedEntry.filter(current -> current == entry).isEmpty()) {
            subscribedEntry.ifPresent(previous -> previous.unregisterListener(this));
            entry.registerListener(this);
            subscribedEntry = Optional.of(entry);
        }
        super.bindToEntry(entry);
    }

    @Override
    protected void dispose() {
        // The entry's event bus holds listeners strongly; without unregistering, a discarded tab
        // instance would be retained (and keep reacting) for as long as the entry lives.
        subscribedEntry.ifPresent(entry -> entry.unregisterListener(this));
        subscribedEntry = Optional.empty();
        super.dispose();
    }

    /// Refreshes the tab when a field is set or cleared from outside (Main tab, Source tab, fetchers,
    /// undo, …), since that can change which fields a regex pattern captures. Event bursts coalesce
    /// into a single deferred refresh, and the refresh reads the entry's state at callback time (the
    /// event's payload is deliberately ignored), so a queued callback can never apply stale state.
    /// Rebuilds only when the resolved field set actually changes, so typing inside this tab's
    /// editors never steals focus.
    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (refreshQueued.getAndSet(true)) {
            return;
        }
        Platform.runLater(() -> {
            // Cleared before refreshing: an event arriving while we refresh must queue a new callback.
            refreshQueued.set(false);
            BibEntry entry = getCurrentEntry();
            if (entry == null) {
                return;
            }
            SequencedSet<Field> target = determineFieldsToShow(entry);
            hasResolvedFields.set(!target.isEmpty());
            if ((gridPane != null) && !target.equals(editors.keySet())) {
                setupPanel(stateManager.getActiveDatabase().orElse(new BibDatabaseContext()), entry, false);
            }
        });
    }
}
