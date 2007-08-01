package net.sf.jabref.imports;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.sf.jabref.Globals;

public class AuthorDialog extends JDialog {

    boolean[] checked;
    JCheckBox[] checkBox;
    JOptionPane optionPane;
    int count, piv = 0;


    public AuthorDialog(JFrame frame, MedlineFetcher mf, String ids[]) {
	super(frame, Globals.lang("Pick titles"),true);

	count = ids.length;
	checked=new boolean[count];
    }

    public boolean[] showDialog(){
	pack();
	setVisible(true);
	return checked;
    }

}
