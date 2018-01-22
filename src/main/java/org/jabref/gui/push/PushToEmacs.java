package org.jabref.gui.push;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToEmacs extends AbstractPushToApplication implements PushToApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToEmacs.class);

    private final JTextField additionalParams = new JTextField(30);

    @Override
    public String getApplicationName() {
        return "Emacs";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("emacs");
    }

    @Override
    public JPanel getSettingsPanel() {
        additionalParams.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        return super.getSettingsPanel();
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, additionalParams.getText());
    }

    @Override
    protected void initSettingsPanel() {
        super.initSettingsPanel();
        builder.appendRows("2dlu, p, 2dlu, p");
        builder.add(Localization.lang("Additional parameters") + ":").xy(1, 3);
        builder.add(additionalParams).xy(3, 3);
        settings = builder.build();
    }

    @Override
    public void pushEntries(BibDatabase database, List<BibEntry> entries, String keys, MetaData metaData) {

        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        initParameters();
        commandPath = Globals.prefs.get(commandPathPreferenceKey);

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        commandPath = Globals.prefs.get(commandPathPreferenceKey);
        String[] addParams = Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS).split(" ");
        try {
            String[] com = new String[addParams.length + 2];
            com[0] = commandPath;
            System.arraycopy(addParams, 0, com, 1, addParams.length);
            String prefix;
            String suffix;
            prefix = "(with-current-buffer (window-buffer) (insert ";
            suffix = "))";

            com[com.length - 1] = OS.WINDOWS ?
            // Windows gnuclient/emacsclient escaping:
            // java string: "(insert \\\"\\\\cite{Blah2001}\\\")";
            // so cmd receives: (insert \"\\cite{Blah2001}\")
            // so emacs receives: (insert "\cite{Blah2001}")
            prefix.concat("\\\"\\" + getCiteCommand().replaceAll("\\\\", "\\\\\\\\") + "{" + keys + "}\\\"").concat(suffix) :
            // Linux gnuclient/emacslient escaping:
            // java string: "(insert \"\\\\cite{Blah2001}\")"
            // so sh receives: (insert "\\cite{Blah2001}")
            // so emacs receives: (insert "\cite{Blah2001}")
            prefix.concat("\"" + getCiteCommand().replaceAll("\\\\", "\\\\\\\\") + "{" + keys + "}\"").concat(suffix);

            final Process p = Runtime.getRuntime().exec(com);

            JabRefExecutorService.INSTANCE.executeAndWait(() -> {
                try (InputStream out = p.getErrorStream()) {
                    int c;
                    StringBuilder sb = new StringBuilder();
                    try {
                        while ((c = out.read()) != -1) {
                            sb.append((char) c);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Could not read from stderr.", e);
                    }
                    // Error stream has been closed. See if there were any errors:
                    if (!sb.toString().trim().isEmpty()) {
                        LOGGER.warn("Push to Emacs error: " + sb);
                        couldNotConnect = true;
                    }
                } catch (IOException e) {
                    LOGGER.warn("File problem.", e);
                }
            });
        } catch (IOException excep) {
            couldNotCall = true;
            LOGGER.warn("Problem pushing to Emacs.", excep);
        }
    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect) {
            JOptionPane.showMessageDialog(panel.frame(), "<HTML>" +
                    Localization.lang("Could not connect to a running gnuserv process. Make sure that "
                            + "Emacs or XEmacs is running,<BR>and that the server has been started "
                            + "(by running the command 'server-start'/'gnuserv-start').") + "</HTML>",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else if (couldNotCall) {
            JOptionPane.showMessageDialog(panel.frame(),
                    Localization.lang("Could not run the gnuclient/emacsclient program. Make sure you have "
                            + "the emacsclient/gnuclient program installed and available in the PATH."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else {
            super.operationCompleted(panel);
        }
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.EMACS_PATH;
    }

    @Override
    protected String getCommandName() {
        return "gnuclient " + Localization.lang("or") + " emacsclient";
    }

}
