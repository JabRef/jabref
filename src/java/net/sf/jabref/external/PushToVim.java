package net.sf.jabref.external;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 7, 2007
 * Time: 6:55:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToVim implements PushToApplication {

    private JPanel settings = null;
    private JTextField vimPath = new JTextField(30),
        vimServer = new JTextField(30),
        citeCommand = new JTextField(30);

    private boolean couldNotConnect=false, couldNotRunClient=false;

    public String getName() {
        return Globals.lang("Insert selected citations into Vim") ;
    }

    public String getApplicationName() {
        return "Vim";
    }

    public String getTooltip() {
        return Globals.lang("Push selection to Vim");
    }

    public Icon getIcon() {
        return GUIGlobals.getImage("vim");
    }

    public String getKeyStrokeName() {
        return null;
    }

    public JPanel getSettingsPanel() {
        if (settings == null)
            initSettingsPanel();
        vimPath.setText(Globals.prefs.get("vim"));
        vimServer.setText(Globals.prefs.get("vimServer"));
        citeCommand.setText(Globals.prefs.get("citeCommandVim"));
        return settings;
    }
    
    public void storeSettings() {
        Globals.prefs.put("vim", vimPath.getText());
        Globals.prefs.put("vimServer", vimServer.getText());
        Globals.prefs.put("citeCommandVim", citeCommand.getText());
    }

    private void initSettingsPanel() {
        DefaultFormBuilder builder = new DefaultFormBuilder(
                new FormLayout("left:pref, 4dlu, fill:pref, 4dlu, fill:pref", ""));

        builder.append(new JLabel(Globals.lang("Path to Vim") + ":"));
        builder.append(vimPath);
        BrowseAction action = new BrowseAction(null, vimPath, false);
        JButton browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(action);
        builder.append(browse);
        builder.nextLine();
        builder.append(Globals.lang("Vim Server Name") + ":");
        builder.append(vimServer);
        builder.nextLine();
        builder.append(Globals.lang("Cite command") + ":");
        builder.append(citeCommand);
        settings = builder.getPanel();
    }

    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keys,
                            MetaData metaData) {

        couldNotConnect=false;
        couldNotRunClient=false;
        try {
                String[] com = new String[] {Globals.prefs.get("vim"), "--servername", Globals.prefs.get("vimServer"), "--remote-send",
                "<C-\\><C-N>a" + Globals.prefs.get("citeCommandVim") +
                       "{" + keys + "}"};

            final Process p = Runtime.getRuntime().exec(com);

            Runnable errorListener = new Runnable() {
                public void run() {
                    InputStream out = p.getErrorStream();
                    int c;
                    StringBuffer sb = new StringBuffer();
                    try {
                        while ((c = out.read()) != -1)
                            sb.append((char) c);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Error stream has been closed. See if there were any errors:
                    if (sb.toString().trim().length() > 0) {
			System.out.println(sb.toString());
                        couldNotConnect = true;
                        return;
                    }
                }
            };
            Thread t = new Thread(errorListener);
            t.start();
            t.join();
        }
        catch (IOException excep) {
            couldNotRunClient = true;
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect)
            JOptionPane.showMessageDialog(
                panel.frame(),
                "<HTML>"+
                Globals.lang("Could not connect to Vim server. Make sure that "
                +"Vim is running<BR>with correct server name."
                +"</HTML>"),
                Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        else if (couldNotRunClient)
            JOptionPane.showMessageDialog(
                panel.frame(),
                Globals.lang("Could not run the 'vim' program."),
                Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        else {
            panel.output(Globals.lang("Pushed citations to Vim"));
        }
    }

    public boolean requiresBibtexKeys() {
        return true;
    }
}
