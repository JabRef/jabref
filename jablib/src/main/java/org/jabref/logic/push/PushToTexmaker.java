package org.jabref.logic.push;

import java.nio.file.Path;

import org.jabref.logic.util.NotificationService;

/**
 * Class for pushing entries into TexMaker.
 */
public class PushToTexmaker extends AbstractPushToApplication {

    public static final String NAME = "Texmaker";

    public PushToTexmaker(NotificationService dialogService, PushToApplicationPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "-insert", getCitePrefix() + keyString + getCiteSuffix()};
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "-line", Integer.toString(line), fileName.toString()};
    }
}
