/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.StringWriter;
import java.io.IOException;
import net.sf.jabref.undo.*;
import net.sf.jabref.export.LatexFieldFormatter;
import java.beans.*;

public class EntryTypeForm extends JDialog implements VetoableChangeListener {

    /* 
     * GUI component that allows editing of the fields of a BibtexEntry.
     * EntryTypeForm also registers itself as a VetoableChangeListener,
     * receiving events whenever a field of the entry changes, enabling the
     * text fields to update themselves if the change is made from somewhere
     * else.
     */

    // A reference to the entry this object works on.
    BibtexEntry entry;

    CloseAction closeAction; 
    // The action concerned with closing the window.

    CopyKeyAction copyKeyAction;
    // The action concerned with copying the BibTeX key to the clipboard.


    AbstractAction 
	nextEntryAction = new NextEntryAction(),
	prevEntryAction = new PrevEntryAction();
    // Actions for switching to next/previous entry.

    StoreFieldAction storeFieldAction;
    // The action concerned with storing a field value.


    SwitchLeftAction switchLeftAction = new SwitchLeftAction();
    SwitchRightAction switchRightAction = new SwitchRightAction();
    // The actions concerned with switching the panels.

    GenerateKeyAction generateKeyAction ;
    // The action which generates a bibtexkey for this entry. 

    Container contentPane = getContentPane();
    JPanel mainPanel = new JPanel(), // The area below the toolbar.
	srcPanel = new JPanel();
    FieldPanel reqPanel = new FieldPanel(),
	optPanel = new FieldPanel(),
	genPanel = new FieldPanel();
    JTextField bibtexKey;
    JTextArea source;
    JTabbedPane tabbed = new JTabbedPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JLabel lab;
    JabRefFrame frame;
    BasePanel panel;
    EntryTypeForm ths = this;

    boolean updateSource = true; // This can be set to false to stop the source
    // text area from gettin updated. This is used in cases where the source
    // couldn't be parsed, and the user is given the option to edit it.
    boolean lastSourceAccepted = true; // This indicates whether the last attempt
    // at parsing the source was successful. It is used to determine whether the
    // dialog should close; it should stay open if the user received an error
    // message about the source, whatever he or she chose to do about it.
    String lastSourceStringAccepted = null; // This is used to prevent double
    // updates after editing source.
    double optW = 0, reqW = 1, genW = 0; // Variables for total weight of fields.
    // These values can be used to calculate the preferred height for the form.
    // reqW starts at 1 because it needs room for the bibtex key field.

    private final int REQ=0, OPT=1, GEN=2, FIELD_WIDTH=40, FIELD_HEIGHT=2;
    private final String KEY_PROPERTY = "bibtexkey";
    JabRefPreferences prefs;
    HelpAction helpAction;

    public EntryTypeForm(JabRefFrame frame_, BasePanel panel_,
			 BibtexEntry entry_, JabRefPreferences prefs_) {
	super(frame_);
	frame = frame_;
	panel = panel_;
	entry = entry_;
	prefs = prefs_;

	entry.addPropertyChangeListener(this);

	setTitle(entry.getType().getName());
	setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	helpAction = new HelpAction
	    (frame.helpDiag, GUIGlobals.entryEditorHelp, "Help (F1)");
	closeAction = new CloseAction();
	copyKeyAction = new CopyKeyAction();
//    generateKeyAction = new GenerateKeyAction(baseFrame,entry);
    generateKeyAction = new GenerateKeyAction(frame);
	storeFieldAction = new StoreFieldAction(this);
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    closeAction.actionPerformed(null);
		}	
		public void windowOpened(WindowEvent e) {
		    switch (tabbed.getSelectedIndex()) {
		    case 0:
			reqPanel.activate();
			break;
		    case 1:
			optPanel.activate();
			break;
		    case 2:
			genPanel.activate();
			break;
		    case 3:
			source.requestFocus();
			break;
		    }
		}	       
	    });


	contentPane.setLayout(new BorderLayout());
	setupToolBar();
	setupFieldPanels(reqPanel, optPanel, genPanel);
	setupSourcePanel();
	tabbed.setTabPlacement(SwingConstants.TOP);
	tabbed.addTab("Required fields", 
		      new ImageIcon(GUIGlobals.showReqIconFile),
		      reqPanel.getPane(), "Show required fields");
	tabbed.addTab("Optional fields", 
		      new ImageIcon(GUIGlobals.showOptIconFile),
		      optPanel.getPane(), "Show optional fields");
	tabbed.addTab("General fields", 
		      new ImageIcon(GUIGlobals.showGenIconFile), 
		      genPanel.getPane(), "Show general fields");
	tabbed.addTab("Bibtex source", 
		      new ImageIcon(GUIGlobals.sourceIconFile), 
		      srcPanel, "Show/edit bibtex source");
	tabbed.addChangeListener(new TabListener());

	contentPane.add(tabbed, BorderLayout.CENTER);

	// We replace the default FocusTraversalPolicy with a subclass
	// that only allows FieldEditor components to gain keyboard focus.	
	setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
		protected boolean accept(Component c) {
		    return (super.accept(c) && (c instanceof JTextComponent));
		}
	    });

	//Util.pr("opt: "+optW+"  req:"+reqW);
	int prefHeight = (int)(Math.max(genW, Math.max(optW, reqW))*GUIGlobals.FORM_HEIGHT[prefs.getInt("entryTypeFormHeightFactor")]);
	setSize(GUIGlobals.FORM_WIDTH[prefs.getInt("entryTypeFormWidth")], prefHeight);
	if (prefs.getBoolean("defaultShowSource")) {
	    tabbed.setSelectedIndex(3);
	}
	setResizable(true);
    }

    private void setupToolBar() {
	JToolBar tlb = new JToolBar();

	// The toolbar carries all the key bindings that are valid for the whole
	// window.
	ActionMap am = tlb.getActionMap();
	InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	
	im.put(GUIGlobals.exitDialog, "close");
	am.put("close", closeAction);
	im.put(KeyStroke.getKeyStroke(GUIGlobals.storeFieldKey), "store");
	am.put("copyKey", copyKeyAction);
	im.put(GUIGlobals.generateKeyStroke, "generateKey");
	am.put("generateKey", generateKeyAction);
	im.put(GUIGlobals.switchPanelLeft, "left");
	am.put("left", switchLeftAction);
	im.put(GUIGlobals.switchPanelRight, "right");
	am.put("right", switchRightAction);
	im.put(GUIGlobals.undoStroke, "undo");
	am.put("undo", undoAction);
	im.put(GUIGlobals.redoStroke, "redo");
	am.put("redo", redoAction);
	im.put(GUIGlobals.helpKeyStroke, "help");
	am.put("help", helpAction);

	tlb.setFloatable(false);
	//tlb.add(closeAction);
	//tlb.addSeparator();
	tlb.add(copyKeyAction);
	tlb.add(generateKeyAction);
	tlb.addSeparator();
	//tlb.add(undoAction);
	//tlb.add(redoAction);
	tlb.add(prevEntryAction);
	tlb.add(nextEntryAction);
	tlb.addSeparator();
	tlb.add(helpAction);
	contentPane.add(tlb, BorderLayout.NORTH);
    }

    private void setupFieldPanels(FieldPanel req, FieldPanel opt, FieldPanel gen) {

	// First we ask the BibtexEntry which fields are optional and
	// required.
	String[] reqFields = entry.getRequiredFields(),
	    optFields = entry.getOptionalFields(),
//        genFields = new String[] {"crossref", "url", "abstract", "comment"}; // May change...
	    genFields = entry.getGeneralFields() ; 
	
	int iter, rmax, omax, gmax;
	
	gmax = genFields.length;
	
	if (reqFields == null) {
	    iter = optFields.length;
	    rmax = 0;
	    omax = optFields.length;
	} else if (optFields == null) {
	    iter = reqFields.length;
	    rmax = reqFields.length;
	    omax = 0;
	} else {
	    iter = Math.max(reqFields.length, optFields.length);
	    rmax = reqFields.length;
	    omax = optFields.length;
	}
	FieldTextArea ta1 = null, ta2 = null, ta3 = null, firstR = null, firstO = null;
	String stringContent;
	Object content;

	req.setLayout(gbl);
	opt.setLayout(gbl);
	gen.setLayout(gbl);
	con.insets = new Insets(10,5,0,5);
	//con.fill = GridBagConstraints.HORIZONTAL;

	con.anchor = GridBagConstraints.WEST;

	for (int i=0; i<iter; i++) {

	    // Constraints for the labels.
	    con.gridwidth = 1;
	    con.weightx = 0;
	    con.weighty = 0;
	    con.fill = GridBagConstraints.NONE;
	    if (i<rmax) {
		if ((content = entry.getField(reqFields[i])) != null) {
		    stringContent = content.toString();
		} else
		    stringContent = null;
	    		
		ta1 = new FieldTextArea(reqFields[i], stringContent);

		setupJTextComponent(ta1);
		if (i==0) {
		    firstR = ta1;
		    req.setActive(ta1);
		}
	    }
	    if (i<omax) {
		if ((content = entry.getField(optFields[i])) != null) {
		    stringContent = content.toString();
		} else
		    stringContent = null;

		ta2 = new FieldTextArea(optFields[i], stringContent);
		setupJTextComponent(ta2);
		if (i==0) { 
		    firstO = ta2;
		    opt.setActive(ta2);
		}
	    }
	    if (i<gmax) {
		if ((content = entry.getField(genFields[i])) != null) {
		    stringContent = content.toString();
		} else
		    stringContent = null;

		ta3 = new FieldTextArea(genFields[i], stringContent);
		setupJTextComponent(ta3);
		if (i==0) { 
		    firstO = ta3;
		    gen.setActive(ta3);
		}
	    }
	    if (i<rmax) {
		gbl.setConstraints(ta1.getLabel(),con);
		req.add(ta1.getLabel());
	    }
	    if (i<omax) {
		gbl.setConstraints(ta2.getLabel(),con);
		opt.add(ta2.getLabel());
	    }
	    if (i<gmax) {
		gbl.setConstraints(ta3.getLabel(),con);
		gen.add(ta3.getLabel());
	    }

	    // Constraints for the text fields.
	    con.gridwidth = GridBagConstraints.REMAINDER;
	    con.weightx = 1;

	    con.fill = GridBagConstraints.BOTH;
	    if (i<rmax) {
		con.weighty = GUIGlobals.getFieldWeight(reqFields[i]);
		reqW += con.weighty;
		//Util.pr(reqFields[i]+" "+con.weighty+"");
		gbl.setConstraints(ta1.getPane(),con);
		req.add(ta1.getPane());
	    }
	    if (i<omax) {
		con.weighty = GUIGlobals.getFieldWeight(optFields[i]);
		optW += con.weighty;
		gbl.setConstraints(ta2.getPane(),con);
		opt.add(ta2.getPane());
	    }
	    if (i<gmax) {
		con.weighty = GUIGlobals.getFieldWeight(genFields[i]);
		genW += con.weighty;
		gbl.setConstraints(ta3.getPane(),con);
		gen.add(ta3.getPane());
	    }
	}
	// Add the edit field for Bibtex-key.
	
	con.insets.top += 25;
	con.insets.bottom = 10;
	con.gridwidth = 1;
	con.weighty = 0;
	con.weightx = 0;
	con.anchor = GridBagConstraints.SOUTHWEST;
	con.fill = GridBagConstraints.NONE;
	FieldTextField tf = 
	    new FieldTextField(KEY_PROPERTY,
			       (String)entry.getField(KEY_PROPERTY));
	gbl.setConstraints(tf.getLabel(),con);
	req.add(tf.getLabel());
	con.gridwidth = GridBagConstraints.REMAINDER;
	//	con.anchor = GridBagConstraints.WEST;
	con.weightx = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	setupJTextComponent(tf);
	gbl.setConstraints(tf,con);
	req.add(tf);

    }

    private void setupSourcePanel() {
	source = new JTextArea();
	con = new GridBagConstraints();
	con.insets = new Insets(10,10,10,10);
	con.fill = GridBagConstraints.BOTH;
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.gridheight = GridBagConstraints.REMAINDER;
	con.weightx = 1;
	con.weighty = 1;
	srcPanel.setLayout(gbl);
	source.setEditable(prefs.getBoolean("enableSourceEditing"));
	source.setLineWrap(true);
	source.setTabSize(GUIGlobals.INDENT);
	setupJTextComponent(source);
	updateSource();


	JScrollPane sp = new JScrollPane(source, 
					 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	gbl.setConstraints(sp, con);
	srcPanel.add(sp);

    
    }

    private void updateSource() {
	if (updateSource) {
	    StringWriter sw = new StringWriter(200);
	    try {
		entry.write(sw, new net.sf.jabref.export.LatexFieldFormatter());
		String srcString = sw.getBuffer().toString();
		source.setText(srcString);
	    } catch (IOException ex) {
		source.setText("Error: "+ex.getMessage()+"\n\n"
			       +"Correct the entry, and "
			       +"reopen editor to display/edit source.");
		source.setEditable(false);
	    }
	}
    }

    private void setupJTextComponent(JTextComponent ta) {

	// Activate autocompletion if it should be used for this field.
	/*
	if ((ta instanceof FieldTextArea) && 
	    (prefs.getBoolean("autoComplete"))) {
	    FieldTextArea fta = (FieldTextArea)ta;
	    Completer comp = baseFrame.getAutoCompleter(fta.getFieldName());
	    if (comp != null)
		fta.setAutoComplete(comp);
	}
	*/

	// Set up key bindings and focus listener for the FieldEditor.
	InputMap im = ta.getInputMap(JComponent.WHEN_FOCUSED);
	ActionMap am = ta.getActionMap();
	//im.put(KeyStroke.getKeyStroke(GUIGlobals.closeKey), "close");
	//am.put("close", closeAction);
	im.put(KeyStroke.getKeyStroke(GUIGlobals.storeFieldKey), "store");
	am.put("store", storeFieldAction);
	im.put(GUIGlobals.switchPanelLeft, "left");
	am.put("left", switchLeftAction);
	im.put(GUIGlobals.switchPanelRight, "right");
	am.put("right", switchRightAction);
	im.put(GUIGlobals.helpKeyStroke, "help");
	am.put("help", helpAction);
	
	try{
	    int i = 0 ; 
	    HashSet keys =  new HashSet(ta.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS)) ; 
	    keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB")) ; 
	    ta.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys) ; 
	    keys = new HashSet(ta.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS)) ; 
	    keys.add(KeyStroke.getKeyStroke("shift pressed TAB")) ; 
	    ta.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys) ;
	}catch(Throwable t){
	    System.err.println(t) ; 
	}
	
	
	ta.addFocusListener(new FieldListener());
    }

    class FieldListener extends FocusAdapter {
	/*
	 * Focus listener that fires the storeFieldAction when a FieldTextArea
	 * loses focus.
	 */
	public void focusGained(FocusEvent e) {
	    //Util.pr("Gained focus "+e.getSource().toString().substring(0,30));
	    if (e.getSource() instanceof FieldEditor) {
		FieldEditor ta = (FieldEditor)e.getSource();
		Component parent = ta.getParent();
		while (!(parent instanceof FieldPanel)) {
		    parent = parent.getParent();
		}
		((FieldPanel)parent).setActive(ta);
	    } else {
		// The source panel must have been chosen. Update it.
		if (panel.baseChanged)
		    updateSource();
	    }
	}

	public void focusLost(FocusEvent e) {	  
	    //Util.pr("Lost focus "+e.getSource().toString().substring(0,30));
	    if (!e.isTemporary()) {
		storeFieldAction.actionPerformed
		    (new ActionEvent(e.getSource(), 0, ""));
	    }
	}

    }

    class FieldPanel extends JPanel {

	/*
	 * This extension to JPanel keeps a reference to its active
	 * field, on behalf of which it requests the focus when
	 * it is told to.
	 */

	FieldEditor activeField = null;
	JScrollPane sp;

	public JComponent getPane() {
	    return this; // Component to add. Return the scrollpane, if there is one.
	}

	public void setActive(FieldEditor c) {
	    activeField = c;
	}

    public Vector getFields(){
        Vector textFields  = new Vector() ;  
        Component[] components = this.getComponents() ; 

        try{
            for(int i = 0 ; i < components.length ; i++){
                if(components[i]  instanceof FieldEditor){
                    textFields.add(components[i]) ; 
                }
		//else if ((components[i] instanceof JScrollPane)) {
		//    Util.pr(((JScrollPane)components[i]).getViewport().getComponent(0).toString().substring(0,50));
		//}
		else if (components[i] instanceof JScrollPane) {
		    textFields.add(((JScrollPane)components[i]).getViewport()
				   .getComponent(0));
		}
	    }
            

            return textFields ; 
        }catch(ClassCastException cce){
            System.err.println("caught in getFields: "+cce) ; 
        }
        return null ; 
    }


    
	public void activate() {
	    if (activeField != null)
		activeField.requestFocus();
	}

    }

    class TabListener implements ChangeListener {
	public void stateChanged(ChangeEvent e) {
	    if (((JTabbedPane)e.getSource()).getSelectedIndex() < 3) {
		FieldPanel fp = (FieldPanel)(((JTabbedPane)e.getSource()).getSelectedComponent());
		fp.activate();
	    } else {
		source.requestFocus();
	    }
	}
    }

    

    class CloseAction extends AbstractAction {
	public CloseAction() {
	    super("Close window");
	    //, new ImageIcon(GUIGlobals.closeIconFile));
	    putValue(SHORT_DESCRIPTION, "Close window (Ctrl-Q)");
	}    
	public void actionPerformed(ActionEvent e) {
	    if (tabbed.getSelectedComponent() == srcPanel) {
		storeFieldAction.actionPerformed(new ActionEvent(source, 0, ""));
		if (lastSourceAccepted) {
		    panel.entryTypeFormClosing(entry.getId());
		    dispose();
		}
	    } else {
		panel.entryTypeFormClosing(entry.getId());
		dispose();
	    }
	}
    }

    class CopyKeyAction extends AbstractAction {
	public CopyKeyAction() {
	    super("Copy BibTeX key to clipboard",
		  new ImageIcon(GUIGlobals.copyKeyIconFile));
	    putValue(SHORT_DESCRIPTION, "Copy BibTeX key to clipboard (Ctrl-K)");
	    //putValue(MNEMONIC_KEY, GUIGlobals.copyKeyCode);
	}

   
	public void actionPerformed(ActionEvent e) {
	    String s = (String)(entry.getField(KEY_PROPERTY));
	    StringSelection ss = new StringSelection(s);
	    if (s != null) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss,ss);
	    }
	}
    }




    class StoreFieldAction extends AbstractAction {
	JDialog parent;
	public StoreFieldAction(JDialog parent) {
	    super("Store field value");
	    this.parent = parent;
	    putValue(SHORT_DESCRIPTION, "Store field value");
	}
	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() instanceof FieldEditor) {
		String toSet = null, fieldName = null;
		FieldEditor fe = (FieldEditor)e.getSource();
		boolean set;
		if (fe.getText().length() > 0)
		    toSet = fe.getText();
		// We check if the field has changed, since we don't want to mark the
		// base as changed unless we have a real change.
		if (toSet == null) {
		    if (entry.getField(fe.getFieldName()) == null)
			set = false;
		    else
			set = true;
		} else {
		    if ((entry.getField(fe.getFieldName()) != null)
			&& toSet.equals(entry.getField(fe.getFieldName()).toString()))
			set = false;
		    else
			set = true;
		}
		
		if (set) try {

		    // The following statement attempts to write the
		    // new contents into a StringWriter, and this will
		    // cause an IOException if the field is not
		    // properly formatted. If that happens, the field
		    // is not stored and the textarea turns red.

		    if (toSet != null)
			(new LatexFieldFormatter()).format(toSet);
		    
		    Object oldValue = entry.getField(fe.getFieldName());
		    entry.setField(fe.getFieldName(), toSet);
		    if ((toSet != null) && (toSet.length() > 0)) {
			fe.setLabelColor(GUIGlobals.validFieldColor);
			fe.setBackground(GUIGlobals.validFieldBackground);
		    } else {
			fe.setLabelColor(GUIGlobals.nullFieldColor);
			fe.setBackground(GUIGlobals.validFieldBackground);
		    }
		    
		    // Add an UndoableFieldChange to the baseframe's undoManager.
		    panel.undoManager.addEdit
			(new UndoableFieldChange(entry, fe.getFieldName(),
						 oldValue, toSet));

		    panel.refreshTable();
		    panel.markBaseChanged();

		} catch (IllegalArgumentException ex) {
		    frame.output("Invalid field format: "+ex.getMessage());
		    fe.setLabelColor(GUIGlobals.invalidFieldColor);
		    fe.setBackground(GUIGlobals.invalidFieldBackground);
		} /*catch (java.io.IOException ex2) {
		    fe.setLabelColor(GUIGlobals.invalidFieldColor);
		    fe.setBackground(GUIGlobals.invalidFieldBackground);		
		    }*/
		else {
		    // set == false
		    // We set the field and label color.
		    fe.setBackground(GUIGlobals.validFieldBackground);
		    fe.setLabelColor((toSet == null) ?
				     GUIGlobals.nullFieldColor :
				     GUIGlobals.validFieldColor);
		}
			    
	    } else if ((source.isEditable()) 
		       && (source.getText() != lastSourceStringAccepted)) {
		// Store edited bibtex code.
		BibtexParser bp = new BibtexParser
		    (new java.io.StringReader(source.getText()));	
		try {
		    BibtexDatabase db = bp.parse().getDatabase();
		    if (db.getEntryCount() > 1)
			throw new Exception("More than one entry found.");
		    if (db.getEntryCount() < 1)
			throw new Exception("No entries found.");
		    String id = entry.getId();

		    NamedCompound compound = new NamedCompound("source edit");
		    BibtexEntry nu = db.getEntryById
			((String)db.getKeySet().iterator().next());
		    boolean anyChanged = false;

		    // First, remove fields that the user have removed.
		    Object[] fields = entry.getAllFields();
		    for (int i=0; i<fields.length; i++)
			if (GUIGlobals.isWriteableField(fields[i].toString()))
			    if (nu.getField(fields[i].toString()) == null) {
				compound.addEdit(new UndoableFieldChange
						 (entry, fields[i].toString(),
						  entry.getField(fields[i].toString()), 
						  (Object)null));
				entry.setField(fields[i].toString(), null);
				anyChanged = true;
			    }			

		    // Then set all fields that have been set by the user.
		    fields = nu.getAllFields();
		    for (int i=0; i<fields.length; i++) 
			if (entry.getField(fields[i].toString()) !=
			    nu.getField(fields[i].toString()))
			    {
				compound.addEdit
				    (new UndoableFieldChange
				     (entry, fields[i].toString(),
				      entry.getField(fields[i].toString()), 
				      nu.getField(fields[i].toString())));	
				entry.setField(fields[i].toString(), 
					       nu.getField(fields[i].toString()));
				anyChanged = true;
			    }
		    compound.end();
		    if (anyChanged)
			panel.undoManager.addEdit(compound);
		    lastSourceStringAccepted = source.getText();
		    updateAllFields();
		    lastSourceAccepted = true;

		    updateSource = true;
		    panel.refreshTable();
		    panel.markBaseChanged();
		} catch (Throwable ex) {
		    // The source couldn't be parsed, so the user is given an
		    // error message, and the choice to keep or revert the contents
		    // of the source text field.
		    updateSource = false;
		    lastSourceAccepted = false;
		    tabbed.setSelectedComponent(srcPanel);

		    Object[] options = { "Edit","Revert to original source" };
		    
		    int answer = JOptionPane.showOptionDialog
			(parent, "Error: "+ex.getMessage(),
			 "Problem with parsing entry",
			 JOptionPane.YES_NO_OPTION, 
			 JOptionPane.ERROR_MESSAGE,null, options,options[0]);
		    if (answer == 0) {
			//updateSource = true;
		    } else {
			updateSource = true;
			updateSource();
		    }
		}
	    
	    }
	}
    }

    class SwitchLeftAction extends AbstractAction {
	public SwitchLeftAction() {
	    super("Switch to the panel to the left");
	}    
	public void actionPerformed(ActionEvent e) {
	    int i = tabbed.getSelectedIndex();
	    tabbed.setSelectedIndex((i>0 ? i-1 : tabbed.getTabCount()-1));
	    if (tabbed.getSelectedComponent() instanceof FieldPanel)
		((FieldPanel)tabbed.getSelectedComponent()).activate(); 
	       // Set focus to the last used textfield.
	}
    }

    class SwitchRightAction extends AbstractAction {
	public SwitchRightAction() {
	    super("Switch to the panel to the right");
	}    
	public void actionPerformed(ActionEvent e) {
	    int i = tabbed.getSelectedIndex();
	    tabbed.setSelectedIndex(i<tabbed.getTabCount()-1 ? i+1 : 0);
	    if (tabbed.getSelectedComponent() instanceof FieldPanel)
		((FieldPanel)tabbed.getSelectedComponent()).activate(); 
     	        // Set focus to the last used textfield.
	}
    }
    /*
    class ShowReqAction extends AbstractAction {
	public ShowReqAction() {
	    super("Show required",
		  new ImageIcon(GUIGlobals.showReqIconFile));
	    putValue(SHORT_DESCRIPTION, "Show required fields");
	    putValue(MNEMONIC_KEY, GUIGlobals.showReqKeyCode);
	}
    
	public void actionPerformed(ActionEvent e) {
	    //System.out.println("Show required fields");
	    tabbed.setSelectedIndex(REQ);
	    reqPanel.activate(); // Set focus to the last used textfield.
	}
    }

    class ShowOptAction extends AbstractAction {
	public ShowOptAction() {
	    super("Show optional", 
		  new ImageIcon(GUIGlobals.showOptIconFile));
	    putValue(SHORT_DESCRIPTION, "Show optional fields");
	    putValue(MNEMONIC_KEY, GUIGlobals.showOptKeyCode);
	}
    
	public void actionPerformed(ActionEvent e) {
	    tabbed.setSelectedIndex(OPT);
	    optPanel.activate(); // Set focus to the last used textfield.
	}
    }


    class ShowGenAction extends AbstractAction {
	public ShowGenAction() {
	    super("Show general", 
		  new ImageIcon(GUIGlobals.showGenIconFile));
	    putValue(SHORT_DESCRIPTION, "Show general fields");
	    putValue(MNEMONIC_KEY, GUIGlobals.showGenKeyCode);
	}
    
	public void actionPerformed(ActionEvent e) {
	    tabbed.setSelectedIndex(GEN);
	    genPanel.activate(); // Set focus to the last used textfield.
	}
    }
    */

    class NextEntryAction extends AbstractAction {
	    public NextEntryAction() {
		super(Globals.lang("Next entry"),
		      new ImageIcon(GUIGlobals.forwardIconFile));

		putValue(SHORT_DESCRIPTION, Globals.lang("Next entry"));
	    }
	    public void actionPerformed(ActionEvent e) {
		int thisRow = panel.tableModel
		    .getNumberFromName(entry.getId());
		String id = null;
		if (thisRow+1 < panel.database.getEntryCount())
		    id = panel.tableModel
			.getNameFromNumber(thisRow+1);
		else if (thisRow > 0)
		    id = panel.tableModel
			.getNameFromNumber(0);
		switchTo(id);
	    }
    }

    class PrevEntryAction extends AbstractAction {
	    public PrevEntryAction() {
		super(Globals.lang("Previous entry"),
		      new ImageIcon(GUIGlobals.backIconFile));

		putValue(SHORT_DESCRIPTION, Globals.lang("Previous entry"));
		
	    }
	    public void actionPerformed(ActionEvent e) {
		int thisRow = panel.tableModel
		    .getNumberFromName(entry.getId());
		String id = null;
		if (thisRow-1 >= 0)
		    id = panel.tableModel
			.getNameFromNumber(thisRow-1);
		else if (thisRow != panel.database.getEntryCount()-1)
		    id = panel.tableModel
			.getNameFromNumber(panel.database.getEntryCount()-1);
		switchTo(id);
	    }
	};
    
    private void switchTo(String id) {
	if (id != null) {
	    if (!panel.entryTypeForms.containsKey(id)) {
		BibtexEntry be = panel.database.getEntryById(id);

		// If the entry we are switching to is of the same type as
		// this one, we can make the switch more elegant by keeping this
		// same dialog, and updating it.
		if (entry.getType() == be.getType()) {
		    panel.entryTypeForms.remove(entry.getId());
		    entry = be;
		    updateAllFields();
		    updateSource();
		    panel.entryTypeForms.put(id, ths);

		} else {

		    EntryTypeForm form = new EntryTypeForm
			(frame, panel, be, prefs);
		    form.setLocation(ths.getLocation());
		    //Util.placeDialog(form, frame); 
		    // We want to center the editor.
		    form.setVisible(true);
		    dispose();
		    panel.entryTypeForms.remove(entry.getId());
		    panel.entryTypeForms.put(id, form);
		} 
	    } else {
		((EntryTypeForm)(panel.entryTypeForms.get(id)))
		    .setVisible(true);
		panel.entryTypeForms.remove(entry.getId());
		dispose();
	    }
	    
	    
	}
    }

    class GenerateKeyAction extends AbstractAction {
        JabRefFrame parent ; 
        BibtexEntry selectedEntry ; 
        public GenerateKeyAction(JabRefFrame parentFrame) {
            super("Generate Bibtexkey", 
              new ImageIcon(GUIGlobals.genKeyIconFile));
            parent = parentFrame ; 
//            selectedEntry = newEntry ; 
            putValue(SHORT_DESCRIPTION, "Generate Bibtexkey (Ctrl-G)");
    //        putValue(MNEMONIC_KEY, GUIGlobals.showGenKeyCode);
        }
    
        public void actionPerformed(ActionEvent e) {
	    // 1. get Bitexentry for selected index (already have)
            // 2. run the LabelMaker by it
	    
	    try {
               // this updates the table automatically, on close, but not within the tab
	       Object oldValue = entry.getField(GUIGlobals.KEY_FIELD);
	       entry = frame.labelMaker.applyRule(entry) ; 
	       
	       // Store undo information:
	       panel.undoManager.addEdit(new UndoableFieldChange
					  (entry, GUIGlobals.KEY_FIELD, oldValue, 
					   entry.getField(GUIGlobals.KEY_FIELD)));

            // here we update the field
	       String bibtexKeyData = (String) entry.getField
		   (Globals.KEY_FIELD) ; 
            // set the field named for "bibtexkey"
			setField(Globals.KEY_FIELD, bibtexKeyData) ;
			panel.markBaseChanged();
			panel.refreshTable();
           }
           catch (Throwable t){
	                System.err.println("error setting key: " +t) ; 			
           }
        }
    }

    UndoAction undoAction = new UndoAction();
    class UndoAction extends AbstractAction {
	public UndoAction() {
	    super("Undo", new ImageIcon(GUIGlobals.undoIconFile));
	    putValue(SHORT_DESCRIPTION, "Undo");
	}    
	public void actionPerformed(ActionEvent e) {
	    panel.runCommand("undo");
	}
    }

    RedoAction redoAction = new RedoAction();
    class RedoAction extends AbstractAction {
	public RedoAction() {
	    super("Undo", new ImageIcon(GUIGlobals.redoIconFile));
	    putValue(SHORT_DESCRIPTION, "Redo");
	}    
	public void actionPerformed(ActionEvent e) {
	    panel.runCommand("redo");
	}
    }

    public boolean setField(String fieldName, String newFieldData){
        // iterate through all tabs and fields within those tabs until we get 
        // the appropriate field name.  
        // Thanks to reflection, this shouldn't be too bad

        // search each panel individually
 
        try{

	    if (setFieldInPanel(reqPanel, fieldName, newFieldData))
		return true;
	    if (setFieldInPanel(optPanel, fieldName, newFieldData))
		return true;
	    if (setFieldInPanel(genPanel, fieldName, newFieldData))
		return true;

        }catch(ClassCastException cce){
			System.err.println("caught in setField: "+cce) ; 
            return false ; 
        }

        return false ; 
    }

    private boolean setFieldInPanel(FieldPanel pan, String fieldName, 
				    String newFieldData) throws ClassCastException {
	Vector fields = pan.getFields() ; 
	for(int i = 0 ; i < fields.size() ; i++){
	    if(((FieldEditor) fields.elementAt(i)).getFieldName().equals(fieldName)){
		((FieldEditor) fields.elementAt(i)).setText(newFieldData) ;
		return true ; 
	    }
	}
	return false; // Nothing found.
    }

    private void updateAllFields() {
	FieldPanel[] panels = new FieldPanel[] {reqPanel, optPanel, genPanel};
	for (int i=0; i<panels.length; i++) {
	    Vector fields = panels[i].getFields();
	    for (int j=0; j<fields.size(); j++) {
		FieldEditor ed = (FieldEditor)fields.elementAt(j);
		Object content = entry.getField(ed.getFieldName());
		ed.setText(content == null ? "" : content.toString());
		//if (ed.getFieldName().equals("year"))
		//    Util.pr(content.toString());
	    }
	}
    }

    // Update the JTextArea when a field has changed.
    public void vetoableChange(PropertyChangeEvent e) {
	setField(e.getPropertyName(), (String)(e.getNewValue())); 
	//Util.pr(e.getPropertyName());
    }
	
	class ExternalViewerListener extends MouseAdapter {
		public void mouseClicked(MouseEvent evt) {
			if (evt.getClickCount() == 2) {
				JTextField tf = (JTextField)evt.getSource();
				tf.selectAll();
				String link = tf.getText(); // get selected ?  String 	getSelectedText() 
				// check html first since browser can invoke viewers
				if(link.substring(0,7).equals("http://")){ // hml
					try {
						System.err.println("Message: Opening url (" + link + ") with the HTML viewer (" + prefs.get("htmlviewer") +")");						
						Process child = Runtime.getRuntime().exec(prefs.get("htmlviewer") + " " + link);
					} catch (IOException e) {
						System.err.println("Warning: Unable to open url " + link + " with the HTML viewer (" + prefs.get("htmlviewer") +")");			
					}
				}
				else if(link.endsWith(".ps")){
					try {
						System.err.println("Message: Opening file " + link + " with the ps viewer (" + prefs.get("psviewer") +")");									
						Process child = Runtime.getRuntime().exec(prefs.get("psviewer") + " " + link);
					} catch (IOException e) {
						System.err.println("Warning: Unable to open file (" + link + ") with the postscipt viewer (" + prefs.get("psviewer") +")");
					}
					
				}else if(link.endsWith(".pdf")){
					try {
						System.err.println("Message: Opening file (" + link + ") with the pdf viewer (" + prefs.get("pdfviewer") +")");						
						Process child = Runtime.getRuntime().exec(prefs.get("pdfviewer") + " " + link);
					} catch (IOException e) {
						System.err.println("Warning: Unable to open file " + link + " with the pdf viewer (" + prefs.get("pdfviewer") +")");			
					}
				}
				
				else{
					System.err.println("Message: currently only pdf, ps and HTML files can be opened by double clicking");
					//ignore
				}

			}
			
		}
    }
 

}
