package net.sf.jabref.imports;

import java.util.ArrayList;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.Hashtable;
//import java.util.regex.Pattern;
//import java.util.regex.Matcher;
import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import java.io.*;
import net.sf.jabref.HelpAction;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class CiteSeerFetcherPanel extends SidePaneComponent implements ActionListener {

    SidePaneHeader header = 
	new SidePaneHeader("Fetch CiteSeer", GUIGlobals.wwwCiteSeerIcon, this);
    BasePanel panel;
    String idList;
    JTextField tf = new JTextField();
    JPanel pan = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    CiteSeerFetcher citeSeerFetcher;
    AuthorDialog authorDialog;
    JFrame jFrame; // invisible dialog holder
    JButton go = new JButton(Globals.lang("Fetch")),
	helpBut = new JButton(new ImageIcon(GUIGlobals.helpIconFile));
    HelpAction help;
    CiteSeerFetcherPanel ths = this;

    public CiteSeerFetcherPanel(BasePanel panel_, SidePaneManager p0, final CiteSeerFetcher fetcher) {
	super(p0);
	panel = panel_;
	help = new HelpAction(panel.frame().helpDiag, GUIGlobals.medlineHelp, "Help");
	this.citeSeerFetcher = fetcher;
	helpBut.addActionListener(help);
	helpBut.setMargin(new Insets(0,0,0,0));        
	//tf.setMinimumSize(new Dimension(1,1));
	//add(hd, BorderLayout.NORTH);
	//ok.setToolTipText(Globals.lang("Fetch Medline"));
	setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	con.insets = new Insets(0, 0, 2,  0);
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 1;
	con.weighty = 0;
	gbl.setConstraints(header, con);
	add(header);
	con.weighty = 1;
	con.insets = new Insets(0, 0, 0,  0);
	//    pan.setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(tf, con);
	add(tf);
	con.weighty = 0;
	con.gridwidth = 1;
	gbl.setConstraints(go, con);
	add(go);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(helpBut, con);
	add(helpBut);
	
	go.addActionListener(this);
	tf.addActionListener(this);
    }
    
    public JTextField getTextField() {
        return tf;
    }
    
    public void actionPerformed(ActionEvent e) {
	if(citeSeerFetcher.activateImportFetcher()) {
	    (new Thread() {
		    
		    BibtexEntry entry;
		    
		    class UpdateComponent implements Runnable {
			boolean changesMade;
			
			UpdateComponent(boolean changesMade) {
			    this.changesMade = changesMade;
			}
			
			public void run() {
			    citeSeerFetcher.endImportCiteSeerProgress();
			    if (changesMade)
				panel.markBaseChanged();
			    panel.refreshTable();
			    //for(int i=0; i < clickedOn.length; i++)
			    //        currentBp.entryTable.addRowSelectionInterval(i,i);
			    //currentBp.showEntry(toShow);
			    panel.output(Globals.lang("Completed Import Fields from CiteSeer."));
			}
		    };
		    
		    public void run() {
			citeSeerFetcher.beginImportCiteSeerProgress();
			NamedCompound undoEdit =
			    new NamedCompound("CiteSeer import entries"),
			    // Use a dummy UndoEdit to avoid storing the information on
			    // every field change, since we are importing new entries:
			    dummyCompound = new NamedCompound("dummy");
			BooleanAssign overwriteAll = new BooleanAssign(false),
			    overwriteNone = new BooleanAssign(false),
			    newValue = new BooleanAssign(false);			
			Hashtable rejectedEntries = new Hashtable();
			String text = tf.getText().replaceAll(",", ";");
			String[] ids = text.split(";");
			BibtexEntry[] entries = new BibtexEntry[ids.length];
			for (int i=0; i<entries.length; i++) {
			    // Create the entry:
			    entries[i] = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.getType("article"));
			    // Set its citeseerurl field:
			    entries[i].setField("citeseerurl", ids[i].trim());
			    // Try to import based on the id:
			    boolean newValues = citeSeerFetcher.importCiteSeerEntry
				(entries[i], dummyCompound, overwriteAll, overwriteNone,
				 newValue, rejectedEntries);

			}

			if (rejectedEntries.size() < entries.length) {
			    for (int i=0; i<entries.length; i++) {
				if (rejectedEntries.contains(entries[i]))
				    continue;

				try {
				    panel.database().insertEntry(entries[i]);
				    System.out.println(entries[i]);
				    UndoableInsertEntry undoItem = new UndoableInsertEntry
					(panel.database(), entries[i], panel);
				    undoEdit.addEdit(undoItem);
				} catch (KeyCollisionException ex) {
				    ex.printStackTrace();
				}
				
			    }
			    undoEdit.end();
			    panel.undoManager.addEdit(undoEdit);
			}   
			UpdateComponent updateComponent = new UpdateComponent
			    (rejectedEntries.size() < entries.length);
			SwingUtilities.invokeLater(updateComponent);
		    
			citeSeerFetcher.deactivateImportFetcher();
		    }
		}).start();
	} else {
	    JOptionPane.showMessageDialog(panel.frame(),
					  Globals.lang("A CiteSeer import operation is currently in progress.") + "  " +
					  Globals.lang("Please wait until it has finished."),
					  Globals.lang("CiteSeer Import Error"),
					  JOptionPane.WARNING_MESSAGE);
	}
    }
}

