package org.jabref.logic.push;

import java.nio.file.Path;

import org.jabref.logic.util.NotificationService;

public class PushToVScode extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.VSCODE;

    public PushToVScode(NotificationService notificationService, PushToApplicationPreferences preferences) {
        super(notificationService, preferences);
    }

    @Override
    public String getDisplayName() {
        return APPLICATION.getDisplayName();
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        // VS Code offers no command line option to insert text into the open editor, so pushing an entry can only
        // bring VS Code to the front. Inserting the citation is tracked at https://github.com/JabRef/jabref/issues/15348
        return new String[] {commandPath};
    }

    @Override
    public String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "--g", "%s:%s:%s".formatted(fileName.toString(), line, column)};
    }
}
