package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.fieldeditors.journalinfo.JournalInfoView;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalEditorViewModel extends AbstractEditorViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalEditorViewModel.class);
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final ReadOnlyBooleanWrapper isJournalInfoButtonVisible = new ReadOnlyBooleanWrapper();
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;

    public JournalEditorViewModel(
            Field field,
            SuggestionProvider<?> suggestionProvider,
            JournalAbbreviationRepository journalAbbreviationRepository,
            FieldCheckers fieldCheckers,
            TaskExecutor taskExecutor,
            DialogService dialogService,
            PreferencesService preferences) {
        super(field, suggestionProvider, fieldCheckers);
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        isJournalInfoButtonVisibleProperty().bind(preferences.getJournalInformationPreferences().journalInfoOptOutProperty().not());
    }

    public void toggleAbbreviation() {
        if (StringUtil.isBlank(text.get())) {
            return;
        }

        // Ignore brackets when matching abbreviations.
        final String name = StringUtil.ignoreCurlyBracket(text.get());

        journalAbbreviationRepository.getNextAbbreviation(name).ifPresent(nextAbbreviation -> {
            text.set(nextAbbreviation);
            // TODO: Add undo
            // panel.getUndoManager().addEdit(new UndoableFieldChange(entry, editor.getName(), text, nextAbbreviation));
        });
    }

    public void showJournalInfo(Button journalInfoButton) {
        Optional<String> optionalIssn = this.entry.getField(StandardField.ISSN);

        if (optionalIssn.isPresent()) {
            PopOver popOver = new PopOver();
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setMaxSize(30, 30);
            popOver.setContentNode(progressIndicator);
            popOver.setDetachable(false);
            popOver.setTitle(Localization.lang("Journal Information"));
            popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
            popOver.setArrowSize(0);
            popOver.show(journalInfoButton, 0);

            BackgroundTask
                    .wrap(() -> new JournalInfoView().populateJournalInformation(optionalIssn.get()))
                    .onSuccess(updatedNode -> {
                        popOver.setContentNode(updatedNode);
                        popOver.show(journalInfoButton, 0);
                    })
                    .onFailure(exception -> {
                        popOver.hide();
                        String message = Localization.lang("Error while fetching journal information: %0",
                                exception.getMessage());
                        LOGGER.warn(message, exception);
                        dialogService.notify(message);
                    })
                    .executeWith(taskExecutor);
        } else {
            dialogService.notify(Localization.lang("ISSN required for fetching journal information"));
        }
    }

    public boolean getIsJournalInfoButtonVisible() {
        return isJournalInfoButtonVisible.get();
    }

    public ReadOnlyBooleanWrapper isJournalInfoButtonVisibleProperty() {
        return isJournalInfoButtonVisible;
    }

    public void setIsJournalInfoButtonVisible(boolean isJournalInfoButtonVisible) {
        this.isJournalInfoButtonVisible.set(isJournalInfoButtonVisible);
    }
}
