package net.sf.jabref.external;

import net.sf.jabref.*;

import javax.swing.*;
import java.io.*;
import java.awt.event.*;

public class PushToLyx implements PushToApplication {

    private boolean couldNotFindPipe=false;
    private boolean couldNotWrite=false;
    private String message = "";

    public void pushEntries(final BibtexEntry[] entries, final String keyString) {

        couldNotFindPipe = false;
        couldNotWrite = false;


        final File lyxpipe = new File( Globals.prefs.get("lyxpipe") +".in"); // this needs to fixed because it gives "asdf" when going prefs.get("lyxpipe")
        if( !lyxpipe.exists() || !lyxpipe.canWrite()){
            couldNotFindPipe = true;
            return;
        }

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
