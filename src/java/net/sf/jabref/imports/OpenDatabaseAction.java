package net.sf.jabref.imports;

import net.sf.jabref.*;
import javax.swing.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

// The action concerned with opening an existing database.
public  class OpenDatabaseAction extends MnemonicAwareAction {
    boolean showDialog;

    private JabRefFrame frame;

    public OpenDatabaseAction(JabRefFrame frame, boolean showDialog) {
	super(new ImageIcon(GUIGlobals.openIconFile));
	this.frame = frame;
	this.showDialog = showDialog;
	putValue(NAME, "Open database");
	putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Open database"));
	putValue(SHORT_DESCRIPTION, Globals.lang("Open BibTeX database"));
    }
    
    public void actionPerformed(ActionEvent e) {
	File fileToOpen = null;

	if (showDialog) {
	    
	    String chosenFile = Globals.getNewFile(frame, Globals.prefs, new File(Globals.prefs.get("workingDirectory")), ".bib",
						   JFileChooser.OPEN_DIALOG, true);
	    
	    if (chosenFile != null) {
		fileToOpen = new File(chosenFile);
	    }
	}
	else {
	    Util.pr(NAME);
	    Util.pr(e.getActionCommand());
	    fileToOpen = new File(Util.checkName(e.getActionCommand()));
	}

	// Run the actual open in a thread to prevent the program
	// locking until the file is loaded.
	if (fileToOpen != null) {
	    final File theFile = fileToOpen;
	    (new Thread() {
		    public void run() {
			openIt(theFile, true);
		    }
		}).start();
	    frame.getFileHistory().newFile(fileToOpen.getPath());
	}
    }

    class OpenItSwingHelper implements Runnable{
        BasePanel bp;
        boolean raisePanel;
	File file;

        OpenItSwingHelper(BasePanel bp, File file, boolean raisePanel) {
            this.bp = bp;
            this.raisePanel = raisePanel;
	    this.file = file;
        }

        public void run() {
            frame.addTab(bp, file, raisePanel);
        }
    }

    public void openIt(File file, boolean raisePanel) {
      if ( (file != null) && (file.exists())) {
          frame.output(Globals.lang("Opening") + ": '" + file.getPath()+"'");
        try {
          String fileName = file.getPath();
          Globals.prefs.put("workingDirectory", file.getPath());
          // Should this be done _after_ we know it was successfully opened?
          String encoding = Globals.prefs.get("defaultEncoding");
          ParserResult pr = ImportFormatReader.loadDatabase(file, encoding);
          BibtexDatabase db = pr.getDatabase();
          HashMap meta = pr.getMetaData();

          if (pr.hasWarnings()) {
            final String[] wrns = pr.warnings();
            (new Thread() {
              public void run() {
                StringBuffer wrn = new StringBuffer();
                for (int i = 0; i < wrns.length; i++)
                  wrn.append( (i + 1) + ". " + wrns[i] + "\n");
                
                if (wrn.length() > 0)
                  wrn.deleteCharAt(wrn.length() - 1);
                // Note to self or to someone else: The following line causes an
                // ArrayIndexOutOfBoundsException in situations with a large number of
                // warnings; approx. 5000 for the database I opened when I observed the problem
                // (duplicate key warnings). I don't think this is a big problem for normal situations,
                // and it may possibly be a bug in the Swing code.
                JOptionPane.showMessageDialog(frame, wrn.toString(),
                                              Globals.lang("Warnings"),
                                              JOptionPane.WARNING_MESSAGE);
              }
            }).start();
          }

          BasePanel bp = new BasePanel(frame, db, file,
                                       meta, Globals.prefs);
          bp.setEncoding(pr.getEncoding()); // Keep track of which encoding was used for loading.
          /*
            if (Globals.prefs.getBoolean("autoComplete")) {
            db.setCompleters(autoCompleters);
            }
           */

          // file is set to null inside the EventDispatcherThread
          SwingUtilities.invokeLater(new OpenItSwingHelper(bp, file, raisePanel));

	  // See if any custom entry types were imported, but disregard those we already know:
          for (Iterator i=pr.getEntryTypes().keySet().iterator(); i.hasNext();) {
              String typeName = ((String)i.next()).toLowerCase();
              if (BibtexEntryType.ALL_TYPES.get(typeName) != null)
                  i.remove();
          }
	  if (pr.getEntryTypes().size() > 0) {
              
              
              
	      StringBuffer sb = new StringBuffer(Globals.lang("Custom entry types found in file")+": ");
	      Object[] types = pr.getEntryTypes().keySet().toArray();
	      Arrays.sort(types);
	      for (int i=0; i<types.length; i++) {
		  sb.append(types[i].toString()+", ");
	      }
	      String s = sb.toString();
	      int answer = JOptionPane.showConfirmDialog(frame, 
					    s.substring(0, s.length()-2)+".\n"
					    +Globals.lang("Remember these entry types?"),
					    Globals.lang("Custom entry types"),
                                            JOptionPane.YES_NO_OPTION,
					    JOptionPane.QUESTION_MESSAGE);
              if (answer == JOptionPane.YES_OPTION) {
                  // Import
                  HashMap et = pr.getEntryTypes();
              	  for (Iterator i=et.keySet().iterator(); i.hasNext();) {
                      BibtexEntryType typ = (BibtexEntryType)et.get(i.next());
                      //System.out.println(":"+typ.getName()+"\n"+typ.toString());
                      BibtexEntryType.ALL_TYPES.put(typ.getName().toLowerCase(), typ);
                  }    
                  
              }
	  }

          frame.output(Globals.lang("Opened database") + " '" + fileName +
                 "' " + Globals.lang("with") + " " +
                 db.getEntryCount() + " " + Globals.lang("entries") + ".");

        }
        catch (Throwable ex) {
          //  ex.printStackTrace();
          JOptionPane.showMessageDialog
              (frame, ex.getMessage(),
               Globals.lang("Open database"), JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }
