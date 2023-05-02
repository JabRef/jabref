package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ExternalApplicationsPreferences {

    private final StringProperty eMailSubject;
    private final BooleanProperty shouldAutoOpenEmailAttachmentsFolder;
    private final StringProperty citeCommand;
    private final BooleanProperty useCustomTerminal;
    private final StringProperty customTerminalCommand;
    private final BooleanProperty useCustomFileBrowser;
    private final StringProperty customFileBrowserCommand;

    public ExternalApplicationsPreferences(String eMailSubject,
                                           boolean shouldAutoOpenEmailAttachmentsFolder,
                                           String citeCommand,
                                           boolean useCustomTerminal,
                                           String customTerminalCommand,
                                           boolean useCustomFileBrowser,
                                           String customFileBrowserCommand) {

        this.eMailSubject = new SimpleStringProperty(eMailSubject);
        this.shouldAutoOpenEmailAttachmentsFolder = new SimpleBooleanProperty(shouldAutoOpenEmailAttachmentsFolder);
        this.citeCommand = new SimpleStringProperty(citeCommand);
        this.useCustomTerminal = new SimpleBooleanProperty(useCustomTerminal);
        this.customTerminalCommand = new SimpleStringProperty(customTerminalCommand);
        this.useCustomFileBrowser = new SimpleBooleanProperty(useCustomFileBrowser);
        this.customFileBrowserCommand = new SimpleStringProperty(customFileBrowserCommand);
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

    public String getCiteCommand() {
        return citeCommand.get();
    }

    public StringProperty citeCommandProperty() {
        return citeCommand;
    }

    public void setCiteCommand(String citeCommand) {
        this.citeCommand.set(citeCommand);
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
}
