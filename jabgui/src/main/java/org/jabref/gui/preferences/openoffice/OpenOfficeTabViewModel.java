package org.jabref.gui.preferences.openoffice;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class OpenOfficeTabViewModel implements PreferenceTabViewModel {

    private final StringProperty pandocPath = new SimpleStringProperty();

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final OpenOfficePreferences openOfficePreferences;
    private final TaskExecutor taskExecutor;

    public OpenOfficeTabViewModel(DialogService dialogService,
                                  FilePreferences filePreferences,
                                  OpenOfficePreferences openOfficePreferences,
                                  TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.openOfficePreferences = openOfficePreferences;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void setValues() {
        pandocPath.set(openOfficePreferences.getPandocPath());
    }

    @Override
    public void storeSettings() {
        openOfficePreferences.setPandocPath(pandocPath.get());
    }

    public StringProperty pandocPathProperty() {
        return pandocPath;
    }

    public void browsePandocPath() {
        Optional<Path> selectedPath = dialogService.showFileOpenDialog(
                new FileDialogConfiguration.Builder()
                        .withInitialDirectory(filePreferences.getWorkingDirectory())
                        .build());

        selectedPath.ifPresent(path -> pandocPath.set(path.toString()));
    }

    public void autoDetectPandocPath() {
        BackgroundTask<Optional<String>> task =
                BackgroundTask.wrap(PandocLatexConverter::autoDetect);

        task.titleProperty().set(Localization.lang("Auto-detecting pandoc"));
        task.showToUser(true);

        task.onSuccess(result ->
            result.ifPresentOrElse(
                    path -> {
                        pandocPath.set(path);
                        dialogService.notify(
                                Localization.lang("Pandoc detected at: %0", path));
                    },
                    () -> dialogService.notify(
                            Localization.lang("Pandoc could not be detected automatically"))));

        task.onFailure(_ ->
                dialogService.notify(
                        Localization.lang("Auto-detection of pandoc path failed")));

        taskExecutor.execute(task);
    }
}
