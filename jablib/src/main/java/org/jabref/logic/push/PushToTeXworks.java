package org.jabref.logic.push;

import java.nio.file.Path;

import org.jabref.logic.util.NotificationService;

public class PushToTeXworks extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.TEXWORKS;

    /**
     * Constructs a new {@code PushToTeXworks} instance.
     *
     * @param notificationService The dialog service for displaying messages to the user.
     * @param preferences         The service for accessing user preferences.
     */
    public PushToTeXworks(NotificationService notificationService, PushToApplicationPreferences preferences) {
        super(notificationService, preferences);
    }

    @Override
    public String getDisplayName() {
        return APPLICATION.getDisplayName();
    }

    @Override
    public String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-text", "%s%s%s".formatted(getCitePrefix(), keyString, getCiteSuffix())};
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "--position=\"%s\"".formatted(line), fileName.toString()};
    }
}
