package net.sf.jabref.imports;

import javax.swing.*;
import java.beans.*;

public class AuthorDialog extends JDialog implements PropertyChangeListener{

    boolean[] checked;
    JCheckBox[] checkBox;
    JOptionPane optionPane;
    int count, piv = 0;


    public AuthorDialog(JFrame frame, MedlineFetcher mf, String args[]) {
	super(frame, "Pick titles",true);
	count = args.length;
	checked=new boolean[count];
	checkBox = new JCheckBox[count];
	for (int i=0; i<count; i++){
	    checkBox[i]=new JCheckBox(args[i]);
	    checked[i]=false;
	}
	String message="A maximum of ten hits will be shown. If the desired article is not found, refine your search";
	Object[] msgs = {message, checkBox};
	optionPane =
	    new JOptionPane(msgs,
			    JOptionPane.WARNING_MESSAGE,
			    JOptionPane.DEFAULT_OPTION);
	setContentPane(optionPane);

	optionPane.addPropertyChangeListener(this);
    }

    public boolean[] showDialog(){
	pack();
	setVisible(true);
	return checked;
    }

    public void propertyChange(java.beans.PropertyChangeEvent e) {
	String prop = e.getPropertyName();

	if (isVisible()
	    && (e.getSource() == optionPane)
	    && (prop.equals(JOptionPane.VALUE_PROPERTY) ||
		prop.equals(JOptionPane.INPUT_VALUE_PROPERTY))) {

	    // set the boolean array here
	    for (int i=0; i<count; i++)
		if (checkBox[i].isSelected())
		    checked[i]=true;


	    setVisible(false);
	}
    }
}
