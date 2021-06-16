package org.jabref.preferences;

public class ExternalApplicationsPreferences {

    private final String eMailSubject;
    private final boolean shouldAutoOpenEmailAttachmentsFolder;
    private final String pushToApplicationName;
    private final String citeCommand;
    private final boolean useCustomTerminal;
    private final String customTerminalCommand;
    private final boolean useCustomFileBrowser;
    private final String customFileBrowserCommand;

    public ExternalApplicationsPreferences(String eMailSubject,
                                           boolean shouldAutoOpenEmailAttachmentsFolder,
                                           String pushToApplicationName,
                                           String citeCommand,
                                           boolean useCustomTerminal,
                                           String customTerminalCommand,
                                           boolean useCustomFileBrowser,
                                           String customFileBrowserCommand) {

        this.eMailSubject = eMailSubject;
        this.shouldAutoOpenEmailAttachmentsFolder = shouldAutoOpenEmailAttachmentsFolder;
        this.pushToApplicationName = pushToApplicationName;
        this.citeCommand = citeCommand;
        this.useCustomTerminal = useCustomTerminal;
        this.customTerminalCommand = customTerminalCommand;
        this.useCustomFileBrowser = useCustomFileBrowser;
        this.customFileBrowserCommand = customFileBrowserCommand;
    }

    public String getEmailSubject() {
        return eMailSubject;
    }

    public boolean shouldAutoOpenEmailAttachmentsFolder() {
        return shouldAutoOpenEmailAttachmentsFolder;
    }

    public String getPushToApplicationName() {
        return pushToApplicationName;
    }

    public String getCiteCommand() {
        return citeCommand;
    }

    public boolean useCustomTerminal() {
        return useCustomTerminal;
    }

    public String getCustomTerminalCommand() {
        return customTerminalCommand;
    }

    public boolean useCustomFileBrowser() {
        return useCustomFileBrowser;
    }

    public String getCustomFileBrowserCommand() {
        return customFileBrowserCommand;
    }
}
