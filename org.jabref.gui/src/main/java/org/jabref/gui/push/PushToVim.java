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
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToVim extends AbstractPushToApplication implements PushToApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToVim.class);

    private final JTextField vimServer = new JTextField(30);

    @Override
    public String getApplicationName() {
        return "Vim";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("vim");
    }

    @Override
    public JPanel getSettingsPanel() {
        vimServer.setText(Globals.prefs.get(JabRefPreferences.VIM_SERVER));
        return super.getSettingsPanel();
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.VIM_SERVER, vimServer.getText());
    }

    @Override
    protected void initSettingsPanel() {
        super.initSettingsPanel();
        builder.appendRows("2dlu, p");
        builder.add(Localization.lang("Vim server name") + ":").xy(1, 3);
        builder.add(vimServer).xy(3, 3);
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

        try {
            String[] com = new String[] {commandPath, "--servername",
                    Globals.prefs.get(JabRefPreferences.VIM_SERVER), "--remote-send",
                    "<C-\\><C-N>a" + getCiteCommand() +
                    "{" + keys + "}"};

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
                        LOGGER.warn("Push to Vim error: " + sb);
                        couldNotConnect = true;
                    }
                } catch (IOException e) {
                    LOGGER.warn("File problem.", e);
                }
            });
        } catch (IOException excep) {
            couldNotCall = true;
            LOGGER.warn("Problem pushing to Vim.", excep);
        }

    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    "<HTML>" +
                            Localization.lang("Could not connect to Vim server. Make sure that "
                                    + "Vim is running<BR>with correct server name.")
                    + "</HTML>",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else if (couldNotCall) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    Localization.lang("Could not run the 'vim' program."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else {
            super.operationCompleted(panel);
        }
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.VIM;
    }

}
