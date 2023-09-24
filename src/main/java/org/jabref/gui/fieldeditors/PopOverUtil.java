package org.jabref.gui.fieldeditors;

import java.util.Optional;

import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.journalinfo.JournalInfoView;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.controlsfx.control.PopOver;

public class PopOverUtil {

    public static void showJournalInfo(Button button, BibEntry entry, DialogService dialogService, TaskExecutor taskExecutor) {
        Optional<String> optionalIssn = entry.getField(StandardField.ISSN);
        Optional<String> optionalJournalName = entry.getFieldOrAlias(StandardField.JOURNAL);

        if (optionalIssn.isPresent() || optionalJournalName.isPresent()) {
            PopOver popOver = new PopOver();
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setMaxSize(30, 30);
            popOver.setContentNode(progressIndicator);
            popOver.setDetachable(true);
            popOver.setTitle(Localization.lang("Journal Information"));
            popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
            popOver.setArrowSize(0);
            popOver.show(button, 0);

            BackgroundTask
                    .wrap(() -> new JournalInfoView().populateJournalInformation(optionalIssn.orElse(""), optionalJournalName.orElse("")))
                    .onSuccess(updatedNode -> {
                        popOver.setContentNode(updatedNode);
                        popOver.show(button, 0);
                    })
                    .onFailure(exception -> {
                        popOver.hide();
                        String message = Localization.lang("Error while fetching journal information: %0",
                                exception.getMessage());
                        dialogService.notify(message);
                    })
                    .executeWith(taskExecutor);
        } else {
            dialogService.notify(Localization.lang("ISSN or journal name required for fetching journal information"));
        }
    }
}
