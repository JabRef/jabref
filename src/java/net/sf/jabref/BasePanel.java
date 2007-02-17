/*
Copyright (C) 2003 Morten O. Alver and Nizar N. Batada

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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sf.jabref.collab.ChangeScanner;
import net.sf.jabref.collab.FileUpdateListener;
import net.sf.jabref.collab.FileUpdatePanel;
import net.sf.jabref.export.*;
import net.sf.jabref.external.AutoSetExternalFileForEntries;
import net.sf.jabref.external.WriteXMPAction;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.gui.*;
import net.sf.jabref.imports.AppendDatabaseAction;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.journals.AbbreviateAction;
import net.sf.jabref.journals.UnabbreviateAction;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.search.NoSearchMatcher;
import net.sf.jabref.search.SearchMatcher;
import net.sf.jabref.undo.CountingUndoManager;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableChangeType;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableKeyChange;
import net.sf.jabref.undo.UndoableRemoveEntry;
import net.sf.jabref.wizard.text.gui.TextInputDialog;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.component.UIFSplitPane;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;

public class BasePanel extends JPanel implements ClipboardOwner, FileUpdateListener {

    public final static int SHOWING_NOTHING=0, SHOWING_PREVIEW=1, SHOWING_EDITOR=2, WILL_SHOW_EDITOR=3;
    private int mode=0;
    private EntryEditor currentEditor = null;
    private PreviewPanel currentPreview = null;

    boolean tmp = true;

    private MainTableSelectionListener selectionListener = null;
    private ListEventListener groupsHighlightListener;
    UIFSplitPane contentPane = new UIFSplitPane();

    JSplitPane splitPane;
    //BibtexEntry testE = new BibtexEntry("tt");
    //boolean previewActive = true;

    JabRefFrame frame;
    BibtexDatabase database;
    // The database shown in this panel.
    
    //  	Moving file to MetaData (Morten, 2006.08.29)
    // private File fileToOpen = null; 
    
    String fileMonitorHandle = null;
    boolean saving = false, updatedExternally = false;
    private String encoding;

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();

    HashMap autoCompleters = new HashMap();
    // Hashtable that holds as keys the names of the fields where
    // autocomplete is active, and references to the autocompleter objects.

    // The undo manager.
    public CountingUndoManager undoManager = new CountingUndoManager(this);
    UndoAction undoAction = new UndoAction();
    RedoAction redoAction = new RedoAction();

    //ExampleFileFilter fileFilter;
    // File filter for .bib files.

    boolean baseChanged = false, nonUndoableChange = false;
    // Used to track whether the base has changed since last save.

    //EntryTableModel tableModel = null;
    //public EntryTable entryTable = null;
    public MainTable mainTable = null;
    public FilterList searchFilterList = null, groupFilterList = null;

    public RightClickMenu rcm;

    BibtexEntry showing = null;
    // To indicate which entry is currently shown.
    public HashMap entryEditors = new HashMap();
    // To contain instantiated entry editors. This is to save time
    // in switching between entries.

    //HashMap entryTypeForms = new HashMap();
    // Hashmap to keep track of which entries currently have open
    // EntryTypeForm dialogs.

    PreambleEditor preambleEditor = null;
    // Keeps track of the preamble dialog if it is open.

    StringDialog stringDialog = null;
    // Keeps track of the string dialog if it is open.

    /**
     * The group selector component for this database. Instantiated by the
     * SidePaneManager if necessary, or from this class if merging groups from a
     * different database.
     */
    //GroupSelector groupSelector;

    public boolean sortingBySearchResults = false,
        coloringBySearchResults = false,
    hidingNonHits = false,
        sortingByGroup = false,
        sortingByCiteSeerResults = false,
        coloringByGroup = false;
        //previewEnabled = Globals.prefs.getBoolean("previewEnabled");
    int lastSearchHits = -1; // The number of hits in the latest search.
    // Potential use in hiding non-hits completely.

    // MetaData parses, keeps and writes meta data.
    MetaData metaData;
    HashMap fieldExtras = new HashMap();
    //## keep track of all keys for duplicate key warning and unique key generation
    //private HashMap allKeys  = new HashMap();	// use a map instead of a set since i need to know how many of each key is inthere

    private boolean suppressOutput = false;

    private HashMap actions = new HashMap();
    private SidePaneManager sidePaneManager;

    /**
     * Create a new BasePanel with an empty database.
     * @param frame The application window.
     */
    public BasePanel(JabRefFrame frame) {
      this.sidePaneManager = Globals.sidePaneManager;
      database = new BibtexDatabase();
      metaData = new MetaData();
        metaData.initializeNewDatabase();
      this.frame = frame;
      setupActions();
      setupMainPanel();
        encoding = Globals.prefs.get("defaultEncoding");
        //System.out.println("Default: "+encoding);
    }

    public BasePanel(JabRefFrame frame, BibtexDatabase db, File file,
                     HashMap meta, String encoding) {

        this.encoding = encoding;
       // System.out.println(encoding);
     //super(JSplitPane.HORIZONTAL_SPLIT, true);
      this.sidePaneManager = Globals.sidePaneManager;
      this.frame = frame;
      database = db;
      if (meta != null)
        parseMetaData(meta);
      else {
        metaData = new MetaData();
        metaData.initializeNewDatabase();   
      }
      setupActions();
      setupMainPanel();

      metaData.setFile(file);

      // Register so we get notifications about outside changes to the file.
      if (file != null)
        try {
          fileMonitorHandle = Globals.fileUpdateMonitor.addUpdateListener(this,
              file);
        } catch (IOException ex) {
        }
    }

    public boolean isBaseChanged(){
    	return baseChanged;
    }
    
    public int getMode() {
        return mode;
    }

    public BibtexDatabase database() {
		return database;
	}

	public MetaData metaData() {
		return metaData;
	}

	public JabRefFrame frame() {
		return frame;
	}

	public JabRefPreferences prefs() {
		return Globals.prefs;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

    public void output(String s) {
    //Util.pr("\""+s+"\""+(SwingUtilities.isEventDispatchThread()));
        if (!suppressOutput)
            frame.output(s);
    }

    private void setupActions() {
        
        actions.put("undo", undoAction);
        actions.put("redo", redoAction);

        // The action for opening an entry editor.
        actions.put("edit", new BaseAction() {
            public void action() {
                selectionListener.editSignalled();
            }
                /*
                  if (isShowingEditor()) {
                      new FocusRequester(splitPane.getBottomComponent());
                      return;
                  }

                  frame.block();
                //(new Thread() {
                //public void run() {
                int clickedOn = -1;
                // We demand that one and only one row is selected.
                if (entryTable.getSelectedRowCount() == 1) {
                  clickedOn = entryTable.getSelectedRow();
                }
                if (clickedOn >= 0) {
                  String id = tableModel.getIdForRow(clickedOn);
                  BibtexEntry be = database.getEntryById(id);
                  showEntry(be);

                  if (splitPane.getBottomComponent() != null) {
                      new FocusRequester(splitPane.getBottomComponent());
                  }

                }
        frame.unblock();
              }
                */
            });


        actions.put("test", new BaseAction () {
                public void action() throws Throwable {

                    ExportFormats.initAllExports();
                    JFileChooser fc = ExportFormats.createExportFileChooser("/home/alver/Documents");
                    fc.showSaveDialog(frame);
                    File file = fc.getSelectedFile();
                    if (file == null)
                        return;
                    FileFilter ff = fc.getFileFilter();
                    if (ff instanceof ExportFileFilter) {
                        ExportFormat format = ((ExportFileFilter)ff).getExportFormat();
                        format.performExport(database, file.getPath(), "UTF8", null);
                        // Make sure we remember which filter was used, to set the default
                        // for next time:
                        Globals.prefs.put("lastUsedExport", format.getConsoleName());

                    }

                }
            });


        // The action for saving a database.
        actions.put("save", new AbstractWorker() {
            private boolean success = false, cancelled = false;
            public void init() throws Throwable {
                success = false;
                cancelled = false;
                if (getFile() == null)
                    runCommand("saveAs");
                else {

                    if (updatedExternally || Globals.fileUpdateMonitor.hasBeenModified(fileMonitorHandle)) {
                        String[] opts = new String[]{Globals.lang("Review changes"), Globals.lang("Save"),
                                Globals.lang("Cancel")};
                        int answer = JOptionPane.showOptionDialog(frame, Globals.lang("File has been updated externally. "
                                + "What do you want to do?"), Globals.lang("File updated externally"),
                                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                                null, opts, opts[0]);
                        /*  int choice = JOptionPane.showConfirmDialog(frame, Globals.lang("File has been updated externally. "
+"Are you sure you want to save?"), Globals.lang("File updated externally"),
                       JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);*/

                        if (answer == JOptionPane.CANCEL_OPTION)
                            return;
                        else if (answer == JOptionPane.YES_OPTION) {
                            ChangeScanner scanner = new ChangeScanner(frame, BasePanel.this); //, panel.database(), panel.metaData());
                            //try {
                            scanner.changeScan(getFile());
                            setUpdatedExternally(false);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    sidePaneManager.hide("fileUpdate");
                                }
                            });

                            //} catch (IOException ex) {
                            //    ex.printStackTrace();
                            //}

                            return;
                        }
                    }

                    frame.output(Globals.lang("Saving database") + "...");
                    saving = true;
                }
            }

            public void update() {
                if (success) {
                	// Reset title of tab
                    frame.setTabTitle(BasePanel.this, getFile().getName(),
                            getFile().getAbsolutePath());
                    frame.output(Globals.lang("Saved database")+" '"
                             +getFile().getPath()+"'.");
                } else if (!cancelled) {
                    frame.output(Globals.lang("Save failed"));
                }
            }

            public void run() {
                if (getFile() == null) {
                    cancelled = true;
                    return;
                }

                try {
                    // If the option is set, autogenerate keys for all entries that are
                    // lacking keys, before saving:
                    autoGenerateKeysBeforeSaving();
                    
                    // Now save the database:
                    success = saveDatabase(getFile(), false, encoding);

                    //Util.pr("Testing resolve string... BasePanel line 237");
                    //Util.pr("Resolve aq: "+database.resolveString("aq"));
                    //Util.pr("Resolve text: "+database.resolveForStrings("A text which refers to the string #aq# and #billball#, hurra."));

                    try {
                        Globals.fileUpdateMonitor.updateTimeStamp(fileMonitorHandle);
                    } catch (IllegalArgumentException ex) {
                        // This means the file has not yet been registered, which is the case
                        // when doing a "Save as". Maybe we should change the monitor so no
                        // exception is cast.
                    }
                    saving = false;
                    if (success) {
                        undoManager.markUnchanged();
                        // (Only) after a successful save the following
                        // statement marks that the base is unchanged
                        // since last save:
                        nonUndoableChange = false;
                        baseChanged = false;
                        updatedExternally = false;
                    }
                } catch (SaveException ex2) {
                    ex2.printStackTrace();
                }
            }
        });

        actions.put("saveAs", new BaseAction() {
            public void action() throws Throwable {
                
                JPanel options = new JPanel();
                DefaultFormBuilder builder = new DefaultFormBuilder(options, new FormLayout("left:pref", "pref, pref, pref"));
                //options.setLayout(new GridLayout(2,1));
                ButtonGroup bg = new ButtonGroup();
                JRadioButton sAll = new JRadioButton("<html>All entries</html>");
                JRadioButton sSel = new JRadioButton("<html>Selected<br>entries</html>");
                sAll.setSelected(true);
                bg.add(sAll);
                bg.add(sSel);
                builder.append(Globals.lang("Include")+":");
                builder.append(sAll);
                builder.append(sSel);
                builder.appendGlueRow();
                options.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
                String chosenFile = Globals.getNewFile(frame, new File(Globals.prefs.get("workingDirectory")), ".bib",
                            JFileChooser.SAVE_DIALOG, false, options);
                if (chosenFile == null)
                    return; // cancelled
                Globals.prefs.put("workingDirectory", metaData.getFile().getParent());
                // Check if the file already exists:
                if ((new File(chosenFile)).exists() && (JOptionPane.showConfirmDialog
                                (frame, "'"+metaData.getFile().getName()+"' "+Globals.lang("exists. Overwrite file?"),
                                Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                                != JOptionPane.OK_OPTION)) {
                    return; // cancelled
                    
                }
                // Save:
                if (sAll.isSelected()) {
                    //
                    // Normal save
                    //
                    if (chosenFile != null) {
                        metaData.setFile(new File(chosenFile));
                        runCommand("save");
                        // Register so we get notifications about outside changes to the file.
                        try {
                            fileMonitorHandle = Globals.fileUpdateMonitor.addUpdateListener(BasePanel.this,getFile());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        frame.getFileHistory().newFile(metaData.getFile().getPath());
                    }
                }
                else {
                    //
                    // Save selected entries
                    //
                    File expFile = new File(chosenFile);
                    saveDatabase(expFile, true, encoding);
                    frame.getFileHistory().newFile(expFile.getPath());
                    frame.output(Globals.lang("Saved selected to")+" '"
                                 +expFile.getPath()+"'.");
                }
            }
        });

        actions.put("saveSelectedAs", new BaseAction () {
                public void action() throws Throwable {

                  String chosenFile = Globals.getNewFile(frame, new File(Globals.prefs.get("workingDirectory")), ".bib",
                                                         JFileChooser.SAVE_DIALOG, false);
                  if (chosenFile != null) {
                    File expFile = new File(chosenFile);
                    if (!expFile.exists() ||
                        (JOptionPane.showConfirmDialog
                         (frame, "'"+expFile.getName()+"' "+
                          Globals.lang("exists. Overwrite file?"),
                          Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                         == JOptionPane.OK_OPTION)) {
                      saveDatabase(expFile, true, Globals.prefs.get("defaultEncoding"));
                      //runCommand("save");
                      frame.getFileHistory().newFile(expFile.getPath());
                      frame.output(Globals.lang("Saved selected to")+" '"
                                   +expFile.getPath()+"'.");
                        }
                    }
                }
            });
    
        // The action for copying selected entries.
        actions.put("copy", new BaseAction() {
                public void action() {
                    BibtexEntry[] bes = mainTable.getSelectedEntries();

                    if ((bes != null) && (bes.length > 0)) {
                        TransferableBibtexEntry trbe
                            = new TransferableBibtexEntry(bes);
                        // ! look at ClipBoardManager
                        Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(trbe, BasePanel.this);
                        output(Globals.lang("Copied")+" "+(bes.length>1 ? bes.length+" "
                                                           +Globals.lang("entries")
                                                           : "1 "+Globals.lang("entry")+"."));
                    } else {
                        // The user maybe selected a single cell.
                        int[] rows = mainTable.getSelectedRows(),
                            cols = mainTable.getSelectedColumns();
                        if ((cols.length == 1) && (rows.length == 1)) {
                            // Copy single value.
                            Object o = mainTable.getValueAt(rows[0], cols[0]);
                            if (o != null) {
                                StringSelection ss = new StringSelection(o.toString());
                                Toolkit.getDefaultToolkit().getSystemClipboard()
                                    .setContents(ss, BasePanel.this);

                                output(Globals.lang("Copied cell contents")+".");
                            }
                        }
                    }
                }
            });

        actions.put("cut", new BaseAction() {
                public void action() throws Throwable {
                    runCommand("copy");
                    BibtexEntry[] bes = mainTable.getSelectedEntries();
                    //int row0 = mainTable.getSelectedRow();
                    if ((bes != null) && (bes.length > 0)) {
                        // Create a CompoundEdit to make the action undoable.
                        NamedCompound ce = new NamedCompound
                        (Globals.lang(bes.length > 1 ? "cut entries" : "cut entry"));
                        // Loop through the array of entries, and delete them.
                        for (int i=0; i<bes.length; i++) {
                            database.removeEntry(bes[i].getId());
                            ensureNotShowing(bes[i]);
                            ce.addEdit(new UndoableRemoveEntry
                                       (database, bes[i], BasePanel.this));
                        }
                        //entryTable.clearSelection();
                        frame.output(Globals.lang("Cut_pr")+" "+
                                     (bes.length>1 ? bes.length
                                      +" "+ Globals.lang("entries")
                                      : Globals.lang("entry"))+".");
                        ce.end();
                        undoManager.addEdit(ce);
                        markBaseChanged();

                        // Reselect the entry in the first prev. selected position:
                        /*if (row0 >= entryTable.getRowCount())
                            row0 = entryTable.getRowCount()-1;
                        if (row0 >= 0)
                            entryTable.addRowSelectionInterval(row0, row0);*/
                    }
                }
            });

        actions.put("delete", new BaseAction() {
                public void action() {
                  boolean cancelled = false;
                  BibtexEntry[] bes = mainTable.getSelectedEntries();
                  int row0 = mainTable.getSelectedRow();
                  if ((bes != null) && (bes.length > 0)) {

                      boolean goOn = showDeleteConfirmationDialog(bes.length);
                      if (!goOn) {
                          return;
                      }
                      else {
                          // Create a CompoundEdit to make the action undoable.
                          NamedCompound ce = new NamedCompound
                              (Globals.lang(bes.length > 1 ? "delete entries" : "delete entry"));
                          // Loop through the array of entries, and delete them.
                          for (int i = 0; i < bes.length; i++) {
                              database.removeEntry(bes[i].getId());
                              ensureNotShowing(bes[i]);
                              ce.addEdit(new UndoableRemoveEntry(database, bes[i], BasePanel.this));
                          }
                          markBaseChanged();
                          frame.output(Globals.lang("Deleted") + " " +
                                       (bes.length > 1 ? bes.length
                                        + " " + Globals.lang("entries")
                                        : Globals.lang("entry")) + ".");
                          ce.end();
                          undoManager.addEdit(ce);
                          //entryTable.clearSelection();
                      }


                          // Reselect the entry in the first prev. selected position:
                          /*if (row0 >= entryTable.getRowCount())
                              row0 = entryTable.getRowCount()-1;
                          if (row0 >= 0) {
                             final int toSel = row0;
                            //
                              SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    entryTable.addRowSelectionInterval(toSel, toSel);
                                    //entryTable.ensureVisible(toSel);
                                }
                              });
                            */
                          }

                      }

            });

        // The action for pasting entries or cell contents.
        // Edited by Seb Wills <saw27@mrao.cam.ac.uk> on 14-Apr-04:
        //  - more robust detection of available content flavors (doesn't only look at first one offered)
        //  - support for parsing string-flavor clipboard contents which are bibtex entries.
        //    This allows you to (a) paste entire bibtex entries from a text editor, web browser, etc
        //                       (b) copy and paste entries between multiple instances of JabRef (since
        //         only the text representation seems to get as far as the X clipboard, at least on my system)
        actions.put("paste", new BaseAction() {
                public void action() {
                    // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
                    Transferable content = Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getContents(null);
                    if (content != null) {
                        BibtexEntry[] bes = null;
                        if (content.isDataFlavorSupported(TransferableBibtexEntry.entryFlavor)) {
                            // We have determined that the clipboard data is a set of entries.
                            try {
                                bes = (BibtexEntry[])(content.getTransferData(TransferableBibtexEntry.entryFlavor));

                            } catch (UnsupportedFlavorException ex) {
                                ex.printStackTrace();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                            // We have determined that no TransferableBibtexEntry is available, but
                            // there is a string, which we will handle according to context:
                            int[] rows = mainTable.getSelectedRows();
                                //cols = entryTable.getSelectedColumns();
                            //Util.pr(rows.length+" x "+cols.length);
                            /*if ((cols != null) && (cols.length == 1) && (cols[0] != 0)
                                && (rows != null) && (rows.length == 1)) {
                                // A single cell is highlighted, so paste the string straight into it without parsing
                                try {
                                    tableModel.setValueAt((String)(content.getTransferData(DataFlavor.stringFlavor)), rows[0], cols[0]);
                                    refreshTable();
                                    markBaseChanged();
                                    output("Pasted cell contents");
                                } catch (UnsupportedFlavorException ex) {
                                    ex.printStackTrace();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                } catch (IllegalArgumentException ex) {
                                    output("Can't paste.");
                                }
                            } else {*/
                              // no single cell is selected, so try parsing the clipboard contents as bibtex entries instead
                              try {
                                  BibtexParser bp = new BibtexParser
                                      (new java.io.StringReader( (String) (content.getTransferData(
                                      DataFlavor.stringFlavor))));
                                  BibtexDatabase db = bp.parse().getDatabase();
                                  Util.pr("Parsed " + db.getEntryCount() + " entries from clipboard text");
                                  if(db.getEntryCount()>0) {
                                      Set keySet = db.getKeySet();
                                      if (keySet != null) {
                                          // Copy references to the entries into a BibtexEntry array.
                                          // Could import directly from db, but going via bes allows re-use
                                          // of the same pasting code as used for TransferableBibtexEntries
                                          bes = new BibtexEntry[db.getEntryCount()];
                                          Iterator it = keySet.iterator();
                                          for (int i=0; it.hasNext();i++) {
                                              bes[i]=db.getEntryById((String) (it.next()));
                                          }
                                      }
                                  } /*else {
                                    String cont = (String)(content.getTransferData(DataFlavor.stringFlavor));
                                    Util.pr("----------------\n"+cont+"\n---------------------");
                                    TextAnalyzer ta = new TextAnalyzer(cont);
                                      output(Globals.lang("Unable to parse clipboard text as Bibtex entries."));
                                      }*/
                              } catch (UnsupportedFlavorException ex) {
                                  ex.printStackTrace();
                              } catch (Throwable ex) {
                                  ex.printStackTrace();
                              }

                        }

                        // finally we paste in the entries (if any), which either came from TransferableBibtexEntries
                        // or were parsed from a string
                        if ((bes != null) && (bes.length > 0)) {

                          NamedCompound ce = new NamedCompound
                              (Globals.lang(bes.length > 1 ? "paste entries" : "paste entry"));
                          for (int i=0; i<bes.length; i++) {
                            try {
                              BibtexEntry be = (BibtexEntry)(bes[i].clone());
                                Util.setAutomaticFields(be);

                              // We have to clone the
                              // entries, since the pasted
                              // entries must exist
                              // independently of the copied
                              // ones.
                              be.setId(Util.createNeutralId());
                              database.insertEntry(be);
                              ce.addEdit(new UndoableInsertEntry
                                         (database, be, BasePanel.this));
                            } catch (KeyCollisionException ex) {
                              Util.pr("KeyCollisionException... this shouldn't happen.");
                            }
                          }
                          ce.end();
                          undoManager.addEdit(ce);
                          //entryTable.clearSelection();
                          //entryTable.revalidate();
                          output(Globals.lang("Pasted")+" "+
                                 (bes.length>1 ? bes.length+" "+
                                  Globals.lang("entries") : "1 "+Globals.lang("entry"))
                                 +".");
                          markBaseChanged();
                        }
                      }

                    }

});

        actions.put("selectAll", new BaseAction() {
                public void action() {
                    mainTable.selectAll();
                }
            });

        // The action for opening the preamble editor
        actions.put("editPreamble", new BaseAction() {
                public void action() {
                    if (preambleEditor == null) {
                        PreambleEditor form = new PreambleEditor
                            (frame, BasePanel.this, database, Globals.prefs);
                        Util.placeDialog(form, frame);
                        form.setVisible(true);
                        preambleEditor = form;
                    } else {
                        preambleEditor.setVisible(true);
                    }

                }
            });

        // The action for opening the string editor
        actions.put("editStrings", new BaseAction() {
                public void action() {
                    if (stringDialog == null) {
                        StringDialog form = new StringDialog
                            (frame, BasePanel.this, database, Globals.prefs);
                        Util.placeDialog(form, frame);
                        form.setVisible(true);
                        stringDialog = form;
                    } else {
                        stringDialog.setVisible(true);
                    }

                }
            });

        // The action for toggling the groups interface
        actions.put("toggleGroups", new BaseAction() {
            public void action() {
              sidePaneManager.toggle("groups");
              frame.groupToggle.setSelected(sidePaneManager.isComponentVisible("groups"));
            }
        });


        // The action for auto-generating keys.
        actions.put("makeKey", new AbstractWorker() {
        //int[] rows;
        List entries;
        int numSelected;
        boolean cancelled = false;

        // Run first, in EDT:
        public void init() {

                    entries = new ArrayList(Arrays.asList(getSelectedEntries()));
                    //rows = entryTable.getSelectedRows() ;
                    numSelected = entries.size();

                    if (entries.size() == 0) { // None selected. Inform the user to select entries first.
                        JOptionPane.showMessageDialog(frame, Globals.lang("First select the entries you want keys to be generated for."),
                                                      Globals.lang("Autogenerate BibTeX key"), JOptionPane.INFORMATION_MESSAGE);
                        return ;
                    }
            frame.block();
            output(Globals.lang("Generating BibTeX key for")+" "+
                           numSelected+" "+(numSelected>1 ? Globals.lang("entries")
                                            : Globals.lang("entry"))+"...");
        }

        // Run second, on a different thread:
                public void run() {
                    BibtexEntry bes = null ;
                    NamedCompound ce = new NamedCompound(Globals.lang("autogenerate keys"));
                    //BibtexEntry be;
                    Object oldValue;
                    boolean hasShownWarning = false;
                    // First check if any entries have keys set already. If so, possibly remove
                    // them from consideration, or warn about overwriting keys.
                    loop: for (Iterator i=entries.iterator(); i.hasNext();) {
                        bes = (BibtexEntry)i.next();
                        if (bes.getField(BibtexFields.KEY_FIELD) != null) {
                            if (Globals.prefs.getBoolean("avoidOverwritingKey"))
                                // Rmove the entry, because its key is already set:
                                i.remove();
                            else if (Globals.prefs.getBoolean("warnBeforeOverwritingKey")) {
                                // Ask if the user wants to cancel the operation:
                                CheckBoxMessage cbm = new CheckBoxMessage(Globals.lang("One or more keys will be overwritten. Continue?"),
                                        Globals.lang("Disable this confirmation dialog"), false);
                                int answer = JOptionPane.showConfirmDialog(frame, cbm, Globals.lang("Overwrite keys"),
                                        JOptionPane.YES_NO_OPTION);
                                if (cbm.isSelected())
                                    Globals.prefs.putBoolean("warnBeforeOverwritingKey", false);
                                if (answer == JOptionPane.NO_OPTION) {
                                    // Ok, break off the operation.
                                    cancelled = true;
                                    return;
                                }
                                // No need to check more entries, because the user has already confirmed
                                // that it's ok to overwrite keys:
                                break loop;
                            }
                        }
                    }

                    HashMap oldvals = new HashMap();
                    // Iterate again, removing already set keys. This is skipped if overwriting
                    // is disabled, since all entries with keys set will have been removed.
                    if (!Globals.prefs.getBoolean("avoidOverwritingKey")) for (Iterator i=entries.iterator(); i.hasNext();) {
                        bes = (BibtexEntry)i.next();
                        // Store the old value:
                        oldvals.put(bes, bes.getField(BibtexFields.KEY_FIELD));
                        database.setCiteKeyForEntry(bes.getId(), null);
                    }

                    // Finally, set the new keys:
                    for (Iterator i=entries.iterator(); i.hasNext();) {
                        bes = (BibtexEntry)i.next();
                        bes = LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), database, bes);
                        ce.addEdit(new UndoableKeyChange
                                   (database, bes.getId(), (String)oldvals.get(bes),
                                    (String)bes.getField(BibtexFields.KEY_FIELD)));
                    }
                    ce.end();
                    undoManager.addEdit(ce);
        }

        // Run third, on EDT:
        public void update() {
            if (cancelled) {
                frame.unblock();
                return;
            }
            markBaseChanged() ;
            numSelected = entries.size();
            output(Globals.lang("Generated BibTeX key for")+" "+
               numSelected+" "+(numSelected!=1 ? Globals.lang("entries")
                                    : Globals.lang("entry")));
            frame.unblock();
        }
    });

        actions.put("search", new BaseAction() {
                public void action() {
                    //sidePaneManager.togglePanel("search");
                    sidePaneManager.show("search");
                    //boolean on = sidePaneManager.isPanelVisible("search");
                    frame.searchToggle.setSelected(true);
                    if (true)
                      frame.searchManager.startSearch();
                }
            });

        actions.put("toggleSearch", new BaseAction() {
                public void action() {
                    //sidePaneManager.togglePanel("search");
                    sidePaneManager.toggle("search");
                    boolean on = sidePaneManager.isComponentVisible("search");
                    frame.searchToggle.setSelected(on);
                    if (on)
                      frame.searchManager.startSearch();
                }
            });

        actions.put("incSearch", new BaseAction() {
                public void action() {
                    sidePaneManager.show("search");
                    frame.searchToggle.setSelected(true);
                    frame.searchManager.startIncrementalSearch();
                }
            });

        // The action for copying the selected entry's key.
        actions.put("copyKey", new BaseAction() {
                public void action() {
                    BibtexEntry[] bes = mainTable.getSelectedEntries();
                    if ((bes != null) && (bes.length > 0)) {
                        storeCurrentEdit();
                        //String[] keys = new String[bes.length];
                        Vector keys = new Vector();
                        // Collect all non-null keys.
                        for (int i=0; i<bes.length; i++)
                            if (bes[i].getField(BibtexFields.KEY_FIELD) != null)
                                keys.add(bes[i].getField(BibtexFields.KEY_FIELD));
                        if (keys.size() == 0) {
                            output("None of the selected entries have BibTeX keys.");
                            return;
                        }
                        StringBuffer sb = new StringBuffer((String)keys.elementAt(0));
                        for (int i=1; i<keys.size(); i++) {
                            sb.append(',');
                            sb.append((String)keys.elementAt(i));
                        }

                        StringSelection ss = new StringSelection(sb.toString());
                        Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(ss, BasePanel.this);

                        if (keys.size() == bes.length)
                            // All entries had keys.
                            output(Globals.lang((bes.length > 1) ? "Copied keys"
                                                : "Copied key")+".");
                        else
                            output(Globals.lang("Warning")+": "+(bes.length-keys.size())
                                   +" "+Globals.lang("out of")+" "+bes.length+" "+
                                   Globals.lang("entries have undefined BibTeX key")+".");
                    }
                }
            });

        // The action for copying a cite for the selected entry.
        actions.put("copyCiteKey", new BaseAction() {
                public void action() {
                    BibtexEntry[] bes = mainTable.getSelectedEntries();
                    if ((bes != null) && (bes.length > 0)) {
                        storeCurrentEdit();
                        //String[] keys = new String[bes.length];
                        Vector keys = new Vector();
                        // Collect all non-null keys.
                        for (int i=0; i<bes.length; i++)
                            if (bes[i].getField(BibtexFields.KEY_FIELD) != null)
                                keys.add(bes[i].getField(BibtexFields.KEY_FIELD));
                        if (keys.size() == 0) {
                            output("None of the selected entries have BibTeX keys.");
                            return;
                        }
                        StringBuffer sb = new StringBuffer((String)keys.elementAt(0));
                        for (int i=1; i<keys.size(); i++) {
                            sb.append(',');
                            sb.append((String)keys.elementAt(i));
                        }

                        StringSelection ss = new StringSelection
                            ("\\cite{"+sb.toString()+"}");
                        Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(ss, BasePanel.this);

                        if (keys.size() == bes.length)
                            // All entries had keys.
                            output(Globals.lang((bes.length > 1) ? "Copied keys"
                                                : "Copied key")+".");
                        else
                            output(Globals.lang("Warning")+": "+(bes.length-keys.size())
                                   +" "+Globals.lang("out of")+" "+bes.length+" "+
                                   Globals.lang("entries have undefined BibTeX key")+".");
                    }
                }
            });

          actions.put("mergeDatabase", new AppendDatabaseAction(frame, this));

          
        
          
          
         actions.put("openFile", new BaseAction() {
           public void action() {
             (new Thread() {
               public void run() {
                 BibtexEntry[] bes = mainTable.getSelectedEntries();
                 String field = "ps";
                 if ( (bes != null) && (bes.length == 1)) {
                   Object link = bes[0].getField("ps");
                   if (bes[0].getField("pdf") != null) {
                     link = bes[0].getField("pdf");
                     field = "pdf";
                   }
                   String filepath = null;
                   if (link != null) {
                     filepath = link.toString();
                   }
                   else {

                     // see if we can fall back to a filename based on the bibtex key
                     String basefile;
                     Object key = bes[0].getField(BibtexFields.KEY_FIELD);
                     if (key != null) {
                       basefile = key.toString();
                        final String[] types = new String[] {"pdf", "ps"};
                        final String sep = System.getProperty("file.separator");
                        for (int i = 0; i < types.length; i++) {
                            String dir = Globals.prefs.get(types[i]+"Directory");
                            if (dir.endsWith(sep)) {
                                dir = dir.substring(0, dir.length()-sep.length());
                            }
                            String found = Util.findPdf(basefile, types[i], dir, new OpenFileFilter("."+types[i]));
                            if (found != null) {
                                filepath = dir+sep+found;
                                break;
                            }
                        }
                     }
                   }


                   if (filepath != null) {
                     //output(Globals.lang("Calling external viewer..."));
                     try {
                       Util.openExternalViewer(metaData(), filepath, field);
                       output(Globals.lang("External viewer called") + ".");
                     }
                     catch (IOException ex) {
                       output(Globals.lang("Error") + ": " + ex.getMessage());
                     }
                   }
                   else
                     output(Globals.lang(
                         "No pdf or ps defined, and no file matching Bibtex key found") +
                            ".");
                 }
                 else
                   output(Globals.lang("No entries or multiple entries selected."));
               }
             }).start();
           }
         });


              actions.put("openUrl", new BaseAction() {
                      public void action() {
                          BibtexEntry[] bes = mainTable.getSelectedEntries();
                          String field = "doi";
                          if ((bes != null) && (bes.length == 1)) {
                              Object link = bes[0].getField("doi");
                              if (bes[0].getField("url") != null) {
                                link = bes[0].getField("url");
                                field = "url";
                              }
                              if (link != null) {
                                //output(Globals.lang("Calling external viewer..."));
                                try {
                                  Util.openExternalViewer(metaData(), link.toString(), field);
                                  output(Globals.lang("External viewer called")+".");
                                } catch (IOException ex) {
                                    output(Globals.lang("Error") + ": " + ex.getMessage());
                                }
                              }
                              else
                                  output(Globals.lang("No url defined")+".");
                          } else
                            output(Globals.lang("No entries or multiple entries selected."));
                      }
              });

          actions.put("replaceAll", new BaseAction() {
                    public void action() {
                      ReplaceStringDialog rsd = new ReplaceStringDialog(frame);
                      rsd.setVisible(true);
                      if (!rsd.okPressed())
                          return;
                      int counter = 0;
                      NamedCompound ce = new NamedCompound(Globals.lang("Replace string"));
                      if (!rsd.selOnly()) {
                          for (Iterator i=database.getKeySet().iterator();
                               i.hasNext();)
                              counter += rsd.replace(database.getEntryById((String)i.next()), ce);
                      } else {
                          BibtexEntry[] bes = mainTable.getSelectedEntries();
                          for (int i=0; i<bes.length; i++)
                              counter += rsd.replace(bes[i], ce);
                      }

                      output(Globals.lang("Replaced")+" "+counter+" "+
                             Globals.lang(counter==1?"occurence":"occurences")+".");
                      if (counter > 0) {
                          ce.end();
                          undoManager.addEdit(ce);
                          markBaseChanged();
                      }
                  }
              });

              actions.put("dupliCheck", new BaseAction() {
                public void action() {
                  DuplicateSearch ds = new DuplicateSearch(BasePanel.this);
                  ds.start();
                }
              });

              /*actions.put("strictDupliCheck", new BaseAction() {
                public void action() {
                  StrictDuplicateSearch ds = new StrictDuplicateSearch(BasePanel.this);
                  ds.start();
                }
              });*/

              actions.put("plainTextImport", new BaseAction() {
                public void action()
                {
                  // get Type of new entry
                  EntryTypeDialog etd = new EntryTypeDialog(frame);
                  Util.placeDialog(etd, BasePanel.this);
                  etd.setVisible(true);
                  BibtexEntryType tp = etd.getChoice();
                  if (tp == null)
                    return;

                  String id = Util.createNeutralId();
                  BibtexEntry bibEntry = new BibtexEntry(id, tp) ;
                  TextInputDialog tidialog = new TextInputDialog(frame, BasePanel.this,
                                                                 "import", true,
                                                                 bibEntry) ;
                  Util.placeDialog(tidialog, BasePanel.this);
                  tidialog.setVisible(true);

                  if (tidialog.okPressed())
                  {
                      Util.setAutomaticFields(Arrays.asList(new BibtexEntry[] {bibEntry}));
                    insertEntry(bibEntry) ;
                  }
                }
              });

              // The action starts the "import from plain text" dialog
              /*actions.put("importPlainText", new BaseAction() {
                      public void action()
                      {
                        BibtexEntry bibEntry = null ;
                        // try to get the first marked entry
                        BibtexEntry[] bes = entryTable.getSelectedEntries();
                        if ((bes != null) && (bes.length > 0))
                          bibEntry = bes[0] ;

                        if (bibEntry != null)
                        {
                          // Create an UndoableInsertEntry object.
                          undoManager.addEdit(new UndoableInsertEntry(database, bibEntry, BasePanel.this));

                          TextInputDialog tidialog = new TextInputDialog(frame, BasePanel.this,
                                                                         "import", true,
                                                                         bibEntry) ;
                          Util.placeDialog(tidialog, BasePanel.this);
                          tidialog.setVisible(true);

                          if (tidialog.okPressed())
                          {
                            output(Globals.lang("changed ")+" '"
                                   +bibEntry.getType().getName().toLowerCase()+"' "
                                   +Globals.lang("entry")+".");
                            refreshTable();
                            int row = tableModel.getNumberFromName(bibEntry.getId());

                            entryTable.clearSelection();
                            entryTable.scrollTo(row);
                            markBaseChanged(); // The database just changed.
                            if (Globals.prefs.getBoolean("autoOpenForm"))
                            {
                                  showEntry(bibEntry);
                            }
                          }
                        }
                      }
                  });
                */
              actions.put("markEntries", new AbstractWorker() {
                  private int besLength = -1;
                public void run() {

                  NamedCompound ce = new NamedCompound(Globals.lang("Mark entries"));
                  BibtexEntry[] bes = mainTable.getSelectedEntries();
                  besLength = bes.length;
          if (bes == null)
              return;
                  for (int i=0; i<bes.length; i++) {
                      Util.markEntry(bes[i], ce);
                  }
                  ce.end();
                  undoManager.addEdit(ce);
                }

                public void update() {
                  markBaseChanged();
                  output(Globals.lang("Marked selected")+" "+Globals.lang(besLength>0?"entry":"entries"));

                }
              });

              actions.put("unmarkEntries", new BaseAction() {
                public void action() {
                    try {
                  NamedCompound ce = new NamedCompound(Globals.lang("Unmark entries"));
                  BibtexEntry[] bes = mainTable.getSelectedEntries();
          if (bes == null)
              return;
                  for (int i=0; i<bes.length; i++) {
                      Util.unmarkEntry(bes[i], database, ce);
                  }
                  ce.end();
                  undoManager.addEdit(ce);
                  markBaseChanged();
                  output(Globals.lang("Unmarked selected")+" "+Globals.lang(bes.length>0?"entry":"entries"));
                    } catch (Throwable ex) { ex.printStackTrace(); }
                }
              });

              actions.put("unmarkAll", new BaseAction() {
                public void action() {
                  NamedCompound ce = new NamedCompound(Globals.lang("Unmark all"));
                  Set keySet = database.getKeySet();
                  for (Iterator i = keySet.iterator(); i.hasNext(); ) {
                    BibtexEntry be = database.getEntryById( (String) i.next());
                    Util.unmarkEntry(be, database, ce);

                  }
                  ce.end();
                  undoManager.addEdit(ce);
                  markBaseChanged();
                }
              });

              actions.put("togglePreview", new BaseAction() {
                      public void action() {
                          boolean enabled = !Globals.prefs.getBoolean("previewEnabled");
                          Globals.prefs.putBoolean("previewEnabled", enabled);
                          frame.setPreviewActive(enabled);
                          frame.previewToggle.setSelected(enabled);
                      }
                  });

              actions.put("toggleHighlightGroupsMatchingAny", new BaseAction() {
                public void action() {
                    boolean enabled = !Globals.prefs.getBoolean("highlightGroupsMatchingAny");
                    Globals.prefs.putBoolean("highlightGroupsMatchingAny", enabled);
                    frame.highlightAny.setSelected(enabled);
                    if (enabled) {
                        frame.highlightAll.setSelected(false);
                        Globals.prefs.putBoolean("highlightGroupsMatchingAll", false);
                    }
                    // ping the listener so it updates:
                    groupsHighlightListener.listChanged(null);
                }
              });

              actions.put("toggleHighlightGroupsMatchingAll", new BaseAction() {
                  public void action() {
                      boolean enabled = !Globals.prefs.getBoolean("highlightGroupsMatchingAll");
                      Globals.prefs.putBoolean("highlightGroupsMatchingAll", enabled);
                      frame.highlightAll.setSelected(enabled);
                      if (enabled) {
                          frame.highlightAny.setSelected(false);
                          Globals.prefs.putBoolean("highlightGroupsMatchingAny", false);
                      }
                      // ping the listener so it updates:
                      groupsHighlightListener.listChanged(null);
                  }
                });

              actions.put("switchPreview", new BaseAction() {
                      public void action() {
                          selectionListener.switchPreview();
                      }
                  });

              actions.put("manageSelectors", new BaseAction() {
                      public void action() {
                          ContentSelectorDialog2 csd = new ContentSelectorDialog2
                              (frame, frame, BasePanel.this, false, metaData, null);
                          Util.placeDialog(csd, frame);
                          csd.setVisible(true);
                      }
                  });


          actions.put("exportToClipboard", new ExportToClipboardAction(frame, database()));
        
        actions.put("writeXMP", new WriteXMPAction(this));
        
        actions.put("abbreviateIso", new AbbreviateAction(this, true));
        actions.put("abbreviateMedline", new AbbreviateAction(this, false));
        actions.put("unabbreviate", new UnabbreviateAction(this));
        actions.put("autoSetPdf", new AutoSetExternalFileForEntries(this, "pdf"));
        actions.put("autoSetPs", new AutoSetExternalFileForEntries(this, "ps"));

    }

    /**
     * This method is called from JabRefFrame is a database specific
     * action is requested by the user. Runs the command if it is
     * defined, or prints an error message to the standard error
     * stream.
     *
     * @param _command The name of the command to run.
    */
    public void runCommand(String _command) {
      final String command = _command;
      //(new Thread() {
      //  public void run() {
          if (actions.get(command) == null)
            Util.pr("No action defined for'" + command + "'");
            else {
        Object o = actions.get(command);
        try {
            if (o instanceof BaseAction)
            ((BaseAction)o).action();
            else {
            // This part uses Spin's features:
            Worker wrk = ((AbstractWorker)o).getWorker();
            // The Worker returned by getWorker() has been wrapped
            // by Spin.off(), which makes its methods be run in
            // a different thread from the EDT.
            CallBack clb = ((AbstractWorker)o).getCallBack();

            ((AbstractWorker)o).init(); // This method runs in this same thread, the EDT.
            // Useful for initial GUI actions, like printing a message.

            // The CallBack returned by getCallBack() has been wrapped
            // by Spin.over(), which makes its methods be run on
            // the EDT.
            wrk.run(); // Runs the potentially time-consuming action
            // without freezing the GUI. The magic is that THIS line
            // of execution will not continue until run() is finished.
            clb.update(); // Runs the update() method on the EDT.
            }
        } catch (Throwable ex) {
            // If the action has blocked the JabRefFrame before crashing, we need to unblock it.
            // The call to unblock will simply hide the glasspane, so there is no harm in calling
            // it even if the frame hasn't been blocked.
            frame.unblock();
            ex.printStackTrace();
        }
        }
      //  }
      //}).start();
    }

    private boolean saveDatabase(File file, boolean selectedOnly, String encoding) throws SaveException {
        SaveSession session;
        frame.block();
        try {
            if (!selectedOnly)
                session = FileActions.saveDatabase(database, metaData, file,
                                           Globals.prefs, false, false, encoding);
            else
                session = FileActions.savePartOfDatabase(database, metaData, file,
                                               Globals.prefs, mainTable.getSelectedEntries(), encoding);

        } catch (UnsupportedCharsetException ex2) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Could not save file. "
                +"Character encoding '%0' is not supported.", encoding),
                    Globals.lang("Save database"), JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex.specificEntry()) {
                // Error occured during processing of
                // be. Highlight it:
                int row = mainTable.findEntry(ex.getEntry()),
                    topShow = Math.max(0, row-3);
                mainTable.setRowSelectionInterval(row, row);
                mainTable.scrollTo(topShow);
                showEntry(ex.getEntry());
            }
            else ex.printStackTrace();

            JOptionPane.showMessageDialog
                (frame, Globals.lang("Could not save file")
                 +".\n"+ex.getMessage(),
                 Globals.lang("Save database"),
                 JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");

        } finally {
            frame.unblock();
        }

        boolean commit = true;
        if (!session.getWriter().couldEncodeAll()) {
            DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, fill:pref", ""));
            JTextArea ta = new JTextArea(session.getWriter().getProblemCharacters());
            ta.setEditable(false);
            builder.append(Globals.lang("The chosen encoding '%0' could not encode the following characters: ",
                      session.getEncoding()));
            builder.append(ta);
            builder.append(Globals.lang("What do you want to do?"));
            String tryDiff = Globals.lang("Try different encoding");
            int answer = JOptionPane.showOptionDialog(frame, builder.getPanel(), Globals.lang("Save database"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[] {Globals.lang("Save"), tryDiff, Globals.lang("Cancel")}, tryDiff);

            if (answer == JOptionPane.NO_OPTION) {
                // The user wants to use another encoding.
                Object choice = JOptionPane.showInputDialog(frame, Globals.lang("Select encoding"), Globals.lang("Save database"),
                        JOptionPane.QUESTION_MESSAGE, null, Globals.ENCODINGS, encoding);
                if (choice != null) {
                    String newEncoding = (String)choice;
                    return saveDatabase(file, selectedOnly, newEncoding);
                } else
                    commit = false;
            } else if (answer == JOptionPane.CANCEL_OPTION)
                    commit = false;


          }

        try {
            if (commit) {
                session.commit();
                this.encoding = encoding; // Make sure to remember which encoding we used.
            }
            else
                session.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return commit;
    }


    /**
     * This method is called from JabRefFrame when the user wants to
     * create a new entry. If the argument is null, the user is
     * prompted for an entry type.
     *
     * @param type The type of the entry to create.
     * @return The newly created BibtexEntry or null the operation was canceled by the user.
     */
    public BibtexEntry newEntry(BibtexEntryType type) {
        if (type == null) {
            // Find out what type is wanted.
            EntryTypeDialog etd = new EntryTypeDialog(frame);
            // We want to center the dialog, to make it look nicer.
            Util.placeDialog(etd, frame);
            etd.setVisible(true);
            type = etd.getChoice();
        }
        if (type != null) { // Only if the dialog was not cancelled.
            String id = Util.createNeutralId();
            final BibtexEntry be = new BibtexEntry(id, type);
            try {
                database.insertEntry(be);

                // Set owner/timestamp if options are enabled:
                ArrayList list = new ArrayList();
                list.add(be);
                Util.setAutomaticFields(list);

                // Create an UndoableInsertEntry object.
                undoManager.addEdit(new UndoableInsertEntry(database, be, BasePanel.this));
                output(Globals.lang("Added new")+" '"+type.getName().toLowerCase()+"' "
                       +Globals.lang("entry")+".");
                final int row = mainTable.findEntry(be);

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (mode != SHOWING_EDITOR) {
                    mode = WILL_SHOW_EDITOR;
                }

                highlightEntry(be);  // Selects the entry. The selection listener will open the editor.

                markBaseChanged(); // The database just changed.
                new FocusRequester(getEntryEditor(be));
                return be;
            } catch (KeyCollisionException ex) {
                Util.pr(ex.getMessage());
            }
        }
        return null;
    }



    /**
     * This method is called from JabRefFrame when the user wants to
     * create a new entry.
     * @param bibEntry The new entry.
     */
    public void insertEntry(BibtexEntry bibEntry)
    {
      if (bibEntry != null)
      {
        try
        {
          database.insertEntry(bibEntry) ;
          if (Globals.prefs.getBoolean("useOwner"))
            // Set owner field to default value
            bibEntry.setField(BibtexFields.OWNER, Globals.prefs.get("defaultOwner") );
            // Create an UndoableInsertEntry object.
            undoManager.addEdit(new UndoableInsertEntry(database, bibEntry, BasePanel.this));
            output(Globals.lang("Added new")+" '"
                   +bibEntry.getType().getName().toLowerCase()+"' "
                   +Globals.lang("entry")+".");
            int row = mainTable.findEntry(bibEntry);

            mainTable.clearSelection();
            mainTable.scrollTo(row);
            markBaseChanged(); // The database just changed.
            if (Globals.prefs.getBoolean("autoOpenForm"))
            {
                  showEntry(bibEntry);
            }
        } catch (KeyCollisionException ex) { Util.pr(ex.getMessage()); }
      }
    }

    public void createMainTable() {
        //Comparator comp = new FieldComparator("author");

        GlazedEntrySorter eventList = new GlazedEntrySorter(database.getEntryMap(), null);
        // Must initialize sort columns somehow:

        database.addDatabaseChangeListener(eventList);
        groupFilterList = new FilterList(eventList.getTheList(), NoSearchMatcher.INSTANCE);
        searchFilterList = new FilterList(groupFilterList, NoSearchMatcher.INSTANCE);
        //final SortedList sortedList = new SortedList(searchFilterList, null);
        MainTableFormat tableFormat = new MainTableFormat(this);
        tableFormat.updateTableFormat();
        //EventTableModel tableModel = new EventTableModel(sortedList, tableFormat);
        mainTable = new MainTable(/*tableModel, */tableFormat, searchFilterList, frame, this);
        
        selectionListener = new MainTableSelectionListener(this, mainTable);
        mainTable.updateFont();
        mainTable.addSelectionListener(selectionListener);
        mainTable.addMouseListener(selectionListener);
        mainTable.addKeyListener(selectionListener);
        mainTable.addFocusListener(selectionListener);
        
        // Add the listener that will take care of highlighting groups as the selection changes:
        groupsHighlightListener = new ListEventListener() {
            public void listChanged(ListEvent listEvent) {
                if (Globals.prefs.getBoolean("highlightGroupsMatchingAny"))
                    getGroupSelector().showMatchingGroups(
                            mainTable.getSelectedEntries(), false);
                else if (Globals.prefs.getBoolean("highlightGroupsMatchingAll"))
                    getGroupSelector().showMatchingGroups(
                            mainTable.getSelectedEntries(), true);
                else // no highlight
                    getGroupSelector().showMatchingGroups(null, true);
            }
        };
        mainTable.addSelectionListener(groupsHighlightListener);

        mainTable.getActionMap().put("cut", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    try { runCommand("cut");
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });
        mainTable.getActionMap().put("copy", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    try { runCommand("copy");
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });
        mainTable.getActionMap().put("paste", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    try { runCommand("paste");
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });

        mainTable.addKeyListener(new KeyAdapter() {

                public void keyPressed(KeyEvent e) {
                    final int keyCode = e.getKeyCode();
                    final TreePath path = frame.groupSelector.getSelectionPath();
                    final GroupTreeNode node = path == null ? null : (GroupTreeNode) path.getLastPathComponent();

                    if (e.isControlDown()) {
                        switch (keyCode) {
                        // The up/down/left/rightkeystrokes are displayed in the
                        // GroupSelector's popup menu, so if they are to be changed,
                        // edit GroupSelector.java accordingly!
                        case KeyEvent.VK_UP:
                            e.consume();
                            if (node != null)
                                frame.groupSelector.moveNodeUp(node, true);
                            break;
                        case KeyEvent.VK_DOWN:
                            e.consume();
                            if (node != null)
                                frame.groupSelector.moveNodeDown(node, true);
                            break;
                        case KeyEvent.VK_LEFT:
                            e.consume();
                            if (node != null)
                                frame.groupSelector.moveNodeLeft(node, true);
                            break;
                        case KeyEvent.VK_RIGHT:
                            e.consume();
                            if (node != null)
                                frame.groupSelector.moveNodeRight(node, true);
                            break;
                        case KeyEvent.VK_PAGE_DOWN:
                            frame.nextTab.actionPerformed(null);
                            e.consume();
                            break;
                        case KeyEvent.VK_PAGE_UP:
                            frame.prevTab.actionPerformed(null);
                            e.consume();
                            break;
                        }
                    } else if (keyCode == KeyEvent.VK_ENTER){
                        e.consume();
                        try { runCommand("edit");
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }
        });
    }

    public void setupMainPanel() {
        //System.out.println("setupMainPanel");
        //splitPane = new com.jgoodies.uif_lite.component.UIFSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(GUIGlobals.SPLIT_PANE_DIVIDER_SIZE);
        // We replace the default FocusTraversalPolicy with a subclass
        // that only allows FieldEditor components to gain keyboard focus,
        // if there is an entry editor open.
        /*splitPane.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
                protected boolean accept(Component c) {
                    Util.pr("jaa");
                    if (showing == null)
                        return super.accept(c);
                    else
                        return (super.accept(c) &&
                                (c instanceof FieldEditor));
                }
                });*/

        createMainTable();

        splitPane.setTopComponent(mainTable.getPane());

        //setupTable();
        // If an entry is currently being shown, make sure it stays shown,
        // otherwise set the bottom component to null.
        if (mode == SHOWING_PREVIEW) {
            mode = SHOWING_NOTHING;
            int row = mainTable.findEntry(currentPreview.entry);
            if (row >= 0)
                mainTable.setRowSelectionInterval(row, row);

        }
        else if (mode == SHOWING_EDITOR) {
            mode = SHOWING_NOTHING;
            /*int row = mainTable.findEntry(currentEditor.entry);
            if (row >= 0)
                mainTable.setRowSelectionInterval(row, row);
            */
            //showEntryEditor(currentEditor);
        } else
            splitPane.setBottomComponent(null);


        setLayout(new BorderLayout());
        removeAll();
        add(splitPane, BorderLayout.CENTER);

        // Set up AutoCompleters for this panel:
        if (Globals.prefs.getBoolean("autoComplete")) {
            instantiateAutoCompleters();
        }

        splitPane.revalidate();
        revalidate();
        repaint();
    }

    public HashMap getAutoCompleters() {
        return autoCompleters;
    }
    
    public AutoCompleter getAutoCompleter(String fieldName) {
        return (AutoCompleter)autoCompleters.get(fieldName);
    }

    private void instantiateAutoCompleters() {
        autoCompleters.clear();
        String[] completeFields = Globals.prefs.getStringArray("autoCompleteFields");
        for (int i = 0; i < completeFields.length; i++) {
            String field = completeFields[i];
            autoCompleters.put(field, new AutoCompleter(field));
        }
        for (Iterator i=database.getKeySet().iterator(); i.hasNext();) {
            BibtexEntry entry = database.getEntryById((String)i.next());
            Util.updateCompletersForEntry(autoCompleters, entry);
        }
    }


    /**
     * This method is called after a database has been parsed. The
     * hashmap contains the contents of all comments in the .bib file
     * that started with the meta flag (GUIGlobals.META_FLAG).
     * In this method, the meta data are input to their respective
     * handlers.
     *
     * @param meta Metadata to input.
     */
    public void parseMetaData(HashMap meta) {
        metaData = new MetaData(meta,database());

    }

    /*
    public void refreshTable() {
        //System.out.println("hiding="+hidingNonHits+"\tlastHits="+lastSearchHits);
        // This method is called by EntryTypeForm when a field value is
        // stored. The table is scheduled for repaint.
        entryTable.assureNotEditing();
        //entryTable.invalidate();
        BibtexEntry[] bes = entryTable.getSelectedEntries();
    if (hidingNonHits)
        tableModel.update(lastSearchHits);
    else
        tableModel.update();
    //tableModel.remap();
        if ((bes != null) && (bes.length > 0))
            selectEntries(bes, 0);

    //long toc = System.currentTimeMillis();
    //	Util.pr("Refresh took: "+(toc-tic)+" ms");
    } */

    public void updatePreamble() {
        if (preambleEditor != null)
            preambleEditor.updatePreamble();
    }

    public void assureStringDialogNotEditing() {
        if (stringDialog != null)
            stringDialog.assureNotEditing();
    }

    public void updateStringDialog() {
        if (stringDialog != null)
            stringDialog.refreshTable();
    }

    public void updateEntryPreviewToRow(BibtexEntry e) {

    }

    public void adjustSplitter() {
        int mode = getMode();
        if (mode == SHOWING_PREVIEW) {
            splitPane.setDividerLocation(splitPane.getHeight()-GUIGlobals.PREVIEW_PANEL_HEIGHT);
        } else {
            splitPane.setDividerLocation(GUIGlobals.VERTICAL_DIVIDER_LOCATION);

        }
    }



    /**
     * Stores the source view in the entry editor, if one is open, has the source view
     * selected and the source has been edited.
     * @return boolean false if there is a validation error in the source panel, true otherwise.
     */
    public boolean entryEditorAllowsChange() {
      Component c = splitPane.getBottomComponent();
      if ((c != null) && (c instanceof EntryEditor)) {
        return ((EntryEditor)c).lastSourceAccepted();
      }
      else
        return true;
    }

    public void moveFocusToEntryEditor() {
      Component c = splitPane.getBottomComponent();
      if ((c != null) && (c instanceof EntryEditor)) {
        new FocusRequester(c);
      }
    }

    /**
     * Ensure that no preview is shown. Called when preview is turned off. Must chech if
     * a preview is in fact visible before doing anything rash.
     */
    public void hidePreview() {
        Globals.prefs.putBoolean("previewEnabled", false);

      Component c = splitPane.getBottomComponent();
      if ((c != null) && !(c instanceof EntryEditor))
        splitPane.setBottomComponent(null);
    }

    public boolean isShowingEditor() {
      return ((splitPane.getBottomComponent() != null)
              && (splitPane.getBottomComponent() instanceof EntryEditor));
    }

    public void showEntry(final BibtexEntry be) {
        if (showing == be) {
            if (splitPane.getBottomComponent() == null) {
                // This is the special occasion when showing is set to an
                // entry, but no entry editor is in fact shown. This happens
                // after Preferences dialog is closed, and it means that we
                // must make sure the same entry is shown again. We do this by
                // setting showing to null, and recursively calling this method.
                showing = null;
                showEntry(be);
            } else {
              // The correct entry is already being shown. Make sure the editor
              // is updated.
              ((EntryEditor)splitPane.getBottomComponent()).updateAllFields();

            }
            return;

        }

        EntryEditor form;
        int divLoc = -1;
        String visName = null;
        if (showing != null) {
            visName = ((EntryEditor)splitPane.getBottomComponent()).
                getVisiblePanelName();
        }
        if (showing != null)
            divLoc = splitPane.getDividerLocation();

        if (entryEditors.containsKey(be.getType().getName())) {
            // We already have an editor for this entry type.
            form = (EntryEditor)entryEditors.get
                ((be.getType().getName()));
            form.switchTo(be);
            if (visName != null)
                form.setVisiblePanel(visName);
            splitPane.setBottomComponent(form);
            //highlightEntry(be);
        } else {
            // We must instantiate a new editor for this type.
            form = new EntryEditor(frame, BasePanel.this, be);
            if (visName != null)
                form.setVisiblePanel(visName);
            splitPane.setBottomComponent(form);

            //highlightEntry(be);
            entryEditors.put(be.getType().getName(), form);

        }
        if (divLoc > 0) {
          splitPane.setDividerLocation(divLoc);
        }
        else
            splitPane.setDividerLocation
                (GUIGlobals.VERTICAL_DIVIDER_LOCATION);
        //new FocusRequester(form);
        //form.requestFocus();

        showing = be;
        setEntryEditorEnabled(true); // Make sure it is enabled.
    }

    /**
     * Get an entry editor ready to edit the given entry. If an appropriate editor is already
     * cached, it will be updated and returned.
     * @param entry The entry to be edited.
     * @return A suitable entry editor.
     */
    public EntryEditor getEntryEditor(BibtexEntry entry) {
        EntryEditor form;
        if (entryEditors.containsKey(entry.getType().getName())) {
            EntryEditor visibleNow = currentEditor;
            // We already have an editor for this entry type.
            form = (EntryEditor)entryEditors.get
                ((entry.getType().getName()));

            form.switchTo(entry);
            //if (visName != null)
            //    form.setVisiblePanel(visName);
        } else {
            // We must instantiate a new editor for this type.
            form = new EntryEditor(frame, BasePanel.this, entry);
            //if (visName != null)
            //    form.setVisiblePanel(visName);

            entryEditors.put(entry.getType().getName(), form);
        }
        return form;
    }

    public EntryEditor getCurrentEditor() {
        return currentEditor;
    }

    /**
     * Sets the given entry editor as the bottom component in the split pane. If an entry editor already
     * was shown, makes sure that the divider doesn't move.
     * Updates the mode to SHOWING_EDITOR.
     * @param editor The entry editor to add.
     */
    public void showEntryEditor(EntryEditor editor) {
        int oldSplitterLocation = -1;
        if (mode == SHOWING_EDITOR)
            oldSplitterLocation = splitPane.getDividerLocation();
        boolean adjustSplitter = (mode == WILL_SHOW_EDITOR);
        mode = SHOWING_EDITOR;
        currentEditor = editor;
        splitPane.setBottomComponent(editor);
        if (oldSplitterLocation > 0)
            splitPane.setDividerLocation(oldSplitterLocation);
        if (adjustSplitter) {
            adjustSplitter();
            //new FocusRequester(editor);
        }
    }

    /**
     * Sets the given preview panel as the bottom component in the split panel.
     * Updates the mode to SHOWING_PREVIEW.
     * @param preview The preview to show.
     */
    public void showPreview(PreviewPanel preview) {
        mode = SHOWING_PREVIEW;
        currentPreview = preview;
        splitPane.setBottomComponent(preview.getPane());
    }

    /**
     * Removes the bottom component.
     */
    public void hideBottomComponent() {
        mode = SHOWING_NOTHING;
        splitPane.setBottomComponent(null);
    }

    /**
     * This method selects the given entry, and scrolls it into view in the table.
     * If an entryEditor is shown, it is given focus afterwards.
     */
    public void highlightEntry(final BibtexEntry be) {
        //SwingUtilities.invokeLater(new Thread() {
        //     public void run() {
                 final int row = mainTable.findEntry(be);
                 if (row >= 0) {
                    mainTable.setRowSelectionInterval(row, row);
                    //entryTable.setActiveRow(row);
                    mainTable.ensureVisible(row);
                 }
        //     }
        //});
    }


    /**
     * This method is called from an EntryEditor when it should be closed. We relay
     * to the selection listener, which takes care of the rest.
     * @param editor The entry editor to close.
     */
    public void entryEditorClosing(EntryEditor editor) {
        selectionListener.entryEditorClosing(editor);
    }

    /**
     * This method selects the given enties.
     * If an entryEditor is shown, it is given focus afterwards.
     */
    /*public void selectEntries(final BibtexEntry[] bes, final int toScrollTo) {

        SwingUtilities.invokeLater(new Thread() {
             public void run() {
                 int rowToScrollTo = 0;
                 entryTable.revalidate();
                 entryTable.clearSelection();
                 loop: for (int i=0; i<bes.length; i++) {
                    if (bes[i] == null)
                        continue loop;
                    int row = tableModel.getNumberFromName(bes[i].getId());
                    if (i==toScrollTo)
                    rowToScrollTo = row;
                    if (row >= 0)
                        entryTable.addRowSelectionIntervalQuietly(row, row);
                 }
                 entryTable.ensureVisible(rowToScrollTo);
                 Component comp = splitPane.getBottomComponent();
                 //if (comp instanceof EntryEditor)
                 //    comp.requestFocus();
             }
        });
    } */

    /**
     * Closes the entry editor if it is showing the given entry.
     *
     * @param be a <code>BibtexEntry</code> value
     */
    public void ensureNotShowing(BibtexEntry be) {
        if ((mode == SHOWING_EDITOR) && (currentEditor.getEntry() == be)) {
            selectionListener.entryEditorClosing(currentEditor);
        }
    }

    public void updateEntryEditorIfShowing() {
        if (mode == SHOWING_EDITOR) {
            if (currentEditor.getType() != currentEditor.getEntry().getType()) {
                // The entry has changed type, so we must get a new editor.
                showing = null;
                EntryEditor newEditor = getEntryEditor(currentEditor.getEntry());
                showEntryEditor(newEditor);
            } else {
                currentEditor.updateAllFields();
                currentEditor.updateSource();
            }
        }
    }

    /**
     * If an entry editor is showing, make sure its currently focused field
     * stores its changes, if any.
     */
    public void storeCurrentEdit() {
        if (isShowingEditor()) {
            EntryEditor editor = (EntryEditor)splitPane.getBottomComponent();
            editor.storeCurrentEdit();
        }

    }

    /**
     * This method iterates through all existing entry editors in this
     * BasePanel, telling each to update all its instances of
     * FieldContentSelector. This is done to ensure that the list of words
     * in each selector is up-to-date after the user has made changes in
     * the Manage dialog.
     */
    public void updateAllContentSelectors() {
        for (Iterator i=entryEditors.keySet().iterator(); i.hasNext();) {
            EntryEditor ed = (EntryEditor)entryEditors.get(i.next());
            ed.updateAllContentSelectors();
        }
    }

    public void rebuildAllEntryEditors() {
        for (Iterator i=entryEditors.keySet().iterator(); i.hasNext();) {
            EntryEditor ed = (EntryEditor)entryEditors.get(i.next());
            ed.rebuildPanels();
        }

    }

    public void markBaseChanged() {
        baseChanged = true;

        // Put an asterix behind the file name to indicate the
        // database has changed.
        String oldTitle = frame.getTabTitle(this);
        if (!oldTitle.endsWith("*"))
            frame.setTabTitle(this, oldTitle+"*", frame.getTabTooltip(this));

        // If the status line states that the base has been saved, we
        // remove this message, since it is no longer relevant. If a
        // different message is shown, we leave it.
        if (frame.statusLine.getText().startsWith("Saved database"))
            frame.output(" ");
    }

    public void markNonUndoableBaseChanged() {
        nonUndoableChange = true;
        markBaseChanged();
    }

    public synchronized void markChangedOrUnChanged() {
        if (undoManager.hasChanged()) {
            if (!baseChanged)
                markBaseChanged();
        }
        else if (baseChanged && !nonUndoableChange) {
            baseChanged = false;
            if (getFile() != null)
                frame.setTabTitle(BasePanel.this, getFile().getName(),
                        getFile().getAbsolutePath());
            else
                frame.setTabTitle(BasePanel.this, Globals.lang("untitled"), null);
        }
    }

    /**
     * Selects a single entry, and scrolls the table to center it.
     *
     * @param pos Current position of entry to select.
     *
     */
    public void selectSingleEntry(int pos) {
        mainTable.clearSelection();
        mainTable.addRowSelectionInterval(pos, pos);
        mainTable.scrollToCenter(pos, 0);
    }

    /* *
     * Selects all entries with a non-zero value in the field
     * @param field <code>String</code> field name.
     */
/*    public void selectResults(String field) {
      LinkedList intervals = new LinkedList();
      int prevStart = -1, prevToSel = 0;
      // First we build a list of intervals to select, without touching the table.
      for (int i = 0; i < entryTable.getRowCount(); i++) {
        String value = (String) (database.getEntryById
                                 (tableModel.getIdForRow(i)))
            .getField(field);
        if ( (value != null) && !value.equals("0")) {
          if (prevStart < 0)
            prevStart = i;
          prevToSel = i;
        }
        else if (prevStart >= 0) {
          intervals.add(new int[] {prevStart, prevToSel});
          prevStart = -1;
        }
      }
      // Then select those intervals, if any.
      if (intervals.size() > 0) {
        entryTable.setSelectionListenerEnabled(false);
        entryTable.clearSelection();
        for (Iterator i=intervals.iterator(); i.hasNext();) {
          int[] interval = (int[])i.next();
          entryTable.addRowSelectionInterval(interval[0], interval[1]);
        }
        entryTable.setSelectionListenerEnabled(true);
      }
  */

    public void setSearchMatcher(SearchMatcher matcher) {
        searchFilterList.setMatcher(matcher);
    }

    public void setGroupMatcher(Matcher matcher) {
        groupFilterList.setMatcher(matcher);
    }

    public void stopShowingSearchResults() {
        searchFilterList.setMatcher(NoSearchMatcher.INSTANCE);
    }

    public void stopShowingGroup() {
        groupFilterList.setMatcher(NoSearchMatcher.INSTANCE);

     }

     public BibtexDatabase getDatabase(){
        return database ;
    }

    public void preambleEditorClosing() {
        preambleEditor = null;
    }

    public void stringsClosing() {
        stringDialog = null;
    }

    public void changeType(BibtexEntry entry, BibtexEntryType type) {
      changeType(new BibtexEntry[] {entry}, type);
    }

    public void changeType(BibtexEntryType type) {
      BibtexEntry[] bes = mainTable.getSelectedEntries();
      changeType(bes, type);
    }

    public void changeType(BibtexEntry[] bes, BibtexEntryType type) {

        if ((bes == null) || (bes.length == 0)) {
            output("First select the entries you wish to change type "+
                   "for.");
            return;
        }
        if (bes.length > 1) {
            int choice = JOptionPane.showConfirmDialog
                (this, "Multiple entries selected. Do you want to change"
                 +"\nthe type of all these to '"+type.getName()+"'?",
                 "Change type", JOptionPane.YES_NO_OPTION,
                 JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.NO_OPTION)
                return;
        }

        NamedCompound ce = new NamedCompound(Globals.lang("change type"));
        for (int i=0; i<bes.length; i++) {
            ce.addEdit(new UndoableChangeType(bes[i],
                                              bes[i].getType(),
                                              type));
            bes[i].setType(type);
        }

        output(Globals.lang("Changed type to")+" '"+type.getName()+"' "
               +Globals.lang("for")+" "+bes.length
               +" "+Globals.lang("entries")+".");
        ce.end();
        undoManager.addEdit(ce);
        markBaseChanged();
        updateEntryEditorIfShowing();
    }

    public boolean showDeleteConfirmationDialog(int numberOfEntries) {
        if (Globals.prefs.getBoolean("confirmDelete")) {
            String msg = Globals.lang("Really delete the selected")
                + " " + Globals.lang("entry") + "?",
                title = Globals.lang("Delete entry");
            if (numberOfEntries > 1) {
                msg = Globals.lang("Really delete the selected")
                    + " " + numberOfEntries + " " + Globals.lang("entries") + "?";
                title = Globals.lang("Delete multiple entries");
            }

            CheckBoxMessage cb = new CheckBoxMessage
                (msg, Globals.lang("Disable this confirmation dialog"), false);

            int answer = JOptionPane.showConfirmDialog(frame, cb, title,
                                                       JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);
            if (cb.isSelected())
                Globals.prefs.putBoolean("confirmDelete", false);
            return (answer == JOptionPane.YES_OPTION);
        } else return true;

    }
    
    /**
     * If the relevant option is set, autogenerate keys for all entries that are
     * lacking keys.
     */
    public void autoGenerateKeysBeforeSaving() {
        if (Globals.prefs.getBoolean("generateKeysBeforeSaving")) {
            BibtexEntry bes;
            NamedCompound ce = new NamedCompound(Globals.lang("autogenerate keys"));
            boolean any = false;
            for (Iterator i=database.getKeySet().iterator(); i.hasNext();) {
                bes = database.getEntryById((String)i.next());
                String oldKey = bes.getCiteKey();
                if ((oldKey == null) || (oldKey.equals(""))) {
                    LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), database, bes);
                    ce.addEdit(new UndoableKeyChange(database, bes.getId(), null,
                        (String)bes.getField(BibtexFields.KEY_FIELD)));
                    any = true;
                }
            }
            // Store undo information, if any:
            if (any) {
                ce.end();
                undoManager.addEdit(ce);
            }
        }
    }
    
    /**
     * Activates or deactivates the entry preview, depending on the argument.
     * When deactivating, makes sure that any visible preview is hidden.
     * @param enabled
     */
    public void setPreviewActive(boolean enabled) {
        selectionListener.setPreviewActive(enabled);
    }


    class UndoAction extends BaseAction {
        public void action() {
            try {
                String name = undoManager.getUndoPresentationName();
                undoManager.undo();
                markBaseChanged();
                frame.output(name);
            } catch (CannotUndoException ex) {
                frame.output(Globals.lang("Nothing to undo")+".");
            }
            // After everything, enable/disable the undo/redo actions
            // appropriately.
            //updateUndoState();
            //redoAction.updateRedoState();
            markChangedOrUnChanged();
        }
    }

    class RedoAction extends BaseAction {
        public void action() {
            try {
                String name = undoManager.getRedoPresentationName();
                undoManager.redo();
                markBaseChanged();
                frame.output(name);
            } catch (CannotRedoException ex) {
                frame.output(Globals.lang("Nothing to redo")+".");
            }
            // After everything, enable/disable the undo/redo actions
            // appropriately.
            //updateRedoState();
            //undoAction.updateUndoState();
            markChangedOrUnChanged();
        }
    }

    // Method pertaining to the ClipboardOwner interface.
    public void lostOwnership(Clipboard clipboard, Transferable contents) {}


  public void setEntryEditorEnabled(boolean enabled) {
    if ((showing != null) && (splitPane.getBottomComponent() instanceof EntryEditor)) {
          EntryEditor ed = (EntryEditor)splitPane.getBottomComponent();
          if (ed.isEnabled() != enabled)
            ed.setEnabled(enabled);
    }
  }

  public String fileMonitorHandle() { return fileMonitorHandle; }

    public void fileUpdated() {
      if (saving)
        return; // We are just saving the file, so this message is most likely due
      // to bad timing. If not, we'll handle it on the next polling.
      //Util.pr("File '"+file.getPath()+"' has been modified.");
      updatedExternally = true;

      final ChangeScanner scanner = new ChangeScanner(frame, BasePanel.this);

      // Adding the sidepane component is Swing work, so we must do this in the Swing
      // thread:
      Thread t = new Thread() {
	      public void run() {
		  
		  // Check if there is already a notification about external
		  // changes:
		  boolean hasAlready = sidePaneManager.hasComponent(FileUpdatePanel.NAME);
		  if (hasAlready) {
		      sidePaneManager.hideComponent(FileUpdatePanel.NAME);
		      sidePaneManager.unregisterComponent(FileUpdatePanel.NAME);
		  }
		  FileUpdatePanel pan = new FileUpdatePanel(frame, BasePanel.this,
							    sidePaneManager, getFile(), scanner);
		  sidePaneManager.register(FileUpdatePanel.NAME, pan);
		  sidePaneManager.show(FileUpdatePanel.NAME);
		  setUpdatedExternally(false);
		  //scanner.displayResult();
	      }
	  };

      // Test: running scan automatically in background
      scanner.changeScan(BasePanel.this.getFile());
      try {
	  scanner.join();
      } catch (InterruptedException e) {
	  e.printStackTrace();
      }

      if (scanner.changesFound()) {
	  SwingUtilities.invokeLater(t);
      } else {
	  setUpdatedExternally(false);
	  //System.out.println("No changes found.");
      }
    }

      public void fileRemoved() {
        Util.pr("File '"+getFile().getPath()+"' has been deleted.");
      }


      public void cleanUp() {
        if (fileMonitorHandle != null)
          Globals.fileUpdateMonitor.removeUpdateListener(fileMonitorHandle);
      }

  public void setUpdatedExternally(boolean b) {
    updatedExternally = b;
  }

    /**
     * Get an array containing the currently selected entries.
     *
     * @return An array containing the selected entries.
     */
    public BibtexEntry[] getSelectedEntries() {
        return mainTable.getSelectedEntries();
    }

    /**
     * Get the file where this database was last saved to or loaded from, if any.
     *
     * @return The relevant File, or null if none is defined.
     */
    public File getFile() {
        return metaData.getFile();
    }
    
    /**
     * Get a String containing a comma-separated list of the bibtex keys
     * of the selected entries.
     *
     * @return A comma-separated list of the keys of the selected entries.
     */
    public String getKeysForSelection() {
        List entries = mainTable.getSelected();
        StringBuffer result = new StringBuffer();
        String citeKey = "";//, message = "";
        boolean first = true;
        for (Iterator i = entries.iterator(); i.hasNext();) {
            BibtexEntry bes = (BibtexEntry) i.next();
            citeKey = (String) bes.getField(BibtexFields.KEY_FIELD);
            // if the key is empty we give a warning and ignore this entry
            if (citeKey == null || citeKey.equals(""))
                continue;
            if (first) {
                result.append(citeKey);
                first = false;
            } else {
                result.append(",").append(citeKey);
            }
        }
        return result.toString();
    }

    public GroupSelector getGroupSelector() {
        return frame.groupSelector;
    }

}
