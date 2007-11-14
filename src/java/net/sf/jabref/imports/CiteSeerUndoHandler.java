/*
 * Created on Jun 29, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sf.jabref.imports;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author mspiegel
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CiteSeerUndoHandler extends DefaultHandler {

    NamedCompound citeseerNamedCompound = null;

    BibtexEntry bibEntry = null;

    String nextField = null;

    boolean nextAssign = false;

    BasePanel panel = null;

    BooleanAssign overwriteAll;
    BooleanAssign overwriteNone;
    BooleanAssign recordFound;
    
    String newAuthors = null;

    int citeseerCitationCount = 0;
    
    /*
     * Woe unto those who call this function from anywhere but
     * makeOverwriteChoice(). You will seriously f*&k things up.
     */
    private boolean overwriteDialog(String oldValue, String newValue,
            String fieldName) {
        boolean retval = false;
        Object[] possibilities = { "Yes", "Yes to All", "No", "No to All" };

        final JOptionPane optionPane = new JOptionPane(
                "Do you want to overwrite the value '" + oldValue
                        + "' \nwith the value '" + newValue + "' \nfor the "
                        + fieldName + " field?", JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, possibilities, "No");

        final JDialog dialog = new JDialog(panel.frame(), "Overwrite Value",
                true);
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String prop = e.getPropertyName();

                if (dialog.isVisible() && (e.getSource() == optionPane)
                        && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                    //If you were going to check something
                    //before closing the window, you'd do
                    //it here.
                    dialog.setVisible(false);
                }
            }
        });
        dialog.pack();
        dialog.setVisible(true);

        String userChoice = (String) optionPane.getValue();
        if (userChoice.equals("Yes to All")) {
            overwriteAll.setValue(true);
            retval = true;
        } else if (userChoice.equals("Yes")) {
            retval = true;
        } else if (userChoice.equals("No to All")) {
            overwriteNone.setValue(true);
        }
        return (retval);
    }

    public CiteSeerUndoHandler(NamedCompound newCompound, BibtexEntry be,
            BasePanel basePanel, BooleanAssign assignment, BooleanAssign overwriteAll, BooleanAssign overwriteNone) {
        citeseerNamedCompound = newCompound;
        bibEntry = be;
        panel = basePanel;
        recordFound = assignment;
        recordFound.setValue(false);
        this.overwriteAll = overwriteAll;
        this.overwriteNone = overwriteNone;    	
    }

    public CiteSeerUndoHandler(NamedCompound newCompound, BibtexEntry be,
            BasePanel basePanel, BooleanAssign assignment) {
    	this(newCompound, be, basePanel, assignment, new BooleanAssign(false), new BooleanAssign(false));
    }
       
	public void characters(char[] ch, int start, int length) {
        if (nextAssign == true) {
            String target = new String(ch, start, length);        
            if (nextField.equals("title")) {
                if (makeOverwriteChoice(bibEntry.getField(nextField),
                        target, nextField)) {
                    UndoableFieldChange fieldChange = new UndoableFieldChange(
                            bibEntry, nextField, bibEntry.getField(nextField),
                            target);
                    citeseerNamedCompound.addEdit(fieldChange);
                    bibEntry.setField(nextField, target);
                }
            } else if (nextField.equals("year")) {
                if (makeOverwriteChoice(bibEntry.getField(nextField),
                        String.valueOf(target.substring(0, 4)), nextField)) {
                    UndoableFieldChange fieldChange = new UndoableFieldChange(
                            bibEntry, nextField, bibEntry.getField(nextField),
                            String.valueOf(target.substring(0, 4)));
                    citeseerNamedCompound.addEdit(fieldChange);
                    bibEntry.setField(nextField, String.valueOf(target
                            .substring(0, 4)));
                }
            } else if (nextField.equals("citeseerurl")) {
                if (makeOverwriteChoice(bibEntry.getField(nextField),
                        target, nextField)) {
                    UndoableFieldChange fieldChange = new UndoableFieldChange(
                            bibEntry, nextField, bibEntry.getField(nextField),
                            target);
                    citeseerNamedCompound.addEdit(fieldChange);
                    bibEntry.setField(nextField, target);
                }
            }
            nextAssign = false;
        }
    }

    /**
     * @param oldValue
     * @param newValue
     * @param fieldName
     * @return overwrite
     */
    private boolean makeOverwriteChoice(String oldValue, String newValue,
            String fieldName) {
        boolean overwrite;
        if ((oldValue == null) || (oldValue.equals("")))
            overwrite = true;
        else if (oldValue.equals(newValue))
            overwrite = false;
        else if (overwriteAll.getValue() == true)
            overwrite = true;
        else if (overwriteNone.getValue() == true)
            overwrite = false;
        else
            overwrite = overwriteDialog(oldValue, newValue, fieldName);
        if (overwrite)
            recordFound.setValue(true);
        return overwrite;
    }

    public void startElement(String name, String localName, String qName, Attributes attrs)
            throws SAXException {
        if (qName.equals("oai_citeseer:relation")) {
    			for (int i = 0; i < attrs.getLength(); i++) {
    			   String attrName = attrs.getQName(i);
    			   String attrValue = attrs.getValue(i);	   
    			   if (attrName.equals("type") && attrValue.equals("Is Referenced By")) {  	
    			   	citeseerCitationCount++;
    			   }
    			}
        } else if (qName.equals("oai_citeseer:author")) {        	
            addAuthor(attrs.getValue("name"));
        } else if (qName.equals("dc:title")) {
            nextField = "title";
            nextAssign = true;
        } else if (qName.equals("dc:date")) {
            nextField = "year";
            nextAssign = true;
        } else if (qName.equals("dc:identifier")) {
            nextField = "citeseerurl";
            nextAssign = true;
        }
    }

    public void endDocument() {
        if (newAuthors != null) {
            if (makeOverwriteChoice(bibEntry.getField("author"),
                    newAuthors, "author")) {
                UndoableFieldChange fieldChange = new UndoableFieldChange(
                        bibEntry, "author", bibEntry.getField("author"), newAuthors);
                citeseerNamedCompound.addEdit(fieldChange);
                bibEntry.setField("author", newAuthors);
            }
        }
        String newCount = new Integer(citeseerCitationCount).toString();
        UndoableFieldChange fieldChange = new UndoableFieldChange(
                bibEntry, "citeseercitationcount", 
				bibEntry.getField("citeseercitationcount"), 
				newCount);
        citeseerNamedCompound.addEdit(fieldChange);
        bibEntry.setField("citeseercitationcount", newCount);
    }

    /**
     * @param string
     */
    private void addAuthor(String newAuthor) {
        if (newAuthors == null) {
            newAuthors = newAuthor;
        } else {
            newAuthors = newAuthors + " and " + newAuthor;
        }
    }

}