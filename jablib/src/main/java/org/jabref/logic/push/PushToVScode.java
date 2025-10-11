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
        // TODO - Implementing this will fix https://github.com/JabRef/jabref/issues/6775
        return new String[] {commandPath};
    }

    @Override
    public String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "--g", "%s:%s:%s".formatted(fileName.toString(), line, column)};
    }
}
