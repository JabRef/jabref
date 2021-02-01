package org.jabref.gui.importer.fetcher;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class LookupIdentifierAction<T extends Identifier> extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupIdentifierAction.class);

    private final JabRefFrame frame;

    private final IdFetcher<T> fetcher;
    private final StateManager stateManager;
    private UndoManager undoManager;

    public LookupIdentifierAction(JabRefFrame frame, IdFetcher<T> fetcher, StateManager stateManager, UndoManager undoManager) {
        this.frame = frame;
        this.fetcher = fetcher;
        this.stateManager = stateManager;
        this.undoManager = undoManager;

        this.executable.bind(needsDatabase(this.stateManager).and(needsEntriesSelected(this.stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(executable, "", Localization.lang("This operation requires one or more entries to be selected.")));
    }

    @Override
    public void execute() {
        try {
            BackgroundTask.wrap(() -> lookupIdentifiers(stateManager.getSelectedEntries()))
                          .onSuccess(frame.getDialogService()::notify)
                          .executeWith(Globals.TASK_EXECUTOR);
        } catch (Exception e) {
            LOGGER.error("Problem running ID Worker", e);
        }
    }

    public Action getAction() {
        return new Action() {

            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.empty();
            }

            @Override
            public Optional<KeyBinding> getKeyBinding() {
                return Optional.empty();
            }

            @Override
            public String getText() {
                return fetcher.getIdentifierName();
            }

            @Override
            public String getDescription() {
                return "";
            }
        };
    }

    private String lookupIdentifiers(List<BibEntry> bibEntries) {
        String totalCount = Integer.toString(bibEntries.size());
        NamedCompound namedCompound = new NamedCompound(Localization.lang("Look up %0", fetcher.getIdentifierName()));
        int count = 0;
        int foundCount = 0;
        for (BibEntry bibEntry : bibEntries) {
            count++;
            final String statusMessage = Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                    fetcher.getIdentifierName(), Integer.toString(count), totalCount, Integer.toString(foundCount));
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getDialogService().notify(statusMessage));
            Optional<T> identifier = Optional.empty();
            try {
                identifier = fetcher.findIdentifier(bibEntry);
            } catch (FetcherException e) {
                LOGGER.error("Could not fetch " + fetcher.getIdentifierName(), e);
            }
            if (identifier.isPresent() && !bibEntry.hasField(identifier.get().getDefaultField())) {
                Optional<FieldChange> fieldChange = bibEntry.setField(identifier.get().getDefaultField(), identifier.get().getNormalized());
                if (fieldChange.isPresent()) {
                    namedCompound.addEdit(new UndoableFieldChange(fieldChange.get()));
                    foundCount++;
                    final String nextStatusMessage = Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                            fetcher.getIdentifierName(), Integer.toString(count), totalCount, Integer.toString(foundCount));
                    DefaultTaskExecutor.runInJavaFXThread(() -> frame.getDialogService().notify(nextStatusMessage));
                }
            }
        }
        namedCompound.end();
        if (foundCount > 0) {
            undoManager.addEdit(namedCompound);
        }
        return Localization.lang("Determined %0 for %1 entries", fetcher.getIdentifierName(), Integer.toString(foundCount));
    }
}
