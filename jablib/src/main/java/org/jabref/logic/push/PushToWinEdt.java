package org.jabref.logic.push;

import java.nio.file.Path;

import org.jabref.logic.util.NotificationService;

public class PushToWinEdt extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.WIN_EDT;

    public PushToWinEdt(NotificationService notificationService, PushToApplicationPreferences preferences) {
        super(notificationService, preferences);
    }

    @Override
    public String getDisplayName() {
        return APPLICATION.getDisplayName();
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath,
                "\"[InsText('" + getCitePrefix() + keyString.replace("'", "''") + getCiteSuffix() + "');]\""};
    }

    @Override
    public String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "\"[Open(|%s|);SelLine(%s,%s);]\"".formatted(fileName.toString(), line, column)};
    }
}
