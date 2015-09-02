package net.sf.jabref.external;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.external.PushToApplication;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Jan 14, 2006
 * Time: 4:55:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToTeXstudio implements PushToApplication {

    private final String defaultCiteCommand = "\\cite";
    private JPanel settings = null;
    private final JTextField citeCommand = new JTextField(30);
    private final JTextField progPath = new JTextField(30);

    private boolean couldNotConnect = false, couldNotRunClient = false;


    @Override
    public String getName() {
        return Globals.lang("Insert selected citations into TeXstudio");
    }

    @Override
    public String getApplicationName() {
        return "TeXstudio";
    }

    @Override
    public String getTooltip() {
        return Globals.lang("Push selection to TeXstudio");
    }

    @Override
    public Icon getIcon() {
        return GUIGlobals.getImage("texstudio");
    }

    @Override
    public String getKeyStrokeName() {
        return "Push to TeXstudio";
    }

    private String defaultProgramPath() {
        if (Globals.ON_WIN) {
            String progFiles = System.getenv("ProgramFiles(x86)");
            if (progFiles == null) {
                progFiles = System.getenv("ProgramFiles");
            }
            return progFiles + "\\texstudio\\texstudio.exe";
        } else {
            return "texstudio";
        }
    }

    @Override
    public JPanel getSettingsPanel() {
        if (settings == null) {
            initSettingsPanel();
        }
        String citeCom = Globals.prefs.get("citeCommandTeXstudio");
        if (citeCom == null) {
            citeCom = defaultCiteCommand;
        }
        citeCommand.setText(citeCom);
        String programPath = Globals.prefs.get("TeXstudioPath");
        if (programPath == null) {
            programPath = defaultProgramPath();
        }
        progPath.setText(programPath);
        return settings;
    }

    @Override
    public void storeSettings() {
        Globals.prefs.put("citeCommandTeXstudio", citeCommand.getText().trim());
        Globals.prefs.put("TeXstudioPath", progPath.getText().trim());
    }

    private void initSettingsPanel() {
        DefaultFormBuilder builder = new DefaultFormBuilder(
                new FormLayout("left:pref, 4dlu, fill:pref", ""));
        builder.append(Globals.lang("Cite command") + ":");
        builder.append(citeCommand);
        builder.nextLine();
        builder.append(Globals.lang("Path to TeXstudio") + ":");
        builder.append(progPath);
        settings = builder.getPanel();
    }

    @Override
    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keys, MetaData metaData) {

        couldNotConnect = false;
        couldNotRunClient = false;
        String citeCom = Globals.prefs.get("citeCommandTeXstudio");
        if (citeCom == null) {
            citeCom = defaultCiteCommand;
        }
        String programPath = Globals.prefs.get("TeXstudioPath");
        if (programPath == null) {
            programPath = defaultProgramPath();
        }
        try {
            String[] com = Globals.ON_WIN ?
                    // No additional escaping is needed for TeXstudio:
                    new String[] {programPath, "--insert-cite", citeCom + "{" + keys + "}"}
                    : new String[] {programPath, "--insert-cite", citeCom + "{" + keys + "}"};

            /*for (int i = 0; i < com.length; i++) {
                String s = com[i];
                System.out.print(s + " ");
            }
            System.out.println("");*/

            final Process p = Runtime.getRuntime().exec(com);
            System.out.println(keys);
            Runnable errorListener = new Runnable() {

                @Override
                public void run() {
                    InputStream out = p.getErrorStream();
                    int c;
                    StringBuilder sb = new StringBuilder();
                    try {
                        while ((c = out.read()) != -1) {
                            sb.append((char) c);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Error stream has been closed. See if there were any errors:
                    if (sb.toString().trim().length() > 0) {
                        //System.out.println(sb.toString());
                        couldNotConnect = true;
                    }
                }
            };
            JabRefExecutorService.INSTANCE.executeAndWait(errorListener);
        } catch (IOException excep) {
            couldNotRunClient = true;
        }

    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    "TeXstudio: could not connect",
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        }
        else if (couldNotRunClient) {
            String programPath = Globals.prefs.get("TeXstudioPath");
            if (programPath == null) {
                programPath = defaultProgramPath();
            }
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    "TeXstudio: " + Globals.lang("Program '%0' not found", programPath),
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        }
        else {
            panel.output(Globals.lang("Pushed citations to TeXstudio"));
        }
    }

    @Override
    public boolean requiresBibtexKeys() {
        return true;
    }
}
