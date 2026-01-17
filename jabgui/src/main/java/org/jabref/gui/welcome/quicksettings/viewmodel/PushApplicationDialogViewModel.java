package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.push.GuiPushToApplication;
import org.jabref.gui.push.GuiPushToApplications;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationDetector;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushApplicationDialogViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushApplicationDialogViewModel.class);

    private final ListProperty<GuiPushToApplication> applicationsProperty =
            new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<GuiPushToApplication> selectedApplicationProperty = new SimpleObjectProperty<>();
    private final StringProperty pathProperty = new SimpleStringProperty("");
    private final ObservableSet<GuiPushToApplication> detectedApplications = FXCollections.observableSet();

    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final PushToApplicationPreferences pushToApplicationPreferences;

    private final Map<PushToApplication, String> detectedApplicationPaths = new ConcurrentHashMap<>();
    private Future<?> detectionFuture;

    public PushApplicationDialogViewModel(GuiPreferences preferences, DialogService dialogService, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.pushToApplicationPreferences = preferences.getPushToApplicationPreferences();

        initializeApplications();
        detectApplications();
        setupPathUpdates();
    }

    private void initializeApplications() {
        List<GuiPushToApplication> allApplications = GuiPushToApplications.getAllGUIApplications(dialogService, pushToApplicationPreferences);
        applicationsProperty.setAll(allApplications);

        if (!pushToApplicationPreferences.getActiveApplicationName().isEmpty()) {
            allApplications.stream()
                           .filter(app -> app.getDisplayName().equals(pushToApplicationPreferences.getActiveApplicationName()))
                           .findFirst()
                           .ifPresent(selectedApplicationProperty::set);
        }
    }

    private void detectApplications() {
        detectionFuture = BackgroundTask
                .wrap(() -> PushToApplicationDetector.detectApplicationPaths(applicationsProperty))
                .onSuccess(detectedPaths -> {
                    detectedApplicationPaths.putAll(detectedPaths);
                    List<GuiPushToApplication> sortedApplications = new ArrayList<>(detectedPaths.keySet());
                    applicationsProperty.stream()
                                        .filter(app -> !detectedPaths.containsKey(app))
                                        .forEach(sortedApplications::add);
                    applicationsProperty.setAll(sortedApplications);
                    detectedApplications.clear();
                    detectedApplications.addAll(detectedPaths.keySet());
                    LOGGER.info("Application detection completed. Found {} applications", detectedPaths.size());
                })
                .onFailure(throwable -> LOGGER.warn("Application detection failed", throwable))
                .executeWith(taskExecutor);
    }

    private void setupPathUpdates() {
        selectedApplicationProperty.addListener((_, _, selectedApp) -> {
            if (selectedApp == null) {
                pathProperty.set("");
                return;
            }
            String existingPath = pushToApplicationPreferences.getCommandPaths().get(selectedApp.getDisplayName());
            pathProperty.set(PushToApplicationDetector.isValidAbsolutePath(existingPath) ?
                             existingPath :
                             Objects.requireNonNullElse(detectedApplicationPaths.get(selectedApp), ""));
        });
    }

    public ListProperty<GuiPushToApplication> applicationsProperty() {
        return applicationsProperty;
    }

    public StringProperty pathProperty() {
        return pathProperty;
    }

    public ObservableSet<GuiPushToApplication> detectedApplications() {
        return detectedApplications;
    }

    public void setPath(String path) {
        pathProperty.set(path);
    }

    public void setSelectedApplication(GuiPushToApplication application) {
        selectedApplicationProperty.set(application);
    }

    public void browseForApplication() {
        FileDialogConfiguration fileConfig = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();
        dialogService.showFileOpenDialog(fileConfig)
                     .ifPresent(selectedFile -> setPath(selectedFile.toString()));
    }

    public boolean isValidConfiguration() {
        if (selectedApplicationProperty.get() == null) {
            return false;
        }
        String pathText = pathProperty.get().trim();
        Path path = Path.of(pathText);
        return !pathText.isEmpty() && path.isAbsolute() && Files.exists(path);
    }

    public void saveSettings() {
        PushToApplication selectedApp = selectedApplicationProperty.get();
        if (selectedApp == null) {
            return;
        }
        pushToApplicationPreferences.setActiveApplicationName(selectedApp.getDisplayName());
        Map<String, String> commandPaths = new HashMap<>(pushToApplicationPreferences.getCommandPaths());
        commandPaths.put(selectedApp.getDisplayName(), pathProperty.get().trim());
        pushToApplicationPreferences.setCommandPaths(commandPaths);
    }

    public void cancelDetection() {
        if (detectionFuture != null) {
            detectionFuture.cancel(true);
        }
    }
}
