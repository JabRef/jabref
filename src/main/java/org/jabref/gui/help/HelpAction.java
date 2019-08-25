package org.jabref.gui.help;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.Globals;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.help.HelpFile;
import org.jabref.preferences.JabRefPreferences;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends SimpleCommand {

    /**
     * New languages of the help have to be added here
     */
    private static final Set<String> AVAILABLE_LANG_FILES = Stream.of("en", "de", "fr", "in", "ja")
                                                                  .collect(Collectors.toCollection(HashSet::new));

    private HelpFile helpPage;

    public HelpAction(HelpFile helpPage) {
        this.helpPage = helpPage;
    }

    public static void openHelpPage(HelpFile helpPage) {
        String lang = Globals.prefs.get(JabRefPreferences.LANGUAGE);
        StringBuilder sb = new StringBuilder("https://help.jabref.org/");

        if (AVAILABLE_LANG_FILES.contains(lang)) {
            sb.append(lang);
            sb.append("/");
        } else {
            sb.append("en/");
        }
        sb.append(helpPage.getPageName());
        JabRefDesktop.openBrowserShowPopup(sb.toString());
    }

    public static SimpleCommand getMainHelpPageCommand() {
        return new SimpleCommand() {
            @Override
            public void execute() {
                openHelpPage(HelpFile.CONTENTS);
            }
        };
    }

    @Override
    public void execute() {
        openHelpPage(helpPage);
    }
}
