package net.sf.jabref.imports;

import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
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

    public CiteSeerFetcherPanel(SidePaneManager p0, final CiteSeerFetcher fetcher) {
	super(p0, GUIGlobals.wwwCiteSeerIcon, Globals.lang("Fetch CiteSeer"));

	help = new HelpAction(Globals.helpDiag, GUIGlobals.medlineHelp, "Help");

	this.citeSeerFetcher = fetcher;
	helpBut.addActionListener(help);
	helpBut.setMargin(new Insets(0,0,0,0));        
	//tf.setMinimumSize(new Dimension(1,1));
	//add(hd, BorderLayout.NORTH);
	//ok.setToolTipText(Globals.lang("Fetch Medline"));
        JPanel main = new JPanel();
	main.setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	con.insets = new Insets(0, 0, 2,  0);
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 1;
	con.weighty = 0;
	//gbl.setConstraints(header, con);
	//add(header);
	con.weighty = 1;
	con.insets = new Insets(0, 0, 0,  0);
	//    pan.setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(tf, con);
	main.add(tf);
	con.weighty = 0;
	con.gridwidth = 1;
	gbl.setConstraints(go, con);
	main.add(go);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(helpBut, con);
	main.add(helpBut);
        main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
	add(main, BorderLayout.CENTER);
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
			Set addedEntries;
			
			UpdateComponent(Set addedEntries) {
                this.addedEntries = addedEntries;
			}
			
			public void run() {
			    citeSeerFetcher.endImportCiteSeerProgress();
			    if (addedEntries != null)
				    panel.markBaseChanged();
			        panel.refreshTable();
                    // Select the entries that were added:
                    if (addedEntries.size() > 0) {
                         BibtexEntry[] toSelect = new BibtexEntry[addedEntries.size()];
                         toSelect = (BibtexEntry[])addedEntries.toArray(toSelect);

                        panel.selectEntries(toSelect, 0);
                        if (toSelect.length == 1)
                            panel.showEntry(toSelect[0]);
                        //else
                        //    panel.updateViewToSelected();
                     }
    		        panel.output(Globals.lang("Completed Import Fields from CiteSeer."));
			}
		    };
		    
		    public void run() {
			citeSeerFetcher.beginImportCiteSeerProgress();
			NamedCompound undoEdit =
			    new NamedCompound("CiteSeer import entries"),
			    // Use a dummy UndoEdit to avoid storing the information on
			    // every field change, since we are importing new entries:
			    dummyCompound = new NamedCompound("Ok");
			BooleanAssign overwriteAll = new BooleanAssign(true),
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

            Set addedEntries = new HashSet();
			if (rejectedEntries.size() < entries.length) {

			    for (int i=0; i<entries.length; i++) {
				if (rejectedEntries.contains(entries[i]))
				    continue;

				try {
				    panel.database().insertEntry(entries[i]);
				    //System.out.println(entries[i]);
				    UndoableInsertEntry undoItem = new UndoableInsertEntry
					(panel.database(), entries[i], panel);
				    undoEdit.addEdit(undoItem);
                    addedEntries.add(entries[i]);
				} catch (KeyCollisionException ex) {
				    ex.printStackTrace();
				}

			    }
			    undoEdit.end();
			    panel.undoManager.addEdit(undoEdit);


			}
			UpdateComponent updateComponent = new UpdateComponent
			    (addedEntries.size() > 0 ? addedEntries : null);
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

