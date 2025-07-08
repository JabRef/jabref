package org.jabref.logic.push;

import java.nio.file.Path;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.NotificationService;

public class PushToTeXstudio extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.TEXSTUDIO;

    public PushToTeXstudio(NotificationService dialogService, CliPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-cite", "%s%s%s".formatted(getCitePrefix(), keyString, getCiteSuffix())};
    }

    @Override
    public String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "--line", Integer.toString(line), fileName.toString()};
    }
}
