package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.logic.crawler.StudyRepository;
import org.jabref.logic.crawler.StudyYamlParser;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.study.Study;
import org.jabref.model.util.FileUpdateMonitor;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to start a new study:
 * <ol>
 *     <li>Let the user input meta data for the study.</li>
 *     <li>Let JabRef do the crawling afterwards.</li>
 * </ol>
 *
 * Needs to inherit {@link ExistingStudySearchAction}, because that action implements the real crawling.
 *
 * There is the hook {@link StartNewStudyAction#crawlPreparation(Path)}, which is used by {@link ExistingStudySearchAction#crawl()}.
 */
public class StartNewStudyAction extends ExistingStudySearchAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartNewStudyAction.class);

    Study newStudy;

    public StartNewStudyAction(LibraryTabContainer tabContainer,
                               Supplier<OpenDatabaseAction> openDatabaseActionSupplier,
                               FileUpdateMonitor fileUpdateMonitor,
                               TaskExecutor taskExecutor,
                               CliPreferences preferences,
                               StateManager stateManager,
                               DialogService dialogService) {
        super(tabContainer,
                openDatabaseActionSupplier,
                dialogService,
                fileUpdateMonitor,
                taskExecutor,
                preferences,
                stateManager,
                true);
    }

    @Override
    protected void crawlPreparation(Path studyRepositoryRoot) throws IOException, GitAPIException {
        StudyYamlParser studyYAMLParser = new StudyYamlParser();
        studyYAMLParser.writeStudyYamlFile(newStudy, studyRepositoryRoot.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));

        // When execution reaches this point, the user created a new study.
        // The GitHandler is already called to initialize the repository with one single commit "Initial commit".
        // The "Initial commit" should also contain the created YAML.
        // Thus, we append to that commit.
        new GitHandler(studyRepositoryRoot).createCommitOnCurrentBranch("Initial commit", true);
    }

    @Override
    public void execute() {
        Optional<SlrStudyAndDirectory> studyAndDirectory = dialogService.showCustomDialogAndWait(
                new ManageStudyDefinitionView(preferences.getFilePreferences().getWorkingDirectory()));
        if (studyAndDirectory.isEmpty()) {
            return;
        }

        if (studyAndDirectory.get().getStudyDirectory().toString().isBlank()) {
            LOGGER.error("Study directory is blank");
            // This branch probably is never taken
            // Thus, we re-use existing localization
            dialogService.showErrorDialogAndWait(Localization.lang("Must not be empty!"));
            return;
        }
        studyDirectory = studyAndDirectory.get().getStudyDirectory();

        // set variable for #setupRepository
        // setupRepository() is called at crawl()
        newStudy = studyAndDirectory.get().getStudy();

        crawl();
    }
}
