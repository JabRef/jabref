package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.crawler.StudyRepository;
import org.jabref.logic.crawler.StudyYamlParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.study.Study;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditExistingStudyAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditExistingStudyAction.class);

    private final DialogService dialogService;
    private final StateManager stateManager;

    public EditExistingStudyAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.executable.bind(ActionHelper.needsStudyDatabase(stateManager));
    }

    @Override
    public void execute() {
        // The action works on the current library
        // This library has to be determined
        if (stateManager.getActiveDatabase().isEmpty() || !stateManager.getActiveDatabase().get().isStudy()) {
            return;
        }
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().get();

        // The action can only be called on an existing AND saved study library
        // The saving is ensured at creation of a study library
        // Thus, this check is only existing to check this assumption
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.error("Library path is not available");
            return;
        }

        Path databasePath = bibDatabaseContext.getDatabasePath().get();

        Path studyDirectory = databasePath.getParent();

        Study study;
        try {
            study = new StudyYamlParser().parseStudyYamlFile(studyDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file"), e);
            return;
        }

        // When the dialog returns, the study.yml file is updated (or kept unmodified at Cancel)
        dialogService.showCustomDialogAndWait(new ManageStudyDefinitionView(study, studyDirectory));
    }
}
