package net.sf.jabref.external;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;

import net.sf.jabref.*;

public class PushToLyx implements PushToApplication {

    private JTextField lyxPipe=new JTextField(30);
    private JPanel settings = null;

    private boolean couldNotFindPipe=false;
    private boolean couldNotWrite=false;
    private String message = "";

    public void pushEntries(BibtexDatabase database, final BibtexEntry[] entries, final String keyString, MetaData metaData) {

        couldNotFindPipe = false;
        couldNotWrite = false;

        String lyxpipeSetting = Globals.prefs.get("lyxpipe");
        if (!lyxpipeSetting.endsWith(".in"))
            lyxpipeSetting = lyxpipeSetting+".in";
        File lp = new File(lyxpipeSetting); // this needs to fixed because it gives "asdf" when going prefs.get("lyxpipe")
        if( !lp.exists() || !lp.canWrite()){
            // See if it helps to append ".in":
            lp = new File(lyxpipeSetting+".in");
            if( !lp.exists() || !lp.canWrite()){
                couldNotFindPipe = true;
                return;
            }
        }

        final File lyxpipe = lp;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    FileWriter fw = new FileWriter(lyxpipe);
                    BufferedWriter lyx_out = new BufferedWriter(fw);
                    String citeStr = "";

                    citeStr = "LYXCMD:sampleclient:citation-insert:" + keyString;
                    lyx_out.write(citeStr + "\n");

                    lyx_out.close();

                } catch (IOException excep) {
                    couldNotWrite = true;
                    return;
                }
            }
        });


	    t.start();
	    /*new Timeout(2000, t, Globals.lang("Error")+": "+
            Globals.lang("unable to access LyX-pipe"));*/
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return Globals.lang("Insert selected citations into LyX/Kile");
    }

    public String getApplicationName() {
        return "LyX/Kile";
    }

    public String getTooltip() {
        return Globals.lang("Push selection to LyX/Kile");
    }

    public Icon getIcon() {
        return GUIGlobals.getImage("lyx");
    }

    public String getKeyStrokeName() {
        return "Push to LyX";
    }


    public void operationCompleted(BasePanel panel) {
        if (couldNotFindPipe) {
            panel.output(Globals.lang("Error") + ": " + Globals.lang("verify that LyX is running and that the lyxpipe is valid")
                    + ". [" + Globals.prefs.get("lyxpipe") + "]");
        } else if (couldNotWrite) {
            panel.output(Globals.lang("Error") + ": " + Globals.lang("unable to write to") + " " + Globals.prefs.get("lyxpipe") +
                    ".in");
        } else {

            panel.output(Globals.lang("Pushed the citations for the following rows to") + " Lyx: " +
                    message);
        }

    }

    public boolean requiresBibtexKeys() {
        return true;
    }

    public JPanel getSettingsPanel() {
        if (settings == null)
            initSettingsPanel();
        lyxPipe.setText(Globals.prefs.get("lyxpipe"));
        return settings;
    }

    public void storeSettings() {
        Globals.prefs.put("lyxpipe", lyxPipe.getText());
    }

    private void initSettingsPanel() {
        settings = new JPanel();
        settings.add(new JLabel(Globals.lang("Path to LyX pipe") + ":"));
        settings.add(lyxPipe);
    }
    /*class Timeout extends javax.swing.Timer
    {
      public Timeout(int timeout, final Thread toStop, final String message) {
        super(timeout, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            toStop.stop();         // !!! <- deprecated
            // toStop.interrupt(); // better ?, interrupts wait and IO
            //stop();
            //output(message);
          }
        });
      }
    } */

}
