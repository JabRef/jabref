/*  Copyright (C) 2003-2012 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class ContentSelectorDialog2 extends JDialog {

    ActionListener wordEditFieldListener = null;
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JPanel fieldPan = new JPanel(),
	wordPan = new JPanel(),
	buttonPan = new JPanel(),
	fieldNamePan = new JPanel(),
	wordEditPan = new JPanel();

    final String
	WORD_EMPTY_TEXT = Globals.lang("<no field>"),
	WORD_FIRSTLINE_TEXT = Globals.lang("<select word>"),
	FIELD_FIRST_LINE = Globals.lang("<field name>");
    MetaData metaData;
    String currentField = null;
    TreeSet<String> fieldSet, wordSet;
    JabRefFrame frame;
    BasePanel panel;
    JButton help = new JButton(Globals.lang("Help")),
	newField = new JButton(Globals.lang("New")),
	removeField = new JButton(Globals.lang("Remove")),
	newWord = new JButton(Globals.lang("New")),
	removeWord = new JButton(Globals.lang("Remove")),
	ok = new JButton(Globals.lang("Ok")),
	cancel = new JButton(),
	apply = new JButton(Globals.lang("Apply"));
    DefaultListModel fieldListModel = new DefaultListModel(),
	wordListModel = new DefaultListModel();
    JList fieldList = new JList(fieldListModel),
	wordList = new JList(wordListModel);
    JTextField fieldNameField = new JTextField("", 20),
	wordEditField = new JTextField("", 20);
    JScrollPane fPane = new JScrollPane(fieldList),
	wPane = new JScrollPane(wordList);

    HashMap<String, DefaultListModel> wordListModels = new HashMap<String, DefaultListModel>();
    ArrayList<String> removedFields = new ArrayList<String>();

    /**
     * 
     * @param owner the parent Window (Dialog or Frame)
     * @param frame the JabRef Frame
     * @param panel the currently selected BasePanel
     * @param modal should this dialog be modal?
     * @param metaData The metadata of the current database
     * @param fieldName the field this selector is initialized for. May be null.
     */
    public ContentSelectorDialog2(Window owner, JabRefFrame frame, BasePanel panel, boolean modal, MetaData metaData,
              String fieldName) {
        super(owner, Globals.lang("Setup selectors"));
        this.setModal(modal);
        this.metaData = metaData;
        this.frame = frame;
        this.panel = panel;
        this.currentField = fieldName;

        //help = new JButton(Globals.lang("Help"));
        //help.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.contentSelectorHelp, "Help"));
        //help = new HelpAction(frame.helpDiag, GUIGlobals.contentSelectorHelp, "Help");
        initLayout();
        //  wordSelector.addItem(WORD_EMPTY_TEXT);
    
        setupFieldSelector();
        setupWordSelector();
        setupActions();
        Util.bindCloseDialogKeyToCancelAction(this.rootPane, cancel.getAction());
        int fieldInd = fieldListModel.indexOf(currentField);
        if (fieldInd >= 0)
            fieldList.setSelectedIndex(fieldInd);
    
        pack();
    }


    private void setupActions() {

	wordList.addListSelectionListener(new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
		    wordEditField.setText((String)wordList.getSelectedValue());
		    wordEditField.selectAll();
		    new FocusRequester(wordEditField);
		}
	    });

	newWord.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    newWordAction();
		}
	    });
                
        wordEditFieldListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = wordList.getSelectedIndex();
                String old = (String)wordList.getSelectedValue(),
            	newVal = wordEditField.getText();
                if (newVal.equals("") || newVal.equals(old)) {
                    return; // Empty string or no change.
                }
                if (wordListModel.contains(newVal)) {
                	// ensure that word already in list is visible
                	index = wordListModel.indexOf(newVal);
                	wordList.ensureIndexIsVisible(index);
                	return;
                }

                int newIndex = findPos(wordListModel, newVal);
                if (index >= 0) {
                	// initiate replacement of selected word
                    wordListModel.remove(index);
                    if (newIndex > index) {
                    	// newIndex has to be adjusted after removal of previous entry
                    	newIndex--;
                    }
                }
                wordListModel.add(newIndex, newVal);
                wordList.ensureIndexIsVisible(newIndex);
                wordEditField.selectAll();
            }
	};
        wordEditField.addActionListener(wordEditFieldListener);
        
	removeWord.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int index = wordList.getSelectedIndex();
		    if (index == -1)
			return;
		    wordListModel.remove(index);
		    wordEditField.setText("");
		    if (wordListModel.size() > 0)
			wordList.setSelectedIndex(Math.min(index, wordListModel.size()-1));
		}
	    });


	fieldList.addListSelectionListener(new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
		    currentField = (String)fieldList.getSelectedValue();
		    fieldNameField.setText("");
		    setupWordSelector();
		}
	    });

	newField.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (!fieldListModel.get(0).equals(FIELD_FIRST_LINE)) {
				// only add <field name> once
				fieldListModel.add(0, FIELD_FIRST_LINE);
			}
		    fieldList.setSelectedIndex(0);
		    fPane.getVerticalScrollBar().setValue(0);
		    fieldNameField.setEnabled(true);
		    fieldNameField.setText(currentField);
		    fieldNameField.selectAll();

		    new FocusRequester(fieldNameField);
		}
	    });

	fieldNameField.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    fieldNameField.transferFocus();
		}
	    });

	fieldNameField.addFocusListener(new FocusAdapter() {
		
		/**
		 * Adds the text value to the list
		 */
		public void focusLost(FocusEvent e) {
		    String s = fieldNameField.getText();
		    fieldNameField.setText("");
		    fieldNameField.setEnabled(false);
		    if (!FIELD_FIRST_LINE.equals(s) && !"".equals(s)) {
		    	// user has typed something
		    	
			    // remove "<first name>" from list
				fieldListModel.remove(0);
				
				int pos;
				if (fieldListModel.contains(s)) {
					// field already exists, scroll to that field (below)
					pos = fieldListModel.indexOf(s);
				} else {
					// Add new field.
					pos = findPos(fieldListModel, s);
					fieldListModel.add(Math.max(0, pos), s);
				}
				fieldList.setSelectedIndex(pos);
				fieldList.ensureIndexIsVisible(pos);
				currentField = s;
				setupWordSelector();
				newWordAction();
				//new FocusRequester(wordEditField);
		    }
		}
	    });

	removeField.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    int index = fieldList.getSelectedIndex();
		    if (index == -1)
			return;
		    String fieldName = (String)fieldListModel.get(index);
		    removedFields.add(fieldName);
		    fieldListModel.remove(index);
		    wordListModels.remove(fieldName);
		    fieldNameField.setText("");
		    if (fieldListModel.size() > 0)
			fieldList.setSelectedIndex(Math.min(index, wordListModel.size()-1));
		}
	    });

	help.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    frame.helpDiag.showPage(GUIGlobals.contentSelectorHelp);
		}
	    });

	ok.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    applyChanges();
		    dispose();
		}
	    });

	apply.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
            // Store if an entry is currently being edited:
            if (!wordEditField.getText().equals("")) {
                wordEditFieldListener.actionPerformed(null);
            }
		    applyChanges();
		}
	    });
	
	@SuppressWarnings("serial")
    Action cancelAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		}
	};
	cancelAction.putValue(Action.NAME, Globals.lang("Cancel"));
	cancel.setAction(cancelAction);
    }

    private void newWordAction() {
	if ((wordListModel.size() == 0) || 
	    !wordListModel.get(0).equals(WORD_FIRSTLINE_TEXT))
	    wordListModel.add(0, WORD_FIRSTLINE_TEXT);
	wordList.setSelectedIndex(0);
	wPane.getVerticalScrollBar().setValue(0);
    }


    private void applyChanges() {
	boolean changedFieldSet = false; // Watch if we need to rebuild entry editors

	// First remove the mappings for fields that have been deleted.
	// If these were re-added, they will be added below, so it doesn't
	// cause any harm to remove them here.
	for (Iterator<String> i=removedFields.iterator(); i.hasNext();) {
	    String fieldName = i.next();
	    metaData.remove(Globals.SELECTOR_META_PREFIX+fieldName);
	    changedFieldSet = true;
	}

	// Cycle through all fields that we have created listmodels for:
	loop: for (Iterator<String> i=wordListModels.keySet().iterator(); i.hasNext();) {
	    // For each field name, store the values:
	    String fieldName = i.next();
	    if ((fieldName == null) || FIELD_FIRST_LINE.equals(fieldName))
		continue loop;
	    DefaultListModel lm = wordListModels.get(fieldName);
	    int start = 0;
	    // Avoid storing the <new word> marker if it is there:
	    if (lm.size() > 0)
		while ((start<lm.size()) && (lm.get(start)).equals(WORD_FIRSTLINE_TEXT))
		    start++;
	    Vector<String> data = metaData.getData(Globals.SELECTOR_META_PREFIX+fieldName);
	    boolean newField = false;
	    if (data == null) {
		newField = true;
		data = new Vector<String>();
		changedFieldSet = true;

	    } else
		data.clear();
	    for (int wrd=start; wrd<lm.size(); wrd++) {
		String word = (String)lm.get(wrd);
		data.add(word);
	    }
	    if (newField)
		metaData.putData(Globals.SELECTOR_META_PREFIX+fieldName, data);
	}

	// System.out.println("TODO: remove metadata for removed selector field.");
	panel.markNonUndoableBaseChanged();

	// Update all selectors in the current BasePanel.
	if (changedFieldSet) {
	    panel.rebuildAllEntryEditors();
    } else {
	    panel.updateAllContentSelectors();
	}
    panel.addContentSelectorValuesToAutoCompleters();
        
    }

    /**
     * Set the contents of the field selector list.
     *
     */
    private void setupFieldSelector() {
		fieldListModel.clear();
		SortedSet<String> contents = new TreeSet<String>();
		for (String s : metaData) {
		    if (s.startsWith(Globals.SELECTOR_META_PREFIX)) {
		    	contents.add(s.substring(Globals.SELECTOR_META_PREFIX.length()));
		    }
		}
		if (contents.size() == 0) {
			// if nothing was added, put the default fields (as described in the help)
			fieldListModel.addElement("author");
			fieldListModel.addElement("journal");
			fieldListModel.addElement("keywords");
			fieldListModel.addElement("publisher");
		} else {
			for (String s : contents) {
				fieldListModel.addElement(s);
			}
		}
		
		if (currentField == null) {
			// if dialog is created for the whole database,
			// select the first field to avoid confusions in GUI usage
			fieldList.setSelectedIndex(0); 
		} else {
			// a specific field has been chosen at the constructur
			// select this field
			int i = fieldListModel.indexOf(currentField);
			if (i != -1) {
				// field has been found in list, select it
				fieldList.setSelectedIndex(i);
			}
		}
    }


    private void setupWordSelector() {

		// Have we already created a listmodel for this field?
    	wordListModel = wordListModels.get(currentField);
		if (wordListModel != null) {
			wordList.setModel(wordListModel);
		} else {
			wordListModel = new DefaultListModel();
			wordList.setModel(wordListModel);
			wordListModels.put(currentField, wordListModel);
			// wordListModel.addElement(WORD_FIRSTLINE_TEXT);
			Vector<String> items = metaData.getData(Globals.SELECTOR_META_PREFIX + currentField);
			if (items != null) {
				wordSet = new TreeSet<String>(items);
				int index = 0;
				for (String s : wordSet){
					wordListModel.add(index, s);
					index++;
				}
			}
		}
	}

    private int findPos(DefaultListModel lm, String item) {
	for (int i=0; i<lm.size(); i++) {
	    String s = (String)lm.get(i);
	    if (item.compareToIgnoreCase(s) < 0) { // item precedes s
		return i;
	    }
	}
	return lm.size();
    }

    private void initLayout() {
	fieldNameField.setEnabled(false);
	fieldList.setVisibleRowCount(4);
	wordList.setVisibleRowCount(10);
	final String VAL = "Uren luren himmelturen, ja Besseggen.";
	fieldList.setPrototypeCellValue(VAL);
	wordList.setPrototypeCellValue(VAL);

	fieldPan.setBorder(BorderFactory.createTitledBorder
			       (BorderFactory.createEtchedBorder(),
				Globals.lang("Field name")));
	wordPan.setBorder(BorderFactory.createTitledBorder
			       (BorderFactory.createEtchedBorder(),
				Globals.lang("Keyword")));
	fieldPan.setLayout(gbl);
	wordPan.setLayout(gbl);
	con.insets = new Insets(2, 2, 2, 2);
	con.fill = GridBagConstraints.BOTH;
	con.gridwidth = 2;
	con.weightx = 1;
	con.weighty = 1;
	con.gridx = 0;
	con.gridy = 0;
	gbl.setConstraints(fPane, con);
	fieldPan.add(fPane);
	gbl.setConstraints(wPane, con);
	wordPan.add(wPane);
	con.gridwidth = 1;
	con.gridx = 2;
	//con.weightx = 0.7;
	con.gridheight = 2;
	gbl.setConstraints(fieldNamePan, con);
	fieldPan.add(fieldNamePan);
	gbl.setConstraints(wordEditPan, con);
	wordPan.add(wordEditPan);
	con.gridx = 0;
	con.gridy = 1;
	con.weightx = 0;
	con.weighty = 0;
	con.gridwidth = 1;
	con.gridheight = 1;
	con.fill = GridBagConstraints.NONE;
	con.anchor = GridBagConstraints.WEST;
    gbl.setConstraints(newField, con);
	fieldPan.add(newField);
	gbl.setConstraints(newWord, con);
	wordPan.add(newWord);
	con.gridx = 1;
	//con.anchor = GridBagConstraints.EAST;
	gbl.setConstraints(removeField, con);
	fieldPan.add(removeField);
	gbl.setConstraints(removeWord, con);
	wordPan.add(removeWord);
	con.anchor = GridBagConstraints.WEST;
	con.gridx = 0;
	con.gridy = 0;
	gbl.setConstraints(fieldNameField, con);
	fieldNamePan.add(fieldNameField);
	gbl.setConstraints(wordEditField, con);
	wordEditPan.add(wordEditField);

	// Add buttons:
        ButtonBarBuilder bsb = new ButtonBarBuilder(buttonPan);
        bsb.addGlue();
        bsb.addButton(ok);
	    bsb.addButton(apply);
        bsb.addButton(cancel);
        bsb.addRelatedGap();
        bsb.addButton(help);
        bsb.addGlue();

    // Add panels to dialog:
	con.fill = GridBagConstraints.BOTH;
	getContentPane().setLayout(gbl);
	con.weightx = 1;
	con.weighty = 0.5;
	con.gridwidth = 1;
	con.gridheight = 1;
	con.gridx = 0;
	con.gridy = 0;
	gbl.setConstraints(fieldPan, con);
	getContentPane().add(fieldPan);
	con.gridy = 1;
	gbl.setConstraints(wordPan, con);
	getContentPane().add(wordPan);
	con.weighty = 0;
	con.gridy = 2;
	con.insets = new Insets(12, 2, 2, 2);
	gbl.setConstraints(buttonPan, con);
	getContentPane().add(buttonPan);


    }


}
