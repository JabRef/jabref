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
http://www.gnu.org/copyleft/gpl.ja.html0

*/
package net.sf.jabref;

import net.sf.jabref.label.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import java.net.URL;
import java.util.regex.Matcher;

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

    JLabel statusLine = new JLabel("", SwingConstants.LEFT),
	statusLabel = new JLabel(Globals.lang("Status")+":",
				 SwingConstants.LEFT);
    SearchManager searchManager  = new SearchManager(ths, prefs);
    FileHistory fileHistory = new FileHistory(prefs, this);

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
	quit = new CloseAction(),
	selectKeys = new SelectKeysAction(),
	newDatabaseAction = new NewDatabaseAction(),
	save = new GeneralAction("save", "Save database",
				 "Save database", GUIGlobals.saveIconFile,
				 prefs.getKey("Save")),
	saveAs = new GeneralAction("saveAs", "Save database as ...",
				 "Save database as ...", 
				   GUIGlobals.saveAsIconFile),
	undo = new GeneralAction("undo", "Undo", "Undo",
				 GUIGlobals.undoIconFile, 
				 prefs.getKey("Undo")),
	redo = new GeneralAction("redo", "Redo", "Redo",
				 GUIGlobals.redoIconFile,
				 prefs.getKey("Redo")),
	cut = new GeneralAction("cut", "Cut", "Cut",
				GUIGlobals.cutIconFile,
				prefs.getKey("Cut")),
	copy = new GeneralAction("copy", "Copy", "Copy",
				 GUIGlobals.copyIconFile,
				 prefs.getKey("Copy")),
	paste = new GeneralAction("paste", "Paste", "Paste",
				 GUIGlobals.pasteIconFile,
				  prefs.getKey("Paste")),

	/*remove = new GeneralAction("remove", "Remove", "Remove selected entries",
	  GUIGlobals.removeIconFile),*/
	selectAll = new GeneralAction("selectAll", "Select all",
				      prefs.getKey("Select all")),
	editPreamble = new GeneralAction("editPreamble", "Edit preamble", 
					 "Edit preamble",
					 GUIGlobals.preambleIconFile,
					 prefs.getKey("Edit preamble")),
	editStrings = new GeneralAction("editStrings", "Edit strings", 
					"Edit strings",
					GUIGlobals.stringsIconFile,
					prefs.getKey("Edit strings")),
	toggleGroups = new GeneralAction("toggleGroups", "Toggle groups interface", 
					 "Toggle groups interface",
					 GUIGlobals.groupsIconFile,
					 prefs.getKey("Toggle groups")),
	makeKeyAction = new GeneralAction("makeKey", "Autogenerate BibTeX keys",
					  "Autogenerate BibTeX keys",
					  GUIGlobals.genKeyIconFile,
					  prefs.getKey("Autgenerate BibTeX keys"));


    // The action for adding a new entry of unspecified type.
    NewEntryAction newEntryAction = new NewEntryAction(prefs.getKey("New entry"));
    NewEntryAction[] newSpecificEntryAction = new NewEntryAction[] {
	new NewEntryAction(BibtexEntryType.ARTICLE, prefs.getKey("New article")),
	new NewEntryAction(BibtexEntryType.BOOK, prefs.getKey("New book")),
	new NewEntryAction(BibtexEntryType.PHDTHESIS, prefs.getKey("New phdthesis")),
	new NewEntryAction(BibtexEntryType.INBOOK, prefs.getKey("New inbook")),
	new NewEntryAction(BibtexEntryType.MASTERSTHESIS, prefs.getKey("New mastersthesis")),
	new NewEntryAction(BibtexEntryType.PROCEEDINGS, prefs.getKey("New proceedings")),
	new NewEntryAction(BibtexEntryType.INPROCEEDINGS),
	new NewEntryAction(BibtexEntryType.INCOLLECTION),
	new NewEntryAction(BibtexEntryType.BOOKLET), 
	new NewEntryAction(BibtexEntryType.MANUAL),
	new NewEntryAction(BibtexEntryType.TECHREPORT),
	new NewEntryAction(BibtexEntryType.UNPUBLISHED,
			   prefs.getKey("New unpublished")),
	new NewEntryAction(BibtexEntryType.MISC) 
    };

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
	    if (tabbedPane.getTabCount() > 0)
		tabbedPane.setSelectedIndex(0);
	}

	setVisible(true);
    }

    private void setupLayout() {
	fillMenu();
	createToolBar();
	getContentPane().setLayout(gbl);
	con.fill = GridBagConstraints.HORIZONTAL;
	con.anchor = GridBagConstraints.WEST;
	con.weightx = 1;
	con.weighty = 0;
	con.gridwidth = GridBagConstraints.REMAINDER;
	mb.setMinimumSize(mb.getPreferredSize());
	gbl.setConstraints(mb, con);
	getContentPane().add(mb);

	con.gridwidth = 1;
	gbl.setConstraints(tlb, con);
	getContentPane().add(tlb);

	JPanel empt = new JPanel();
	gbl.setConstraints(empt, con);
        getContentPane().add(empt); 

	con.anchor = GridBagConstraints.EAST;
	con.weightx = 0;      
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(searchManager, con);
	getContentPane().add(searchManager);

	con.weightx = 1;
	con.weighty = 1;
	con.fill = GridBagConstraints.BOTH;
	con.anchor = GridBagConstraints.WEST;
	gbl.setConstraints(tabbedPane, con);
	getContentPane().add(tabbedPane);

	JPanel status = new JPanel();
	status.setLayout(gbl);
	con.weighty = 0;
	con.weightx = 0;
	con.gridwidth = 0;
	status.add(statusLabel);
	con.weightx = 1;
	con.insets = new Insets(0, 4, 0, 0);
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(statusLine, con);
	status.add(statusLine);
	con.gridwidth = GridBagConstraints.REMAINDER;
	statusLabel.setForeground(GUIGlobals.nullFieldColor.darker());
	gbl.setConstraints(status, con);
	getContentPane().add(status);

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
	public GeneralAction(String command, String text,
			     String description, URL icon,
			     KeyStroke key) {
	    super(Globals.lang(text), new ImageIcon(icon));
	    this.command = command;
	    putValue(ACCELERATOR_KEY, key);
	    putValue(SHORT_DESCRIPTION, Globals.lang(description));
	}
	public GeneralAction(String command, String text) {
	    super(Globals.lang(text));
	    this.command = command;
	}
	public GeneralAction(String command, String text, KeyStroke key) {
	    super(Globals.lang(text));
	    this.command = command;
	    putValue(ACCELERATOR_KEY, key);
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

    class NewEntryAction extends AbstractAction {

	BibtexEntryType type = null; // The type of item to create.
	KeyStroke keyStroke = null;  // Used for the specific instances.

	public NewEntryAction(KeyStroke key) {
	    // This action leads to a dialog asking for entry type.
	    super(Globals.lang("New entry"),  
		  new ImageIcon(GUIGlobals.addIconFile));
	    putValue(ACCELERATOR_KEY, key);
	    putValue(SHORT_DESCRIPTION, Globals.lang("New BibTeX entry"));
	}    

	public NewEntryAction(BibtexEntryType type_) { 
	    // This action leads to the creation of a specific entry.
	    super(type_.getName());
	    type = type_;
	}    

	public NewEntryAction(BibtexEntryType type_, KeyStroke key) { 
	    // This action leads to the creation of a specific entry.
	    super(type_.getName());
	    putValue(ACCELERATOR_KEY, key);
	    type = type_;
	}    

	public void actionPerformed(ActionEvent e) {
	    if (tabbedPane.getTabCount() > 0)
		((BasePanel)(tabbedPane.getSelectedComponent()))
		    .newEntry(type);
	    else
		Util.pr("Action 'New entry' must be disabled when no "
			+"database is open.");	    
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
	    bibtex = new JMenu(Globals.lang("BibTeX")),
	    view = new JMenu(Globals.lang("View")),
	    tools = new JMenu(Globals.lang("Tools")),
	    options = new JMenu(Globals.lang("Options")),
	    newSpec = new JMenu(Globals.lang("New entry..."));
	JMenu importMenu = new JMenu(Globals.lang("Import"));

	setUpImportMenu(importMenu);
	
	file.add(newDatabaseAction);
	file.add(open);//opendatabaseaction
	file.add(importMenu);
	file.add(save);
	file.add(saveAs);
	file.add(fileHistory);
	file.addSeparator();
	file.add(close);
	//==============================
	// NB: I added this because my frame borders are so tiny that I cannot click
	// on the "x" close button. Anyways, I think it is good to have and "exit" button
	// I was too lazy to make a new ExitAction
	//JMenuItem exit_mItem = new JMenuItem(Globals.lang("Exit"));
	//exit_mItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)); //Ctrl-Q to exit
	// above keybinding should be from user define
	//exit_mItem.addActionListener(new CloseAction() );
	//file.add( exit_mItem);
	//=====================================
	file.add(quit);
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

	bibtex.add(newEntryAction);
	for (int i=0; i<newSpecificEntryAction.length; i++)
	    newSpec.add(newSpecificEntryAction[i]);
	bibtex.add(newSpec);
	bibtex.addSeparator();
	bibtex.add(editPreamble);
	bibtex.add(editStrings);
	mb.add(bibtex);

	tools.add(makeKeyAction);
	mb.add(tools);

	options.add(showPrefs);
	options.add(selectKeys);
	mb.add(options);
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

    public void stopShowingSearchResults() {
	for (int i=0; i<tabbedPane.getTabCount(); i++)
	    baseAt(i).stopShowingSearchResults();
    }

    protected ParserResult loadDatabase(File fileToOpen) throws IOException {
	// Temporary (old method):	
	//FileLoader fl = new FileLoader();
	//BibtexDatabase db = fl.load(fileToOpen.getPath());

       	BibtexParser bp = new BibtexParser(new FileReader(fileToOpen));	
	ParserResult pr = bp.parse();
	return pr;
    }

    class SelectKeysAction extends AbstractAction {
	public SelectKeysAction() {
	    super(Globals.lang("Customize key bindings"));	    
	}
	public void actionPerformed(ActionEvent e) {
	    KeyBindingsDialog d= new KeyBindingsDialog
		((HashMap)prefs.getKeyBindings().clone());
	    d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    d.setSize(300,500);
	    Util.placeDialog(d, ths);
	    d.setVisible(true);
	    if (d.getAction()) {
		prefs.setNewKeyBindings(d.getNewKeyBindings());
		JOptionPane.showMessageDialog
		    (ths, 
		     Globals.lang("Your new key bindings have been stored.\n"
				  +"You must restart JabRef for the new key "
				  +"bindings to work properly."),
		     Globals.lang("Key bindings changed"),
		     JOptionPane.INFORMATION_MESSAGE);
	    }
	}
    }

    /** 
     * The action concerned with closing the window.
     */
    class CloseAction extends AbstractAction {
		public CloseAction() {
			super(Globals.lang("Quit"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Quit JabRef"));
			putValue(ACCELERATOR_KEY, prefs.getKey("Quit JabRef"));
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

					fileHistory.storeHistory();
				}
				
				// Let the search interface store changes to prefs.
				searchManager.updatePrefs();
				
				System.exit(0); // End program.
			}
		}
    }
	
    // The action for closing the current database and leaving the window open.
    CloseDatabaseAction closeDatabaseAction = new CloseDatabaseAction();
    class CloseDatabaseAction extends AbstractAction {
		public CloseDatabaseAction() {
			super(Globals.lang("Close database")); 

			putValue(SHORT_DESCRIPTION, 
					 Globals.lang("Close the current database"));
			putValue(ACCELERATOR_KEY, prefs.getKey("Close database"));			
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
	    putValue(ACCELERATOR_KEY, prefs.getKey("Open"));
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
		chooser.addChoosableFileFilter( new OpenFileFilter() );//nb nov2
		
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
	    
	    // Run the actual open in a thread to prevent the program
	    // locking until the file is loaded.
	    if (fileToOpen != null) {
		(new Thread() {
			public void run() {
			    openIt();
			}
		    }).start();
		fileHistory.newFile(fileToOpen.getPath());
	    }
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
		
		private void addBibEntries(ArrayList bibentries, String filename){
			// check if bibentries is null
			BibtexDatabase database=new BibtexDatabase();
			
			Iterator it = bibentries.iterator();
			while(it.hasNext()){
				BibtexEntry entry = (BibtexEntry)it.next();
				
				try {
					entry.setId(Util.createId(entry.getType(), database));				
					database.insertEntry(entry);			
				} catch (KeyCollisionException ex) {
					//ignore
					System.err.println("KeyCollisionException [ addBibEntries(...) ]");
					
				}
				
			}
			HashMap meta = new HashMap();		    
			// Metadata are only put in bibtex files, so we will not find it
			// in imported files. Instead we pass an empty HashMap.	
			BasePanel bp = new BasePanel(ths, database, null,
						     meta, prefs);
			/*
			  if (prefs.getBoolean("autoComplete")) {
			  db.setCompleters(autoCompleters);
			  }
			*/
			tabbedPane.add(Globals.lang("untitled"), bp);
			tabbedPane.setSelectedComponent(bp);
			output("Imported database '"+filename+"' with "+
			       database.getEntryCount()+" entries.");
		}
	

	private void setUpImportMenu(JMenu importMenu){
			//
			// put in menu
			//
			//========================================
			// medline
			//========================================
			JMenuItem newMedlineFile_mItem = new JMenuItem(Globals.lang("Medline XML"));//,						       new ImageIcon(getClass().getResource("images16/Open16.gif")));
			newMedlineFile_mItem.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						String tempFilename= getNewFile();
						if(tempFilename != null){
							ArrayList bibs = ImportFormatReader.readMedline(tempFilename);//MedlineParser.readMedline(tempFilename);
							addBibEntries( bibs, tempFilename);	
						}
					}
				});
			importMenu.add(newMedlineFile_mItem);
			
			JMenuItem newRefMan_mItem = new JMenuItem(Globals.lang("Import RIS"));//, new ImageIcon(getClass().getResource("images16/Open16.gif")));
			newRefMan_mItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String tempFilename=getNewFile();
						if(tempFilename != null){
							ArrayList bibs = ImportFormatReader.readReferenceManager10(tempFilename);
							addBibEntries( bibs, tempFilename);
						}
						
					}
				});
			importMenu.add(newRefMan_mItem);
			
			JMenuItem newISIFile_mItem = new JMenuItem(Globals.lang("Import ISI"));//, new ImageIcon(getClass().getResource("images16/Open16.gif")));
			newISIFile_mItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String tempFilename=getNewFile();
						if(tempFilename != null){
							ArrayList bibs = ImportFormatReader.readISI(tempFilename);
							addBibEntries( bibs, tempFilename);
						}
						
					}
				});
			importMenu.add( newISIFile_mItem);
			
			JMenuItem newOvidFile_mItem = new JMenuItem(Globals.lang("Import Ovid"));//,new ImageIcon(getClass().getResource("images16/Open16.gif")));
			newOvidFile_mItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String tempFilename=getNewFile();
						if(tempFilename != null){
							ArrayList bibs = ImportFormatReader.readOvid(tempFilename);
							addBibEntries( bibs, tempFilename);
						}
						
					}
				});
			importMenu.add(newOvidFile_mItem);
			
			JMenuItem newINSPECFile_mItem = new JMenuItem(Globals.lang("Import INSPEC"));//, new ImageIcon(getClass().getResource("images16/Open16.gif")));
			newINSPECFile_mItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String tempFilename=getNewFile();
						if(tempFilename != null){
							ArrayList bibs = ImportFormatReader.readINSPEC(tempFilename);
							addBibEntries( bibs, tempFilename);
						}
						
					}
				});
			importMenu.add(newINSPECFile_mItem);
			
			JMenuItem newSciFinderFile_mItem = new JMenuItem(Globals.lang("Import SciFinder"));//,new ImageIcon(getClass().getResource("images16/Open16.gif")));
			//newSciFinderFile_mItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)); //Ctrl-F for new file
			newSciFinderFile_mItem.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e)
					{
						String tempFilename = getNewFile();
						if( tempFilename != null )//filenm != null)
						{
							//ArrayList bibs = Scifinder2bibtex.readSciFinderFile( tempFilename);//filename);//filenm );
							ArrayList bibs=ImportFormatReader.readScifinder(tempFilename);
							addBibEntries( bibs, tempFilename);
						}
					}
				});
			importMenu.add(newSciFinderFile_mItem);
			
		}
		
		//
		// simply opens up a jfilechooser dialog and gets a filename
		// returns null if user selects cancel
		// it should also do a check perhaps to see if
		// file exists and is readable?
		//
		
		public String getNewFile(){
			JFileChooser fc;
			if( prefs.get("workingDirectory")== null )
				fc = new JabRefFileChooser(new File( System.getProperty("user.home")));//cwd));
			else{
				fc = new JabRefFileChooser(new File( prefs.get("workingDirectory") ));//cwd));
			}
			
			fc.addChoosableFileFilter( new OpenFileFilter() );
			fc.setDialogType(JFileChooser.OPEN_DIALOG);
			fc.showOpenDialog(null);
			File selectedFile = fc.getSelectedFile();
			if(selectedFile == null) // cancel
				return null;
			prefs.put("workingDirectory", selectedFile.getPath());		
			return selectedFile.getAbsolutePath();
		}
		
	}
	
