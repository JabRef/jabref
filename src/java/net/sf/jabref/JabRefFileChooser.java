package net.sf.jabref;
import java.io.*;
import javax.swing.*;
import javax.swing.plaf.*;
import java.awt.event.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;

//======================================================================
// this class is a work around for the problem with regular filechooser:
// single clicking will no longer put into edit mode
//======================================================================
public class JabRefFileChooser extends JFileChooser {
    public JabRefFileChooser(){
	super();
    }

    public JabRefFileChooser(File file){
	super(file);
    }

    //========================================================
    //
    //========================================================
    
    protected void setUI(ComponentUI newUI) {
	 super.setUI(new JabRefUI(this));
     }
    //========================================================
    //
    //========================================================

    public static void main(String[] args) {
	JabRefFileChooser fc = new JabRefFileChooser();
	int returnVal = fc.showOpenDialog(null);
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    File file = fc.getSelectedFile();
	}
    }
}


class JabRefUI extends MetalFileChooserUI {
    public JabRefUI(JFileChooser filechooser) {
	super(filechooser);
    }
    protected class DoubleClickListener extends BasicFileChooserUI.DoubleClickListener {
	JList list;
	public DoubleClickListener(JList list) {
	    super(list);
	    this.list = list;
	}
	public void mouseEntered(MouseEvent e) {
	    //System.out.println("mouse entered");
	    MouseListener [] l = list.getMouseListeners();
	    for (int i = 0; i < l.length; i++) {
		if (l[i] instanceof MetalFileChooserUI.SingleClickListener) {
		    list.removeMouseListener(l[i]);
		}
	    }
	    super.mouseEntered(e);
	}
    }
    protected MouseListener createDoubleClickListener(JFileChooser fc, JList list) {
	return new DoubleClickListener(list);
    }
} 
