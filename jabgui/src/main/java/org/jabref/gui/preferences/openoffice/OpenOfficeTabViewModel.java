package org.jabref.gui.preferences.openoffice;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class OpenOfficeTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenOfficeTabViewModel.class);

    private final StringProperty pandocPath = new SimpleStringProperty();
    private final BooleanProperty zoteroCompatibilityMode = new SimpleBooleanProperty();
    private final BooleanProperty inferCslStyleFromDocument = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final OpenOfficePreferences openOfficePreferences;
    private final TaskExecutor taskExecutor;
    private final BooleanBinding zoteroCompatibilityModeDisabled;

    public OpenOfficeTabViewModel(DialogService dialogService,
                                  FilePreferences filePreferences,
                                  OpenOfficePreferences openOfficePreferences,
                                  TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.openOfficePreferences = openOfficePreferences;
        this.taskExecutor = taskExecutor;
        zoteroCompatibilityModeDisabled = Bindings.createBooleanBinding(
                () -> !(openOfficePreferences.getCurrentStyle() instanceof CitationStyle),
                openOfficePreferences.currentStyleProperty());
        zoteroCompatibilityMode.addListener((_, _, enabled) -> {
            if (!enabled) {
                inferCslStyleFromDocument.set(false);
            }
        });
        zoteroCompatibilityModeDisabled.addListener((_, _, disabled) -> {
            if (disabled) {
                zoteroCompatibilityMode.set(false);
                inferCslStyleFromDocument.set(false);
            }
        });
    }

    @Override
    public void setValues() {
        pandocPath.set(openOfficePreferences.getPandocPath());
        boolean compatibilityMode = openOfficePreferences.getZoteroCompatibilityMode()
                && !zoteroCompatibilityModeDisabled.get();
        zoteroCompatibilityMode.set(compatibilityMode);
        inferCslStyleFromDocument.set(openOfficePreferences.shouldInferCslStyleFromDocument()
                && compatibilityMode);
    }

    @Override
    public void storeSettings() {
        openOfficePreferences.setPandocPath(pandocPath.get());
        boolean compatibilityMode = zoteroCompatibilityMode.get()
                && !zoteroCompatibilityModeDisabled.get();
        openOfficePreferences.setZoteroCompatibilityMode(compatibilityMode);
        openOfficePreferences.setInferCslStyleFromDocument(inferCslStyleFromDocument.get()
                && compatibilityMode);
    }

    public StringProperty pandocPathProperty() {
        return pandocPath;
    }

    public BooleanProperty zoteroCompatibilityModeProperty() {
        return zoteroCompatibilityMode;
    }

    public BooleanProperty inferCslStyleFromDocumentProperty() {
        return inferCslStyleFromDocument;
    }

    public BooleanBinding zoteroCompatibilityModeDisabledProperty() {
        return zoteroCompatibilityModeDisabled;
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

        task.onFailure(exception -> {
            LOGGER.warn("Auto-detection of pandoc path failed", exception);
            dialogService.notify(
                    Localization.lang("Auto-detection of pandoc path failed"));
        });

        taskExecutor.execute(task);
    }
}
