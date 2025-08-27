package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.icore.ConferenceAcronymExtractor;
import org.jabref.logic.icore.ConferenceRepository;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.icore.ConferenceEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ICORERankingEditorViewModel extends AbstractEditorViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ICORERankingEditorViewModel.class);

    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final ConferenceRepository repo;

    private ConferenceEntry matchedConference;

    public ICORERankingEditorViewModel(
            Field field,
            SuggestionProvider<?> suggestionProvider,
            FieldCheckers fieldCheckers,
            TaskExecutor taskExecutor,
            DialogService dialogService,
            UndoManager undoManager,
            StateManager stateManager,
            GuiPreferences preferences,
            ConferenceRepository conferenceRepository
    ) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.repo = conferenceRepository;
    }

    public void lookupIdentifier(BibEntry bibEntry) {
        Optional<String> bookTitle = bibEntry.getFieldOrAlias(StandardField.BOOKTITLE);

        if (bookTitle.isEmpty()) {
            bookTitle = bibEntry.getFieldOrAlias(StandardField.JOURNAL);
        }

        if (bookTitle.isEmpty()) {
            return;
        }

        Optional<ConferenceEntry> conference;
        Optional<String> acronym = ConferenceAcronymExtractor.extract(bookTitle.get());
        if (acronym.isPresent()) {
            conference = repo.getConferenceFromAcronym(acronym.get());
            if (conference.isPresent()) {
                entry.setField(field, conference.get().rank());
                matchedConference = conference.get();
                return;
            }
        }

        conference = repo.getConferenceFromBookTitle(bookTitle.get());
        if (conference.isPresent()) {
            entry.setField(field, conference.get().rank());
            matchedConference = conference.get();
        } else {
            entry.setField(field, Localization.lang("not found"));
        }
    }

    public void openExternalLink() {
        if (matchedConference != null) {
            try {
                NativeDesktop.openBrowser(matchedConference.getICOREURL(), preferences.getExternalApplicationsPreferences());
            } catch (IOException e) {
                LOGGER.error("Error opening external link in browser", e);
                dialogService.showErrorDialogAndWait(Localization.lang("Could not open website."), e);
            }
        }
    }
}
