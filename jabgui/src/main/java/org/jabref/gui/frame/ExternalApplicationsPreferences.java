package org.jabref.gui.frame;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;

public class ExternalApplicationsPreferences {

    private final StringProperty eMailSubject;
    private final BooleanProperty shouldAutoOpenEmailAttachmentsFolder;

    private final BooleanProperty useCustomTerminal;
    private final StringProperty customTerminalCommand;
    private final BooleanProperty useCustomFileBrowser;
    private final StringProperty customFileBrowserCommand;
    private final StringProperty kindleEmail;
    private final ObservableSet<ExternalFileType> externalFileTypes = FXCollections.observableSet(new TreeSet<>(Comparator.comparing(ExternalFileType::getName)));

    public ExternalApplicationsPreferences(String eMailSubject,
                                           boolean shouldAutoOpenEmailAttachmentsFolder,
                                           Set<ExternalFileType> externalFileTypes,
                                           boolean useCustomTerminal,
                                           String customTerminalCommand,
                                           boolean useCustomFileBrowser,
                                           String customFileBrowserCommand,
                                           String kindleEmail) {

        this.eMailSubject = new SimpleStringProperty(eMailSubject);
        this.shouldAutoOpenEmailAttachmentsFolder = new SimpleBooleanProperty(shouldAutoOpenEmailAttachmentsFolder);
        this.externalFileTypes.addAll(externalFileTypes);
        this.useCustomTerminal = new SimpleBooleanProperty(useCustomTerminal);
        this.customTerminalCommand = new SimpleStringProperty(customTerminalCommand);
        this.useCustomFileBrowser = new SimpleBooleanProperty(useCustomFileBrowser);
        this.customFileBrowserCommand = new SimpleStringProperty(customFileBrowserCommand);
        this.kindleEmail = new SimpleStringProperty(kindleEmail);
    }

    private ExternalApplicationsPreferences() {
        this(
                Localization.lang("References"),     // eMailSubject
                OS.WINDOWS,                          // shouldAutoOpenEmailAttachmentsFolder
                Set.of(),                            // externalFileTypes
                false,                               // useCustomTerminal
                OS.WINDOWS
                ? "C:\\Program Files\\ConEmu\\ConEmu64.exe /single /dir \"%DIR\""
                : "",                            // customTerminalCommand
                false,                               // useCustomFileBrowser
                OS.WINDOWS
                ? "explorer.exe /select, \"%DIR\""
                : "",                            // customFileBrowserCommand
                ""                                   // kindleEmail
        );
    }

    public static ExternalApplicationsPreferences getDefault() {
        return new ExternalApplicationsPreferences();
    }

    public String getEmailSubject() {
        return eMailSubject.get();
    }

    public StringProperty eMailSubjectProperty() {
        return eMailSubject;
    }

    public void setEMailSubject(String eMailSubject) {
        this.eMailSubject.set(eMailSubject);
    }

    public boolean shouldAutoOpenEmailAttachmentsFolder() {
        return shouldAutoOpenEmailAttachmentsFolder.get();
    }

    public BooleanProperty autoOpenEmailAttachmentsFolderProperty() {
        return shouldAutoOpenEmailAttachmentsFolder;
    }

    public void setAutoOpenEmailAttachmentsFolder(boolean shouldAutoOpenEmailAttachmentsFolder) {
        this.shouldAutoOpenEmailAttachmentsFolder.set(shouldAutoOpenEmailAttachmentsFolder);
    }

    public boolean useCustomTerminal() {
        return useCustomTerminal.get();
    }

    public BooleanProperty useCustomTerminalProperty() {
        return useCustomTerminal;
    }

    public void setUseCustomTerminal(boolean useCustomTerminal) {
        this.useCustomTerminal.set(useCustomTerminal);
    }

    public String getCustomTerminalCommand() {
        return customTerminalCommand.get();
    }

    public StringProperty customTerminalCommandProperty() {
        return customTerminalCommand;
    }

    public void setCustomTerminalCommand(String customTerminalCommand) {
        this.customTerminalCommand.set(customTerminalCommand);
    }

    public boolean useCustomFileBrowser() {
        return useCustomFileBrowser.get();
    }

    public BooleanProperty useCustomFileBrowserProperty() {
        return useCustomFileBrowser;
    }

    public void setUseCustomFileBrowser(boolean useCustomFileBrowser) {
        this.useCustomFileBrowser.set(useCustomFileBrowser);
    }

    public String getCustomFileBrowserCommand() {
        return customFileBrowserCommand.get();
    }

    public StringProperty customFileBrowserCommandProperty() {
        return customFileBrowserCommand;
    }

    public void setCustomFileBrowserCommand(String customFileBrowserCommand) {
        this.customFileBrowserCommand.set(customFileBrowserCommand);
    }

    public String getKindleEmail() {
        return kindleEmail.get();
    }

    public StringProperty kindleEmailProperty() {
        return kindleEmail;
    }

    public void setKindleEmail(String kindleEmail) {
        this.kindleEmail.set(kindleEmail);
    }

    public ObservableSet<ExternalFileType> getExternalFileTypes() {
        return this.externalFileTypes;
    }
}
