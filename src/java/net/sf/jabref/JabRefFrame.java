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

import net.sf.jabref.label.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Vector;
import java.io.*;
import java.net.URL;

/**
 * The main window of the application.
 */
public class JabRefFrame extends JFrame {

    JabRefFrame ths = this;
    JabRefPreferences prefs = new JabRefPreferences();

    JTabbedPane tabbedPane = new JTabbedPane();
    JToolBar tlb = new JToolBar();
    JMenuBar mb = new JMenuBar();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JTextField searchField = new JTextField("", 30);
    JLabel statusLine = new JLabel("", SwingConstants.LEFT),
	statusLabel = new JLabel(Globals.lang("Status")+":",
				 SwingConstants.LEFT);

    LabelMaker labelMaker;
    File fileToOpen = null;

    // The help window.
    public HelpDialog helpDiag = new HelpDialog(this);
    


    // Here we instantiate menu/toolbar actions. Actions regarding
    // the currently open database are defined as a GeneralAction
    // with a unique command string. This causes the appropriate
    // BasePanel's runCommand() method to be called with that command.
    // Note: GeneralAction's constructor automatically gets translations
    // for the name and message strings.
    AbstractAction
	open = new OpenDatabaseAction(),
	close = new CloseDatabaseAction(),
	newDatabaseAction = new NewDatabaseAction(),
	save = new GeneralAction("save", "Save database",
				 "Save database", GUIGlobals.saveIconFile),
	saveAs = new GeneralAction("saveAs", "Save database as ...",
				 "Save database as ...", 
				   GUIGlobals.saveAsIconFile),
	undo = new GeneralAction("undo", "Undo", "Undo",
				 GUIGlobals.undoIconFile),
	redo = new GeneralAction("redo", "Redo", "Redo",
				 GUIGlobals.redoIconFile),
	cut = new GeneralAction("cut", "Cut", "Cut",
				 GUIGlobals.cutIconFile),
	copy = new GeneralAction("copy", "Copy", "Copy",
				 GUIGlobals.copyIconFile),
	paste = new GeneralAction("paste", "Paste", "Paste",
				 GUIGlobals.pasteIconFile),

	/*remove = new GeneralAction("remove", "Remove", "Remove selected entries",
	  GUIGlobals.removeIconFile),*/
	selectAll = new GeneralAction("selectAll", "Select all"),
	editPreamble = new GeneralAction("editPreamble", "Edit preamble", 
					 "Edit preamble",
					 GUIGlobals.preambleIconFile),
	editStrings = new GeneralAction("editStrings", "Edit strings", 
					"Edit strings",
					GUIGlobals.stringsIconFile),
	toggleGroups = new GeneralAction("toggleGroups", "Toggle groups inteface", 
					 "Toggle groups inteface",
					 GUIGlobals.groupsIconFile);	

    public JabRefFrame() {
	//Globals.setLanguage("no", "");
	setTitle(GUIGlobals.frameTitle);
	setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    (new CloseAction()).actionPerformed(null);
		}	
	    });
	
	initLabelMaker();
	  
	setupLayout();		
	setSize(new Dimension(prefs.getInt("sizeX"),
			      prefs.getInt("sizeY")));
	setLocation(new Point(prefs.getInt("posX"),
			      prefs.getInt("posY")));

	// If the option is enabled, open the last edited databases, if any.
	if (prefs.getBoolean("openLastEdited") 
	    && (prefs.get("lastEdited") != null)) {
	    
	    // How to handle errors in the databases to open?
	    String[] names = prefs.getStringArray("lastEdited");
	    for (int i=0; i<names.length; i++) {
		fileToOpen = new File(names[i]);
		if (fileToOpen.exists()) {
		    //Util.pr("Opening last edited file:"
		    //+fileToOpen.getName());
		    openDatabaseAction.openIt();
		}
	    }
	}

	setVisible(true);
    }

    private void setupLayout() {
	fillMenu();
	createToolBar();
	getContentPane().setLayout(gbl);
	con.fill = GridBagConstraints.BOTH;
	con.anchor = GridBagConstraints.WEST;
	con.weightx = 1;
	con.weighty = 0;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(mb, con);
	getContentPane().add(mb);

	con.gridwidth = 1;
	gbl.setConstraints(tlb, con);
	getContentPane().add(tlb);

	con.anchor = GridBagConstraints.EAST;
	con.weightx = 0;
	JLabel lab = new JLabel(Globals.lang("Search")+":");
	con.fill = GridBagConstraints.NONE;
	con.anchor = GridBagConstraints.EAST;
	gbl.setConstraints(lab, con);
	getContentPane().add(lab);

	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(searchField, con);
	getContentPane().add(searchField);

	con.weightx = 1;
	con.weighty = 1;
	con.fill = GridBagConstraints.BOTH;
	con.anchor = GridBagConstraints.WEST;
	gbl.setConstraints(tabbedPane, con);
	getContentPane().add(tabbedPane);

	con.gridwidth = 1;
	con.weighty = 0;
	gbl.setConstraints(statusLabel, con);
	getContentPane().add(statusLabel);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(statusLine, con);
	getContentPane().add(statusLine);

    }

    
    private void initLabelMaker() {
	// initialize the labelMaker
	labelMaker = new LabelMaker() ; 
	labelMaker.addRule(new ArticleLabelRule(),
			   BibtexEntryType.ARTICLE); 
	labelMaker.addRule(new BookLabelRule(),
			   BibtexEntryType.BOOK); 
	labelMaker.addRule(new IncollectionLabelRule(),
			   BibtexEntryType.INCOLLECTION); 
	labelMaker.addRule(new InproceedingsLabelRule(),
			   BibtexEntryType.INPROCEEDINGS); 
    }

    /**
     * Returns the BasePanel at tab no. i
     */
    BasePanel baseAt(int i) {
	return (BasePanel)tabbedPane.getComponentAt(i);
    }

    /**
     * Returns the currently viewed BasePanel.
     */
    BasePanel basePanel() {
	return (BasePanel)tabbedPane.getSelectedComponent();
    }

    private int getTabIndex(JComponent comp) {
	for (int i=0; i<tabbedPane.getTabCount(); i++)
	    if (tabbedPane.getComponentAt(i) == comp)
		return i;
	return -1;
    }

    public String getTabTitle(JComponent comp) {
	return tabbedPane.getTitleAt(getTabIndex(comp));
    }

    public void setTabTitle(JComponent comp, String s) {
	tabbedPane.setTitleAt(getTabIndex(comp), s);
    }

    class GeneralAction extends AbstractAction {
	private String command;
	public GeneralAction(String command, String text,
			     String description, URL icon) {
	    super(Globals.lang(text), new ImageIcon(icon));
	    this.command = command;
	    putValue(SHORT_DESCRIPTION, Globals.lang(description));
	}
	public GeneralAction(String command, String text) {
	    super(Globals.lang(text));
	    this.command = command;
	}

	public void actionPerformed(ActionEvent e) {
	    if (tabbedPane.getTabCount() > 0)
		((BasePanel)(tabbedPane.getSelectedComponent()))
		    .runCommand(command);
	    else
		Util.pr("Action '"+command+"' must be disabled when no "
			+"database is open.");
	}
    }

    /** 
     * The action concerned with closing the window.
     */
    class CloseAction extends AbstractAction {
	public CloseAction() {
	    super(Globals.lang("Quit"));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Quit JabRef"));
	}    
	
	public void actionPerformed(ActionEvent e) {
	    // Ask here if the user really wants to close, if the base
	    // has not been saved since last save.
	    boolean close = true;	    
	    Vector filenames = new Vector();
	    if (tabbedPane.getTabCount() > 0) {
		for (int i=0; i<tabbedPane.getTabCount(); i++) {
		    if (baseAt(i).baseChanged) {
			tabbedPane.setSelectedIndex(i);
			int answer = JOptionPane.showConfirmDialog
			    (ths, Globals.lang
			     ("Database has changed. Do you "
			      +"want to save before closing?"),
			     Globals.lang("Save before closing"),
			     JOptionPane.YES_NO_CANCEL_OPTION);
			
			if ((answer == JOptionPane.CANCEL_OPTION) || 
			    (answer == JOptionPane.CLOSED_OPTION))
			    close = false; // The user has cancelled.
			if (answer == JOptionPane.YES_OPTION) {
			    // The user wants to save.
			    basePanel().runCommand("save");
			}
		    }
		    if (baseAt(i).file != null)
			filenames.add(baseAt(i).file.getPath());
		}
	    }
	    if (close) {
		dispose();

		

		prefs.putInt("posX", ths.getLocation().x);
		prefs.putInt("posY", ths.getLocation().y);
		prefs.putInt("sizeX", ths.getSize().width);
		prefs.putInt("sizeY", ths.getSize().height);
		
		if (prefs.getBoolean("openLastEdited")) {
		  // Here we store the names of allcurrent filea. If
		  // there is no current file, we remove any
		  // previously stored file name.
		    if (filenames.size() == 0)
			prefs.remove("lastEdited");
		    else {
			String[] names = new String[filenames.size()];
			for (int i=0; i<filenames.size(); i++)
			    names[i] = (String)filenames.elementAt(i);
			    
			prefs.putStringArray("lastEdited", names);
		    }
		}
		System.exit(0); // End program.
	    }
	}
    }

    private void setupDatabaseLayout() {
	// This method is called whenever this frame has been provided
	// with a database, and completes the layout.

	/*
	if (file != null)
	    setTitle(GUIGlobals.baseTitle+file.getName());
	else
	setTitle(GUIGlobals.untitledTitle);*/

	//DragNDropManager dndm = new DragNDropManager(this);

	//setNonEmptyState();	
	Util.pr("JabRefFrame: Must set non-empty state.");
    }

    private void fillMenu() {
	JMenu file = new JMenu(Globals.lang("File")),
	    edit = new JMenu(Globals.lang("Edit")),
	    bibtex = new JMenu(Globals.lang("Bibtex")),
	    view = new JMenu(Globals.lang("View")),
	    options = new JMenu(Globals.lang("Options"));
	file.add(newDatabaseAction);
	file.add(open);
	file.add(save);
	file.add(saveAs);
	file.addSeparator();
	file.add(close);
	mb.add(file);

	edit.add(undo);
	edit.add(redo);
	edit.addSeparator();
	edit.add(cut);
	edit.add(copy);
	edit.add(paste);
	//edit.add(remove);
	edit.add(selectAll);
	mb.add(edit);

	view.add(toggleGroups);
	mb.add(view);

	bibtex.add(editPreamble);
	bibtex.add(editStrings);
	mb.add(bibtex);

	options.add(showPrefs);
	mb.add(options);

	/*
	file.add(mItem(newDatabaseAction, null));
	file.add(mItem(openDatabaseAction, GUIGlobals.openKeyStroke));
	file.add(mItem(saveDatabaseAction, GUIGlobals.saveKeyStroke));
	file.add(mItem(saveAsDatabaseAction, null));
	file.add(mItem(saveSpecialAction, null));
	file.addSeparator();
	file.add(mItem(mergeDatabaseAction, null));
	file.addSeparator();
	file.add(mItem(closeDatabaseAction, null));
	file.add(mItem(closeAction, GUIGlobals.closeKeyStroke));
	mb.add(file);

	JMenu view = new JMenu("View"), 
	    entry = new JMenu("Edit"),
	    entryType = new JMenu("New ..."),
	    bibtex = new JMenu("Bibtex");
	for (int i=0; i<newSpecificEntryAction.length; i++)
	    entryType.add(mItem(newSpecificEntryAction[i], 
				newSpecificEntryAction[i].keyStroke));
	entry.add(mItem(undoAction, GUIGlobals.undoStroke));
	entry.add(mItem(redoAction, GUIGlobals.redoStroke));
	entry.addSeparator();
	entry.add(mItem(removeEntryAction, GUIGlobals.removeEntryKeyStroke));
	entry.add(mItem(copyAction, GUIGlobals.copyStroke));
	entry.add(mItem(pasteAction, GUIGlobals.pasteStroke));
	entry.addSeparator();
	entry.add(mItem(selectAllAction, GUIGlobals.selectAllKeyStroke));

	view.add(mItem(showGroupsAction, GUIGlobals.showGroupsKeyStroke));

	bibtex.add(entryType);
	bibtex.add(mItem(newEntryAction, GUIGlobals.newEntryKeyStroke));
	bibtex.addSeparator();
	bibtex.add(mItem(copyKeyAction, GUIGlobals.copyKeyStroke));
	bibtex.add(mItem(editPreambleAction, GUIGlobals.editPreambleKeyStroke));
	bibtex.add(mItem(editStringsAction, GUIGlobals.editStringsKeyStroke));
	bibtex.add(mItem(editEntryAction, GUIGlobals.editEntryKeyStroke));


	mb.add(entry);
	mb.add(view);
	mb.add(bibtex);

	JMenu tools = new JMenu("Tools");
	tools.add(mItem(searchPaneAction, GUIGlobals.simpleSearchKeyStroke));
	JMenu autoGenerateMenu = new JMenu("Autogenerate Bibtexkey") ; 
	tools.add(mItem(makeLabelAction, GUIGlobals.generateKeyStroke));
	tools.add(mItem(checkUniqueLabelAction, null));
	//tools.add(autoGenerateMenu) ; 
	mb.add(tools);

	JMenu options = new JMenu("Options");
	//options.add(mItem(setupTableAction, GUIGlobals.setupTableKeyStroke));
	options.add(setupTableAction);
	mb.add(options);

	JMenu help = new JMenu("Help");
	help.add(mItem(new HelpAction(helpDiag, GUIGlobals.baseFrameHelp, "Help"),
		       GUIGlobals.helpKeyStroke));
	help.add(new HelpAction("Help contents", helpDiag, 
				GUIGlobals.helpContents, "Help contents"));
	help.addSeparator();
	help.add(mItem(aboutAction, null));
	mb.add(help);

	return mb;
	*/
    }

    private void createToolBar() {
	tlb.setFloatable(false);
	tlb.add(newDatabaseAction);
	tlb.add(open);
	tlb.add(save);
	tlb.addSeparator();
	tlb.add(editPreamble);
	tlb.add(editStrings);
	//tb.add(closeDatabaseAction);
	//tb.addSeparator();
	/*tlb.add(copyKeyAction);
	tlb.add(makeLabelAction);
	tlb.addSeparator();
	tlb.add(editPreambleAction);
	tlb.add(editStringsAction);
	tlb.add(newEntryAction);
	tlb.add(editEntryAction);
	tlb.add(removeEntryAction);
	tlb.add(copyAction);
	tlb.add(pasteAction);
	tlb.add(searchPaneAction);
	tlb.addSeparator();
	tlb.add(setupTableAction);
	tlb.addSeparator();
	tlb.add(new HelpAction(helpDiag,GUIGlobals.baseFrameHelp, "Help"));
	*/
    }
	


    private JMenuItem mItem(AbstractAction a, KeyStroke ks) {
	// Set up a menu item with action and accelerator key.
	JMenuItem mi = new JMenuItem();
	mi.setAction(a);
	if (ks != null)
	    mi.setAccelerator(ks);
	return mi;
    }

    //private void setupMainPanel() {


    /*public Completer getAutoCompleter(String field) {
	return (Completer)autoCompleters.get(field);
	}

    
    public void assignAutoCompleters() {
	// Set up which fields should have autocompletion. This should
	// probably be made customizable. Existing Completer objects are
	// forgotten. The completers must be updated towards the database.
	byte[] fields = prefs.getByteArray("autoCompFields");
	autoCompleters = new Hashtable();
	for (int i=0; i<fields.length; i++) {
	    autoCompleters.put(GUIGlobals.ALL_FIELDS[fields[i]], new Completer());
	}
	
    }   
    
    public void updateAutoCompleters() {
	if (database != null)
	    database.setCompleters(autoCompleters);
	    }*/



    public void output(String s) {
	statusLine.setText(s);
    }

    protected ParserResult loadDatabase(File fileToOpen) throws IOException {
	// Temporary (old method):	
	//FileLoader fl = new FileLoader();
	//BibtexDatabase db = fl.load(fileToOpen.getPath());

       	BibtexParser bp = new BibtexParser(new FileReader(fileToOpen));	
	ParserResult pr = bp.parse();
	return pr;
    }

    // The action for closing the current database and leaving the window open.
    CloseDatabaseAction closeDatabaseAction = new CloseDatabaseAction();
    class CloseDatabaseAction extends AbstractAction {
	public CloseDatabaseAction() {
	    super(Globals.lang("Close database")); 

	    putValue(SHORT_DESCRIPTION, 
		     Globals.lang("Close the current database"));
	}    
	public void actionPerformed(ActionEvent e) {
	    // Ask here if the user really wants to close, if the base
	    // has not been saved since last save.
	    boolean close = true;	    
	    if (basePanel().baseChanged) {
		int answer = JOptionPane.showConfirmDialog
		    (ths, Globals.lang("Database has changed. Do you want to save "+
				       "before closing?"), 
		     Globals.lang("Save before closing"), 
		     JOptionPane.YES_NO_CANCEL_OPTION);
		if ((answer == JOptionPane.CANCEL_OPTION) || 
		    (answer == JOptionPane.CLOSED_OPTION))
		    close = false; // The user has cancelled.
		if (answer == JOptionPane.YES_OPTION) {
		    // The user wants to save.
		    basePanel().runCommand("save");
		}
	    }

	    if (close) {
		tabbedPane.remove(basePanel());
		output("Closed database.");
	    }
	}
    }



    // The action concerned with opening an existing database.
    OpenDatabaseAction openDatabaseAction = new OpenDatabaseAction();
    class OpenDatabaseAction extends AbstractAction {
	public OpenDatabaseAction() {
	    super(Globals.lang("Open database"), 
		  new ImageIcon(GUIGlobals.openIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Open BibTeX database"));
	}    
	public void actionPerformed(ActionEvent e) {
	    // Open a new database.
	    if ((e.getActionCommand() == null) || 
		(e.getActionCommand().equals("Open database"))) {
		JFileChooser chooser = (prefs.get("workingDirectory") == null) ?
		    new JFileChooser((File)null) :
		    new JFileChooser(new File(prefs.get("workingDirectory")));
		//chooser.setFileFilter(fileFilter);
		Util.pr("JabRefFrame: must set file filter.");
		int returnVal = chooser.showOpenDialog(ths);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
		    fileToOpen = chooser.getSelectedFile();
		}
	    } else {
		Util.pr(NAME);
		Util.pr(e.getActionCommand());
		fileToOpen = new File(Util.checkName(e.getActionCommand()));
	    }
	    openIt();
	}
	 
	public void openIt() {
	    if ((fileToOpen != null) && (fileToOpen.exists())) {
		try {
		    prefs.put("workingDirectory", fileToOpen.getPath());
		    // Should this be done _after_ we know it was successfully opened?

		    ParserResult pr = loadDatabase(fileToOpen);
		    BibtexDatabase db = pr.getDatabase();
		    HashMap meta = pr.getMetaData();

		    BasePanel bp = new BasePanel(ths, db, fileToOpen,
						 meta, prefs);
		    /*
		      if (prefs.getBoolean("autoComplete")) {
		      db.setCompleters(autoCompleters);
		      }
		    */
		    tabbedPane.add(fileToOpen.getName(), bp);
		    tabbedPane.setSelectedComponent(bp);
		    output("Opened database '"+fileToOpen.getPath()+"' with "+
			   db.getEntryCount()+" entries.");

		    fileToOpen = null;

		} catch (Throwable ex) {
		    JOptionPane.showMessageDialog
			(ths, ex.getMessage(), 
			 "Open database", JOptionPane.ERROR_MESSAGE);
		}
	    }
	}
    }

    // The action concerned with opening a new database.
    class NewDatabaseAction extends AbstractAction {
	public NewDatabaseAction() {
	    super(Globals.lang("New database"), 
		  new ImageIcon(GUIGlobals.newIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("New BibTeX database"));
	    //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);	    
	}    
	public void actionPerformed(ActionEvent e) {

	    // Create a new, empty, database.
	    BasePanel bp = new BasePanel(ths, prefs);
	    tabbedPane.add(Globals.lang("untitled"), bp);
	    tabbedPane.setSelectedComponent(bp);
	    output(Globals.lang("New database created."));
	    
	    /*
	    if (prefs.getBoolean("autoComplete"))
	    db.setCompleters(autoCompleters);*/
	}
    }


    // The action for opening the preferences dialog.
    AbstractAction showPrefs = new ShowPrefsAction();
    class ShowPrefsAction extends AbstractAction {
	public ShowPrefsAction() {
	    super(Globals.lang("Preferences"), 
		  new ImageIcon(GUIGlobals.prefsIconFile));
	    putValue(SHORT_DESCRIPTION, Globals.lang("Preferences"));
	}    
	public void actionPerformed(ActionEvent e) {	    
		PrefsDialog.showPrefsDialog(ths, prefs);
		// This action can be invoked without an open database, so
		// we have to check if we have one before trying to invoke
		// methods to execute changes in the preferences.

		// We want to notify all tabs about the changes to 
		// avoid problems when changing the column set.	       
		for (int i=0; i<tabbedPane.getTabCount(); i++) {
		    BasePanel bf = baseAt(i);
		    if (bf.database != null) {
			bf.setupMainPanel();
		    }
		}
	    
	}
    }


    AboutAction aboutAction = new AboutAction();
    class AboutAction extends AbstractAction {
	public AboutAction() {
	    super(Globals.lang("About JabRef"));

	}    
	public void actionPerformed(ActionEvent e) {
	    JDialog about = new JDialog(ths, Globals.lang("About JabRef"),
					true);
	    JEditorPane jp = new JEditorPane();
	    JScrollPane sp = new JScrollPane
		(jp, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    jp.setEditable(false);
	    try {
		jp.setPage(GUIGlobals.aboutPage);
		// We need a hyperlink listener to be able to switch to the license
		// terms and back.
		jp.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
			public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent e) {    
			    if (e.getEventType() 
				== javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) 
				try {
				    ((JEditorPane)e.getSource()).setPage(e.getURL());
				} catch (IOException ex) {}
			}
		    });
		about.getContentPane().add(sp);
		about.setSize(GUIGlobals.aboutSize);
		Util.placeDialog(about, ths);
		about.setVisible(true);
	    } catch (IOException ex) {
		JOptionPane.showMessageDialog(ths, "Could not load file 'About.html'", "Error", JOptionPane.ERROR_MESSAGE); 
	    }

	}
    }


}
