package org.jabref.logic;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.strings.StringUtil;

/**
 *  Preferences for the linked files
 */
public class FilePreferences {

    public static final String[] DEFAULT_FILENAME_PATTERNS = new String[] {"[bibtexkey]", "[bibtexkey] - [title]"};

    private final StringProperty userAndHost = new SimpleStringProperty();
    private final SimpleStringProperty mainFileDirectory = new SimpleStringProperty();
    private final BooleanProperty storeFilesRelativeToBibFile = new SimpleBooleanProperty();
    private final StringProperty fileNamePattern = new SimpleStringProperty();
    private final StringProperty fileDirectoryPattern = new SimpleStringProperty();
    private final BooleanProperty downloadLinkedFiles = new SimpleBooleanProperty();
    private final BooleanProperty fulltextIndexLinkedFiles = new SimpleBooleanProperty();
    private final ObjectProperty<Path> workingDirectory = new SimpleObjectProperty<>();
    private final BooleanProperty createBackup = new SimpleBooleanProperty();
    private final ObjectProperty<Path> backupDirectory = new SimpleObjectProperty<>();
    private final BooleanProperty confirmDeleteLinkedFile = new SimpleBooleanProperty();
    private final BooleanProperty moveToTrash = new SimpleBooleanProperty();
    private final BooleanProperty shouldKeepDownloadUrl = new SimpleBooleanProperty();

    public FilePreferences(String userAndHost,
                           String mainFileDirectory,
                           boolean storeFilesRelativeToBibFile,
                           String fileNamePattern,
                           String fileDirectoryPattern,
                           boolean downloadLinkedFiles,
                           boolean fulltextIndexLinkedFiles,
                           Path workingDirectory,
                           boolean createBackup,
                           Path backupDirectory,
                           boolean confirmDeleteLinkedFile,
                           boolean moveToTrash,
                           boolean shouldKeepDownloadUrl) {
        this.userAndHost.setValue(userAndHost);
        this.mainFileDirectory.setValue(mainFileDirectory);
        this.storeFilesRelativeToBibFile.setValue(storeFilesRelativeToBibFile);
        this.fileNamePattern.setValue(fileNamePattern);
        this.fileDirectoryPattern.setValue(fileDirectoryPattern);
        this.downloadLinkedFiles.setValue(downloadLinkedFiles);
        this.fulltextIndexLinkedFiles.setValue(fulltextIndexLinkedFiles);
        this.workingDirectory.setValue(workingDirectory);
        this.createBackup.setValue(createBackup);
        this.backupDirectory.setValue(backupDirectory);
        this.confirmDeleteLinkedFile.setValue(confirmDeleteLinkedFile);
        this.moveToTrash.setValue(moveToTrash);
        this.shouldKeepDownloadUrl.setValue(shouldKeepDownloadUrl);
    }

    public String getUserAndHost() {
        return userAndHost.getValue();
    }

    public StringProperty getUserAndHostProperty() {
        return userAndHost;
    }

    public Optional<Path> getMainFileDirectory() {
        if (StringUtil.isBlank(mainFileDirectory.getValue())) {
            return Optional.empty();
        } else {
            return Optional.of(Path.of(mainFileDirectory.getValue()));
        }
    }

    public StringProperty mainFileDirectoryProperty() {
        return mainFileDirectory;
    }

    public void setMainFileDirectory(String mainFileDirectory) {
        this.mainFileDirectory.set(mainFileDirectory);
    }

    public boolean shouldStoreFilesRelativeToBibFile() {
        return storeFilesRelativeToBibFile.get();
    }

    public BooleanProperty storeFilesRelativeToBibFileProperty() {
        return storeFilesRelativeToBibFile;
    }

    public void setStoreFilesRelativeToBibFile(boolean shouldStoreFilesRelativeToBibFile) {
        this.storeFilesRelativeToBibFile.set(shouldStoreFilesRelativeToBibFile);
    }

    public String getFileNamePattern() {
        return fileNamePattern.get();
    }

    public StringProperty fileNamePatternProperty() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern.set(fileNamePattern);
    }

    public String getFileDirectoryPattern() {
        return fileDirectoryPattern.get();
    }

    public StringProperty fileDirectoryPatternProperty() {
        return fileDirectoryPattern;
    }

    public void setFileDirectoryPattern(String fileDirectoryPattern) {
        this.fileDirectoryPattern.set(fileDirectoryPattern);
    }

    public boolean shouldDownloadLinkedFiles() {
        return downloadLinkedFiles.get();
    }

    public BooleanProperty downloadLinkedFilesProperty() {
        return downloadLinkedFiles;
    }

    public void setDownloadLinkedFiles(boolean shouldDownloadLinkedFiles) {
        this.downloadLinkedFiles.set(shouldDownloadLinkedFiles);
    }

    public boolean shouldFulltextIndexLinkedFiles() {
        return fulltextIndexLinkedFiles.get();
    }

    public BooleanProperty fulltextIndexLinkedFilesProperty() {
        return fulltextIndexLinkedFiles;
    }

    public void setFulltextIndexLinkedFiles(boolean shouldFulltextIndexLinkedFiles) {
        this.fulltextIndexLinkedFiles.set(shouldFulltextIndexLinkedFiles);
    }

    public Path getWorkingDirectory() {
        return workingDirectory.get();
    }

    public ObjectProperty<Path> workingDirectoryProperty() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory.set(workingDirectory);
    }

    public void setCreateBackup(boolean createBackup) {
        this.createBackup.set(createBackup);
    }

    public boolean shouldCreateBackup() {
        return this.createBackup.getValue();
    }

    public BooleanProperty createBackupProperty() {
        return this.createBackup;
    }

    public ObjectProperty<Path> backupDirectoryProperty() {
        return this.backupDirectory;
    }

    public void setBackupDirectory(Path backupPath) {
        this.backupDirectory.set(backupPath);
    }

    public Path getBackupDirectory() {
        return this.backupDirectory.getValue();
    }

    public boolean confirmDeleteLinkedFile() {
        return confirmDeleteLinkedFile.get();
    }

    public BooleanProperty confirmDeleteLinkedFileProperty() {
        return confirmDeleteLinkedFile;
    }

    public void confirmDeleteLinkedFile(boolean confirmLinkedFileDelete) {
        this.confirmDeleteLinkedFile.set(confirmLinkedFileDelete);
    }

    public boolean moveToTrash() {
        return moveToTrash.get();
    }

    public BooleanProperty moveToTrashProperty() {
        return moveToTrash;
    }

    public void moveToTrash(boolean moveToTrash) {
        this.moveToTrash.set(moveToTrash);
    }

    public boolean shouldKeepDownloadUrl() {
        return shouldKeepDownloadUrl.get();
    }

    public BooleanProperty shouldKeepDownloadUrlProperty() {
        return shouldKeepDownloadUrl;
    }

    public void setKeepDownloadUrl(boolean shouldKeepDownloadUrl) {
        this.shouldKeepDownloadUrl.set(shouldKeepDownloadUrl);
    }
}
