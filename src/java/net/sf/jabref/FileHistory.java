package net.sf.jabref;

import java.awt.event.*;
import javax.swing.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.File;

class FileHistory extends JMenu implements ActionListener {

    int bound = 5; //or user defined
    JabRefPreferences prefs;
    LinkedList history = new LinkedList();
    JabRefFrame frame;

    public FileHistory(JabRefPreferences prefs, JabRefFrame frame) {
	super("Recent files");
	this.prefs = prefs;
	this.frame = frame;
	String[] old = prefs.getStringArray("recentFiles");
	if ((old != null) && (old.length > 0)) {
	    for (int i=0; i<old.length; i++) {
		JMenuItem item = new JMenuItem(old[i]);
		item.addActionListener(this);
		add(item);
		history.addFirst(item);
	    }
	}
    }

    public void newFile(String filename) {
	JMenuItem item = new JMenuItem(filename);
	item.addActionListener(this);
	history.addFirst(item);
	while (history.size() > prefs.getInt("historySize")) {
	    history.removeLast();
	}
	setItems();
    }

    private void setItems() {
	removeAll();
	Iterator i= history.iterator();
	while (i.hasNext()) {
	    add((JMenuItem)i.next());
	}
    }
    
    public void storeHistory() {
	String[] names = new String[history.size()];
	for (int i=0; i<names.length; i++)
	    names[i] = ((JMenuItem)history.get(i)).getText();

	prefs.putStringArray("recentFiles", names);
    }

    public void setFileHistory() {
	
    }
    
    public void actionPerformed(ActionEvent e) {
	String name = ((JMenuItem)e.getSource()).getText();
	frame.fileToOpen = new File(name);
	(new Thread() {
		public void run() {
		    frame.openDatabaseAction.openIt();
		}
	    }).start();

    }


}
