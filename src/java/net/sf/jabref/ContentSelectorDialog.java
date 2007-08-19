package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ContentSelectorDialog extends JDialog {
    JPanel panel1 = new JPanel();
    JPanel jPanel2 = new JPanel();
    JButton Close = new JButton();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JPanel jPanel1 = new JPanel();
    JLabel lab = new JLabel();
    JTextField fieldTf = new JTextField();
    JButton add = new JButton();
    JPanel jPanel3 = new JPanel();
    JButton remove = new JButton();
    JComboBox fieldSelector = new JComboBox();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    //HelpAction help;
    JButton help;
    JPanel jPanel4 = new JPanel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    TitledBorder titledBorder1;
    TitledBorder titledBorder2;
    JLabel jLabel1 = new JLabel();
    JComboBox wordSelector = new JComboBox();
    JTextField wordTf = new JTextField();
    JPanel jPanel5 = new JPanel();
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    JButton addWord = new JButton();
    JPanel jPanel6 = new JPanel();
    JButton removeWord = new JButton();
    GridBagLayout gridBagLayout6 = new GridBagLayout();
    JButton select = new JButton();

    final String
	WORD_EMPTY_TEXT = Globals.lang("<no field>"),
	WORD_FIRSTLINE_TEXT = Globals.lang("<select word>"),
	FIELD_FIRST_LINE = Globals.lang("<field name>");
    MetaData metaData;
    String currentField = null;
    TreeSet<String> fieldSet, wordSet;
    JabRefFrame frame;

    public ContentSelectorDialog(JabRefFrame frame, boolean modal, MetaData metaData) {
	super(frame, Globals.lang("Setup selectors"), modal);
	this.metaData = metaData;
	this.frame = frame;
	help = new JButton(Globals.lang("Help"));
	help.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.contentSelectorHelp, "Help"));
	//help = new HelpAction(frame.helpDiag, GUIGlobals.contentSelectorHelp, "Help");
	try {
	    jbInit();
	    wordSelector.addItem(WORD_EMPTY_TEXT);
	    pack();
	}
	catch(Exception ex) {
	    ex.printStackTrace();
	}
    }

    public ContentSelectorDialog(JabRefFrame frame, boolean modal, MetaData metaData,
				 String fieldName) {
	this(frame, modal, metaData);

	try {
	    fieldSelector.setSelectedItem(fieldName);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	// The next two lines remove the part of the interface allowing
	// the user to control which fields have a selector. I think this
	// makes the dialog more intuitive. When the user opens this dialog
	// from the Tools menu, the full interface will be available.
	panel1.remove(jPanel1);
	pack();
    }

    /**
     * Set the contents of the field selector combobox.
     *
     */
    private void setupFieldSelector() {
	fieldSelector.removeAllItems();
	fieldSelector.addItem(FIELD_FIRST_LINE);
	for (String s : metaData){
	    if (s.startsWith(Globals.SELECTOR_META_PREFIX))
		fieldSelector.addItem(s.substring(Globals.SELECTOR_META_PREFIX.length()));
	}

    }


    private void updateWordPanel() {
	if (currentField == null) {
	    titledBorder2.setTitle("");
	    jPanel3.repaint();
	    return;
	}
	titledBorder2.setTitle(Globals.lang("Field")+": "+currentField);
	jPanel3.repaint();
	fillWordSelector();
	wordTf.setText("");
    }

    private void fillWordSelector() {
		wordSelector.removeAllItems();
		wordSelector.addItem(WORD_FIRSTLINE_TEXT);
		Vector<String> items = metaData.getData(Globals.SELECTOR_META_PREFIX
			+ currentField);
		if ((items != null)) { // && (items.size() > 0)) {
			wordSet = new TreeSet<String>(items);
			for (String word : wordSet)
				wordSelector.addItem(word);
		}
	}

    private void addWord() {
	if (currentField == null)
	    return;
	String word = wordTf.getText().trim();

	if (!wordSet.contains(word)) {
	    Util.pr(Globals.SELECTOR_META_PREFIX+currentField);
	    wordSet.add(word);
	    // Create a new Vector for this word list, and update the MetaData.
	    metaData.putData(Globals.SELECTOR_META_PREFIX+currentField,
			     new Vector<String>(wordSet));
	    fillWordSelector();
	    frame.basePanel().markNonUndoableBaseChanged();
	    //wordTf.selectAll();
	    wordTf.setText("");
	    wordTf.requestFocus();
	}
    }

    private void addField() {
	currentField = fieldTf.getText().trim().toLowerCase();
	if (metaData.getData(Globals.SELECTOR_META_PREFIX+currentField) == null) {
	    metaData.putData(Globals.SELECTOR_META_PREFIX+currentField,
			     new Vector<String>());
	    frame.basePanel().markNonUndoableBaseChanged();
	    setupFieldSelector();
	    updateWordPanel();
	}
    }

    private void removeWord() {
	String word = wordTf.getText().trim();
	if (wordSet.contains(word)) {
	    wordSet.remove(word);
	    // Create a new Vector for this word list, and update the MetaData.
	    metaData.putData(Globals.SELECTOR_META_PREFIX+currentField,
			     new Vector<String>(wordSet));
	    fillWordSelector();
	    frame.basePanel().markNonUndoableBaseChanged();
	    //wordTf.selectAll();
	    wordTf.setText("");
	    wordTf.requestFocus();
	}
    }

    void fieldSelector_actionPerformed(ActionEvent e) {
	if (fieldSelector.getSelectedIndex() > 0) {
	    //fieldTf.setText((String)fieldSelector.getSelectedItem());
	    currentField = (String)fieldSelector.getSelectedItem();
	    updateWordPanel();
	}
    }

    private void jbInit() {
	titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(142, 142, 142)),Globals.lang("Selector enabled fields"));
	titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(142, 142, 142)),Globals.lang("Item list for field"));
	//jPanel1.setBackground(GUIGlobals.lightGray);
	//jPanel2.setBackground(GUIGlobals.lightGray);
	//panel1.setBackground(GUIGlobals.lightGray);
	panel1.setLayout(gridBagLayout1);
	Close.setText(Globals.lang("Close"));
	Close.addActionListener(new ContentSelectorDialog_Close_actionAdapter(this));
	lab.setRequestFocusEnabled(true);
	lab.setText(Globals.lang("Field name")+":");
	fieldTf.setSelectionEnd(8);
	add.setText(Globals.lang("Add"));
	remove.setText(Globals.lang("Remove"));
	jPanel1.setLayout(gridBagLayout2);
	jPanel1.setBorder(titledBorder1);
	jPanel3.setBorder(titledBorder2);
	jPanel3.setLayout(gridBagLayout4);
	jPanel4.setLayout(gridBagLayout3);
	jLabel1.setText(Globals.lang("Word")+":");
	jPanel5.setLayout(gridBagLayout5);
	addWord.setText(Globals.lang("Add"));
	removeWord.setText(Globals.lang("Remove"));
	jPanel6.setLayout(gridBagLayout6);
	select.setText(Globals.lang("Select"));
	fieldSelector.addActionListener(new ContentSelectorDialog_fieldSelector_actionAdapter(this));
	getContentPane().add(panel1);
	this.getContentPane().add(jPanel2, BorderLayout.SOUTH);
	//JToolBar tlb = new JToolBar();
	//tlb.setLayout(new GridLayout(1,1));
	//tlb.setPreferredSize(new Dimension(28, 28));
	//tlb.setFloatable(false);
	//tlb.add(help);
	jPanel2.add(help, null);
	jPanel2.add(Close, null);

	panel1.add(jPanel1,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
						     ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
	jPanel1.add(lab,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						     ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
	jPanel1.add(fieldTf,        new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
							       ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 1, 0));
	panel1.add(jPanel3,    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
						      ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
	jPanel3.add(jLabel1,             new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
								,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
	jPanel1.add(fieldSelector,       new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
								,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
	jPanel1.add(jPanel4,    new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
						       ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	jPanel4.add(add,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						    ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
	jPanel4.add(remove,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						    ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
	jPanel3.add(wordSelector,             new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
								     ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
	jPanel3.add(wordTf,                  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
									 ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
	jPanel3.add(jPanel5,       new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
							  ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
	jPanel3.add(jPanel6,    new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
						       ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	jPanel6.add(addWord,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						      ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
	jPanel6.add(removeWord,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
							,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 5), 0, 0));
	jPanel1.add(select,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						    ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));


	add.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    addField();
		}
	    });

	wordTf.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    addWord();
		}
	    });
	addWord.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    addWord();
		}
	    });
	removeWord.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    removeWord();
		}
	    });
	wordSelector.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    if (wordSelector.getSelectedIndex() > 0) {
			wordTf.setText((String)wordSelector.getSelectedItem());
			wordTf.requestFocus();
		    }
		}
	    });



	setupFieldSelector();
    }

    void Close_actionPerformed(ActionEvent e) {
	dispose();
    }

}

class ContentSelectorDialog_Close_actionAdapter implements java.awt.event.ActionListener {
    ContentSelectorDialog adaptee;

    ContentSelectorDialog_Close_actionAdapter(ContentSelectorDialog adaptee) {
	this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
	adaptee.Close_actionPerformed(e);
    }
}

class ContentSelectorDialog_fieldSelector_actionAdapter implements java.awt.event.ActionListener {
    ContentSelectorDialog adaptee;

    ContentSelectorDialog_fieldSelector_actionAdapter(ContentSelectorDialog adaptee) {
	this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
	adaptee.fieldSelector_actionPerformed(e);
    }
}
