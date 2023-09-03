package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ExternalApplicationsPreferences {

    private final StringProperty eMailSubject;
    private final BooleanProperty shouldAutoOpenEmailAttachmentsFolder;
    private final StringProperty citeCommand;

    private final StringProperty startCharacter;

    private final StringProperty endCharacter;

    private final StringProperty delimiter;

    private final BooleanProperty useCustomTerminal;
    private final StringProperty customTerminalCommand;
    private final BooleanProperty useCustomFileBrowser;
    private final StringProperty customFileBrowserCommand;
    private final StringProperty kindleEmail;

    public ExternalApplicationsPreferences(String eMailSubject,
                                           boolean shouldAutoOpenEmailAttachmentsFolder,
                                           String citeCommand,
                                           boolean useCustomTerminal,
                                           String customTerminalCommand,
                                           boolean useCustomFileBrowser,
                                           String customFileBrowserCommand,
                                           String kindleEmail,
                                           String startCharacter,
                                           String endCharacter,
                                           String delimiter) {

        this.eMailSubject = new SimpleStringProperty(eMailSubject);
        this.shouldAutoOpenEmailAttachmentsFolder = new SimpleBooleanProperty(shouldAutoOpenEmailAttachmentsFolder);
        this.citeCommand = new SimpleStringProperty(citeCommand);
        this.useCustomTerminal = new SimpleBooleanProperty(useCustomTerminal);
        this.customTerminalCommand = new SimpleStringProperty(customTerminalCommand);
        this.useCustomFileBrowser = new SimpleBooleanProperty(useCustomFileBrowser);
        this.customFileBrowserCommand = new SimpleStringProperty(customFileBrowserCommand);
        this.kindleEmail = new SimpleStringProperty(kindleEmail);

        this.startCharacter = new SimpleStringProperty(startCharacter);
        this.endCharacter = new SimpleStringProperty(endCharacter);
        this.delimiter = new SimpleStringProperty(delimiter);
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

    public String getDelimiter() {
        return this.delimiter.getValue();
    }

    public String getStartCharacter() {
        return this.startCharacter.getValue();
    }

    public String getEndCharacter() {
        return this.endCharacter.getValue();
    }

    public StringProperty delimiterProperty() {
        return this.delimiter;
    }

    public StringProperty startCharacterProperty() {
        return this.startCharacter;
    }

    public StringProperty endCharacterProperty() {
        return this.endCharacter;
    }

    public StringProperty citeCommandProperty() {
        return citeCommand;
    }

    public void setCiteCommand(String citeCommand) {
        this.citeCommand.set(citeCommand);
    }

    public void setStartCharacter(String startCharacter) {
        this.startCharacter.setValue(startCharacter);
    }

    public void setEndCharacter(String endCharacter) {
        this.endCharacter.setValue(endCharacter);
    }

    public void setDelimiter(String delimiter) {
        this.delimiter.setValue(delimiter);
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
}
