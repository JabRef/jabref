package net.sf.jabref.external;

import net.sf.jabref.*;
import java.io.*;
import java.awt.event.*;

public class PushToLyx extends BaseAction {

    private BasePanel panel;

    public PushToLyx(BasePanel panel) {
	this.panel = panel;
    }

    public void action() {
	final BibtexEntry[] entries = panel.getSelectedEntries();
	if (entries == null)
	    return;
	final int numSelected = entries.length;
	// Globals.logger("Pushing " +numSelected+(numSelected>1? " entries" : "entry") + " to LyX");
	// check if lyxpipe is defined
	final File lyxpipe = new File( Globals.prefs.get("lyxpipe") +".in"); // this needs to fixed because it gives "asdf" when going prefs.get("lyxpipe")
	if( !lyxpipe.exists() || !lyxpipe.canWrite()){
	    panel.output(Globals.lang("Error")+": "+Globals.lang("verify that LyX is running and that the lyxpipe is valid")
		   +". [" + Globals.prefs.get("lyxpipe") +"]");
	    return;
	}
	//Util.pr("tre");
	if( numSelected > 0){
	    Thread pushThread = new Thread()
		{
		    public void run()
		    {
			try {
                            FileWriter fw = new FileWriter(lyxpipe);
                            BufferedWriter lyx_out = new BufferedWriter(fw);
                            String citeStr = "", citeKey = "", message = "";
                            for (int i = 0; i < numSelected; i++)
				{
				    BibtexEntry bes = entries[i];//database.getEntryById(tableModel.getNameFromNumber(rows[
				    //													      i]));
				    citeKey = (String) bes.getField(GUIGlobals.KEY_FIELD);
				    // if the key is empty we give a warning and ignore this entry
				    if (citeKey == null || citeKey.equals(""))
					continue;
				    if (citeStr.equals(""))
					citeStr = citeKey;
				    else
					citeStr += "," + citeKey;
				    if (i > 0)
					message += ", ";
				    //message += (1 + rows[i]);
				}
                            if (citeStr.equals(""))
				panel.output(Globals.lang("Please define BibTeX key first"));
                            else
				{
				    citeStr = "LYXCMD:sampleclient:citation-insert:" + citeStr;
				    lyx_out.write(citeStr + "\n");
				    panel.output(Globals.lang("Pushed the citations for the following rows to")+" Lyx: " +
					   message);
				}
                            lyx_out.close();
			    
			}
			catch (IOException excep) {
                            panel.output(Globals.lang("Error")+": "+Globals.lang("unable to write to")+" " + Globals.prefs.get("lyxpipe") +
                                   ".in");
			}
			// catch (InterruptedException e2) {}
		    }
		};
	    pushThread.start();
	    Timeout t = new Timeout(2000, pushThread, Globals.lang("Error")+": "+
				    Globals.lang("unable to access LyX-pipe"));
	    t.start();
	}
    }

  class Timeout extends javax.swing.Timer
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
  }

}
