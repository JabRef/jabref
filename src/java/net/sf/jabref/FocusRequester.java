package net.sf.jabref;

import javax.swing.SwingUtilities;
import java.awt.Component;

class FocusRequester implements Runnable {  
    private Component comp;
    
    public FocusRequester(Component comp) {
	this.comp = comp;
	try {
	    SwingUtilities.invokeLater(this);
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }   
    public void run() {
	comp.requestFocus();
    }
}
