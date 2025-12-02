package org.jabref.logic;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.util.strings.StringUtil;

/**
 * Preferences for the linked files
 */
public class FilePreferences {

    public static final String[] DEFAULT_FILENAME_PATTERNS = new String[] {"[bibtexkey]", "[bibtexkey] - [title]"};

    private final StringProperty userAndHost = new SimpleStringProperty();
    private final SimpleStringProperty mainFileDirectory = new SimpleStringProperty();
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
    private final BooleanProperty shouldDownloadCovers = new SimpleBooleanProperty();
    private final StringProperty coversDownloadLocation = new SimpleStringProperty();

    public FilePreferences(String userAndHost,
                           String mainFileDirectory,
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
                           boolean openFileExplorerInLastUsedDirectory,
                           boolean shouldDownloadCovers,
                           String coversDownloadLocation) {
        this.userAndHost.setValue(userAndHost);
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
        this.shouldDownloadCovers.set(shouldDownloadCovers);
        this.coversDownloadLocation.set(coversDownloadLocation);
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

    public boolean shouldDownloadCovers() {
        return shouldDownloadCovers.get();
    }

    public BooleanProperty shouldDownloadCoversProperty() {
        return shouldDownloadCovers;
    }

    public void setShouldDownloadCovers(boolean value) {
        this.shouldDownloadCovers.set(value);
    }

    public String coversDownloadLocation() {
        return coversDownloadLocation.get();
    }

    public StringProperty coversDownloadLocationProperty() {
        return coversDownloadLocation;
    }

    public void setOpenFileExplorerInLastUsedDirectory(String value) {
        this.coversDownloadLocation.set(value);
    }
}
