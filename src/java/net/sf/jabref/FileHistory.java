package net.sf.jabref;

import java.awt.event.*;
import javax.swing.*;
import java.util.LinkedList;
import java.util.Iterator;

class FileHistory extends JMenu implements ActionListener {

    int bound = 5; //or user defined
    JabRefPreferences prefs;
    LinkedList history = new LinkedList();

    public FileHistory(JabRefPreferences prefs) {
	super("Recent files");
	this.prefs = prefs;
	String[] old = prefs.getStringArray("recentFiles");
	Util.pr(old[0]);
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

    /*
    public void storeRecent() {
      
    }*/

    public void setFileHistory() {
	
    }
    
    public void actionPerformed(ActionEvent e) {

    }

}
