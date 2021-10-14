package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.crawler.StudyYamlParser;
import org.jabref.model.study.Study;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.eclipse.jgit.api.errors.GitAPIException;

public class StartNewStudyAction extends ExistingStudySearchAction {
    Study newStudy;

    public StartNewStudyAction(JabRefFrame frame, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor, PreferencesService prefs, StateManager stateManager) {
        super(frame, fileUpdateMonitor, taskExecutor, prefs, stateManager);
    }

    @Override
    protected void setupRepository(Path studyRepositoryRoot) throws IOException, GitAPIException {
        StudyYamlParser studyYAMLParser = new StudyYamlParser();
        studyYAMLParser.writeStudyYamlFile(newStudy, studyRepositoryRoot.resolve("study.yml"));
    }

    @Override
    public void execute() {
        Optional<SlrStudyAndDirectory> studyAndDirectory = dialogService.showCustomDialogAndWait(new ManageStudyDefinitionView(null, null, workingDirectory));
        if (studyAndDirectory.isEmpty()) {
            return;
        }
        if (!studyAndDirectory.get().getStudyDirectory().toString().isBlank()) {
            studyDirectory = studyAndDirectory.get().getStudyDirectory();
        }
        newStudy = studyAndDirectory.get().getStudy();
        crawl();
    }
}
