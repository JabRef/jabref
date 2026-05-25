package org.jabref.logic;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.Directories;
import org.jabref.model.metadata.UserHostInfo;

import org.jspecify.annotations.NullMarked;

/// Preferences for the linked files
@NullMarked
public class FilePreferences {

    public static final String[] DEFAULT_FILENAME_PATTERNS = new String[] {"[bibtexkey]", "[bibtexkey] - [title]"};

    private final SimpleObjectProperty<UserHostInfo> userAndHost = new SimpleObjectProperty<>();
    private final ObjectProperty<Path> mainFileDirectory = new SimpleObjectProperty<>();
    private final BooleanProperty storeFilesRelativeToBibFile = new SimpleBooleanProperty();
    private final BooleanProperty autoRenameFilesOnChange = new SimpleBooleanProperty();
    private final StringProperty fileNamePattern = new SimpleStringProperty();
    private final StringProperty fileDirectoryPattern = new SimpleStringProperty();
    private final BooleanProperty downloadLinkedFiles = new SimpleBooleanProperty();
    private final BooleanProperty fulltextIndexLinkedFiles = new SimpleBooleanProperty();
    private final ObjectProperty<Path> workingDirectory = new SimpleObjectProperty<>();
    private final BooleanProperty createBackup = new SimpleBooleanProperty();
    private final ObjectProperty<Path> backupDirectory = new SimpleObjectProperty<>();
    private final BooleanProperty confirmDeleteLinkedFile = new SimpleBooleanProperty();
    private final BooleanProperty moveToTrash = new SimpleBooleanProperty();

    private final BooleanProperty adjustFileLinksOnTransfer = new SimpleBooleanProperty();
    private final BooleanProperty copyLinkedFilesOnTransfer = new SimpleBooleanProperty();
    private final BooleanProperty moveLinkedFilesOnTransfer = new SimpleBooleanProperty();

    private final BooleanProperty shouldKeepDownloadUrl = new SimpleBooleanProperty();
    private final ObjectProperty<Path> lastUsedDirectory = new SimpleObjectProperty<>();
    private final BooleanProperty openFileExplorerInFileDirectory = new SimpleBooleanProperty();
    private final BooleanProperty openFileExplorerInLastUsedDirectory = new SimpleBooleanProperty();

    private FilePreferences() {
        this(
                new SimpleObjectProperty<>(OS.getUserHostInfo()), // userAndHost (needs to be sourced from InternalPreferences)
                Path.of("/"),                        // mainFileDirectory
                true,                                // storeFilesRelativeToBibFile
                false,                               // autoRenameFilesOnChange
                DEFAULT_FILENAME_PATTERNS[1],        // fileNamePattern
                "",                                  // fileDirectoryPattern
                true,                                // downloadLinkedFiles
                true,                                // fulltextIndexLinkedFiles
                Directories.getUserDirectory(),      // workingDirectory
                true,                                // createBackup
                Directories.getBackupDirectory(),    // backupDirectory
                true,                                // confirmDeleteLinkedFile
                false,                               // moveToTrash
                true,                                // adjustFileLinksOnTransfer
                true,                                // copyLinkedFilesOnTransfer
                false,                               // moveLinkedFilesOnTransfer - defensive, not to cause the impression of files being lost
                true,                                // shouldKeepDownloadUrl
                Path.of("/"),                        // lastUsedDirectory
                true,                                // openFileExplorerInFileDirectory
                false                                // openFileExplorerInLastUsedDirectory
        );
    }

    public FilePreferences(ReadOnlyObjectProperty<UserHostInfo> userAndHost,
                           Path mainFileDirectory,
                           boolean storeFilesRelativeToBibFile,
                           boolean autoRenameFilesOnChange,
                           String fileNamePattern,
                           String fileDirectoryPattern,
                           boolean downloadLinkedFiles,
                           boolean fulltextIndexLinkedFiles,
                           Path workingDirectory,
                           boolean createBackup,
                           Path backupDirectory,
                           boolean confirmDeleteLinkedFile,
                           boolean moveToTrash,
                           boolean adjustFileLinksOnTransfer,
                           boolean copyLinkedFilesOnTransfer,
                           boolean moveFilesOnTransferProperty,
                           boolean shouldKeepDownloadUrl,
                           Path lastUsedDirectory,
                           boolean openFileExplorerInFileDirectory,
                           boolean openFileExplorerInLastUsedDirectory) {
        this.userAndHost.bind(userAndHost);
        this.mainFileDirectory.setValue(mainFileDirectory);
        this.storeFilesRelativeToBibFile.setValue(storeFilesRelativeToBibFile);
        this.autoRenameFilesOnChange.setValue(autoRenameFilesOnChange);
        this.fileNamePattern.setValue(fileNamePattern);
        this.fileDirectoryPattern.setValue(fileDirectoryPattern);
        this.downloadLinkedFiles.setValue(downloadLinkedFiles);
        this.fulltextIndexLinkedFiles.setValue(fulltextIndexLinkedFiles);
        this.workingDirectory.setValue(workingDirectory);
        this.createBackup.setValue(createBackup);
        this.backupDirectory.setValue(backupDirectory);
        this.confirmDeleteLinkedFile.setValue(confirmDeleteLinkedFile);
        this.moveToTrash.setValue(moveToTrash);
        this.adjustFileLinksOnTransfer.setValue(adjustFileLinksOnTransfer);
        this.copyLinkedFilesOnTransfer.setValue(copyLinkedFilesOnTransfer);
        this.moveLinkedFilesOnTransfer.setValue(moveFilesOnTransferProperty);
        this.shouldKeepDownloadUrl.setValue(shouldKeepDownloadUrl);
        this.lastUsedDirectory.setValue(lastUsedDirectory);
        this.openFileExplorerInFileDirectory.set(openFileExplorerInFileDirectory);
        this.openFileExplorerInLastUsedDirectory.set(openFileExplorerInLastUsedDirectory);
    }

    public static FilePreferences getDefault() {
        return new FilePreferences();
    }

    public void setAll(FilePreferences preferences) {
        // userAndHost is always bound to InternalPreferences.getUserAndHost
        this.mainFileDirectory.set(preferences.mainFileDirectoryProperty().get());
        this.storeFilesRelativeToBibFile.set(preferences.shouldStoreFilesRelativeToBibFile());
        this.autoRenameFilesOnChange.set(preferences.shouldAutoRenameFilesOnChange());
        this.fileNamePattern.set(preferences.getFileNamePattern());
        this.fileDirectoryPattern.set(preferences.getFileDirectoryPattern());
        this.downloadLinkedFiles.set(preferences.shouldDownloadLinkedFiles());
        this.fulltextIndexLinkedFiles.set(preferences.shouldFulltextIndexLinkedFiles());
        this.workingDirectory.set(preferences.getWorkingDirectory());
        this.createBackup.set(preferences.shouldCreateBackup());
        this.backupDirectory.set(preferences.getBackupDirectory());
        this.confirmDeleteLinkedFile.set(preferences.confirmDeleteLinkedFile());
        this.moveToTrash.set(preferences.moveToTrash());
        this.adjustFileLinksOnTransfer.set(preferences.shouldAdjustFileLinksOnTransfer());
        this.copyLinkedFilesOnTransfer.set(preferences.shouldCopyLinkedFilesOnTransfer());
        this.moveLinkedFilesOnTransfer.set(preferences.shouldMoveLinkedFilesOnTransfer());
        this.shouldKeepDownloadUrl.set(preferences.shouldKeepDownloadUrl());
        this.lastUsedDirectory.set(preferences.getLastUsedDirectory());
        this.openFileExplorerInFileDirectory.set(preferences.shouldOpenFileExplorerInFileDirectory());
        this.openFileExplorerInLastUsedDirectory.set(preferences.shouldOpenFileExplorerInLastUsedDirectory());
    }

    public String getUserAndHost() {
        return userAndHost.getValue().getUserHostString();
    }

    public Path getMainFileDirectory() {
        return mainFileDirectory.get();
    }

    public ObjectProperty<Path> mainFileDirectoryProperty() {
        return mainFileDirectory;
    }

    public void setMainFileDirectory(Path mainFileDirectory) {
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

    public boolean shouldAutoRenameFilesOnChange() {
        return autoRenameFilesOnChange.get();
    }

    public BooleanProperty autoRenameFilesOnChangeProperty() {
        return autoRenameFilesOnChange;
    }

    public void setAutoRenameFilesOnChange(boolean autoRenameFilesOnChange) {
        this.autoRenameFilesOnChange.set(autoRenameFilesOnChange);
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

    public boolean shouldAdjustFileLinksOnTransfer() {
        return adjustFileLinksOnTransfer.get();
    }

    public BooleanProperty adjustFileLinksOnTransferProperty() {
        return adjustFileLinksOnTransfer;
    }

    public void setAdjustFileLinksOnTransfer(boolean adjustFileLinksOnTransfer) {
        this.adjustFileLinksOnTransfer.set(adjustFileLinksOnTransfer);
    }

    public boolean shouldCopyLinkedFilesOnTransfer() {
        return copyLinkedFilesOnTransfer.get();
    }

    public BooleanProperty copyLinkedFilesOnTransferProperty() {
        return copyLinkedFilesOnTransfer;
    }

    public void setCopyLinkedFilesOnTransfer(boolean copyLinkedFilesOnTransfer) {
        this.copyLinkedFilesOnTransfer.set(copyLinkedFilesOnTransfer);
    }

    public boolean shouldMoveLinkedFilesOnTransfer() {
        return moveLinkedFilesOnTransfer.get();
    }

    public BooleanProperty moveLinkedFilesOnTransferPropertyProperty() {
        return moveLinkedFilesOnTransfer;
    }

    public void setMoveLinkedFilesOnTransfer(boolean moveLinkedFilesOnTransfer) {
        this.moveLinkedFilesOnTransfer.set(moveLinkedFilesOnTransfer);
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

    public Path getLastUsedDirectory() {
        return lastUsedDirectory.get();
    }

    public ObjectProperty<Path> lastUsedDirectoryProperty() {
        return lastUsedDirectory;
    }

    public void setLastUsedDirectory(Path lastUsedDirectory) {
        this.lastUsedDirectory.set(lastUsedDirectory);
    }

    public boolean shouldOpenFileExplorerInFileDirectory() {
        return openFileExplorerInFileDirectory.get();
    }

    public BooleanProperty openFileExplorerInFileDirectoryProperty() {
        return openFileExplorerInFileDirectory;
    }

    public void setOpenFileExplorerInFileDirectory(boolean value) {
        this.openFileExplorerInFileDirectory.set(value);
    }

    public boolean shouldOpenFileExplorerInLastUsedDirectory() {
        return openFileExplorerInLastUsedDirectory.get();
    }

    public BooleanProperty openFileExplorerInLastUsedDirectoryProperty() {
        return openFileExplorerInLastUsedDirectory;
    }

    public void setOpenFileExplorerInLastUsedDirectory(boolean value) {
        this.openFileExplorerInLastUsedDirectory.set(value);
    }

    public FilePreferences withUserHostInfo(ReadOnlyObjectProperty<UserHostInfo> newUserHostInfo) {
        this.userAndHost.bind(newUserHostInfo);
        return this;
    }

    public FilePreferences withMoveToTrash(boolean moveToTrash) {
        this.moveToTrash.set(moveToTrash);
        return this;
    }

    public FilePreferences withMainFileDirectory(Path lastUsedDirectory) {
        this.workingDirectory.set(lastUsedDirectory);
        return this;
    }

    public FilePreferences withLastUsedDirectory(Path lastUsedDirectory) {
        this.lastUsedDirectory.set(lastUsedDirectory);
        return this;
    }
}
