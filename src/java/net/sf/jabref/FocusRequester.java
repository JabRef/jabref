package net.sf.jabref;

import javax.swing.SwingUtilities;
import java.awt.Component;

public class FocusRequester implements Runnable {
    private Component comp;

    public FocusRequester(Component comp) {
       if (comp == null)
               Thread.dumpStack();

        //System.out.println("FocusRequester: "+comp.toString());
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
