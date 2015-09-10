/*  Copyright (C) 2003-2015 JabRef contributors.
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
import java.io.StringReader;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sf.jabref.DatabaseChangeEvent.ChangeType;
import net.sf.jabref.autocompleter.AutoCompleter;
import net.sf.jabref.autocompleter.AutoCompleterFactory;
import net.sf.jabref.autocompleter.ContentAutoCompleters;
import net.sf.jabref.collab.ChangeScanner;
import net.sf.jabref.collab.FileUpdateListener;
import net.sf.jabref.collab.FileUpdatePanel;
import net.sf.jabref.export.ExportToClipboardAction;
import net.sf.jabref.export.FileActions;
import net.sf.jabref.export.FileActions.DatabaseSaveType;
import net.sf.jabref.export.SaveDatabaseAction;
import net.sf.jabref.export.SaveException;
import net.sf.jabref.export.SaveSession;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;
import net.sf.jabref.external.AttachFileAction;
import net.sf.jabref.external.AutoSetExternalFileForEntries;
import net.sf.jabref.external.ExternalFileMenuItem;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.FindFullTextAction;
import net.sf.jabref.external.RegExpFileSearch;
import net.sf.jabref.external.SynchronizeFileField;
import net.sf.jabref.external.WriteXMPAction;
import net.sf.jabref.groups.GroupSelector;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.gui.CleanUpAction;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.GlazedEntrySorter;
import net.sf.jabref.gui.GroupAddRemoveDialog;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.gui.MainTableFormat;
import net.sf.jabref.gui.MainTableSelectionListener;
import net.sf.jabref.gui.MergeEntriesDialog;
import net.sf.jabref.imports.AppendDatabaseAction;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.SPIRESFetcher;
import net.sf.jabref.journals.AbbreviateAction;
import net.sf.jabref.journals.UnabbreviateAction;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.labelPattern.SearchFixDuplicateLabels;
import net.sf.jabref.search.matchers.NoSearchMatcher;
import net.sf.jabref.search.matchers.SearchMatcher;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialFieldAction;
import net.sf.jabref.specialfields.SpecialFieldDatabaseChangeListener;
import net.sf.jabref.specialfields.SpecialFieldValue;
import net.sf.jabref.sql.DBConnectDialog;
import net.sf.jabref.sql.DBExporterAndImporterFactory;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.DbConnectAction;
import net.sf.jabref.sql.SQLUtil;
import net.sf.jabref.sql.exporter.DBExporter;
import net.sf.jabref.undo.CountingUndoManager;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableChangeType;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.undo.UndoableKeyChange;
import net.sf.jabref.undo.UndoableRemoveEntry;
import net.sf.jabref.util.FileBasedLock;
import net.sf.jabref.util.Util;
import net.sf.jabref.wizard.text.gui.TextInputDialog;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BasePanel extends JPanel implements ClipboardOwner, FileUpdateListener {

    private static final long serialVersionUID = 1L;

    private static final Log LOGGER = LogFactory.getLog(BasePanel.class);

    public final static int SHOWING_NOTHING = 0;
    private final static int SHOWING_PREVIEW = 1;
    public final static int SHOWING_EDITOR = 2;
    public final static int WILL_SHOW_EDITOR = 3;

    /* 
     * The database shown in this panel.
     */
    BibtexDatabase database;

    private int mode = 0;
    private EntryEditor currentEditor = null;
    private PreviewPanel currentPreview = null;

    boolean tmp = true;

    private MainTableSelectionListener selectionListener = null;
    private ListEventListener<BibtexEntry> groupsHighlightListener;
    JSplitPane contentPane = new JSplitPane();

    private JSplitPane splitPane;

    JabRefFrame frame;

    private String fileMonitorHandle = null;
    private boolean saving = false;
    private boolean updatedExternally = false;
    private String encoding;

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();

    // AutoCompleter used in the search bar
    private AutoCompleter<String> searchAutoCompleter;

    // The undo manager.
    public final CountingUndoManager undoManager = new CountingUndoManager(this);
    private final UndoAction undoAction = new UndoAction();
    private final RedoAction redoAction = new RedoAction();

    private final List<BibtexEntry> previousEntries = new ArrayList<BibtexEntry>();
    private final List<BibtexEntry> nextEntries = new ArrayList<BibtexEntry>();

    //ExampleFileFilter fileFilter;
    // File filter for .bib files.

    private boolean baseChanged = false;
    private boolean nonUndoableChange = false;
    // Used to track whether the base has changed since last save.

    //EntryTableModel tableModel = null;
    //public EntryTable entryTable = null;
    public MainTable mainTable = null;
    public MainTableFormat tableFormat = null;
    private FilterList<BibtexEntry> searchFilterList = null;
    private FilterList<BibtexEntry> groupFilterList = null;

    public RightClickMenu rcm;

    private BibtexEntry showing = null;

    // Variable to prevent erroneous update of back/forward histories at the time
    // when a Back or Forward operation is being processed:
    private boolean backOrForwardInProgress = false;

    // To indicate which entry is currently shown.
    public final HashMap<String, EntryEditor> entryEditors = new HashMap<String, EntryEditor>();
    // To contain instantiated entry editors. This is to save time
    // in switching between entries.

    //HashMap entryTypeForms = new HashMap();
    // Hashmap to keep track of which entries currently have open
    // EntryTypeForm dialogs.

    private PreambleEditor preambleEditor = null;
    // Keeps track of the preamble dialog if it is open.

    private StringDialog stringDialog = null;
    // Keeps track of the string dialog if it is open.

    private SaveDatabaseAction saveAction;

    /**
     * The group selector component for this database. Instantiated by the
     * SidePaneManager if necessary, or from this class if merging groups from a
     * different database.
     */
    //GroupSelector groupSelector;

    private boolean showingSearch = false;

    public boolean sortingBySearchResults = false;
    public boolean coloringBySearchResults = false;
    public boolean hidingNonHits = false;
    public boolean sortingByGroup = false;
    public boolean sortingByCiteSeerResults = false;
    public boolean coloringByGroup = false;

    int lastSearchHits = -1; // The number of hits in the latest search.
    // Potential use in hiding non-hits completely.

    // MetaData parses, keeps and writes meta data.
    final MetaData metaData;

    private final HashMap<String, Object> actions = new HashMap<String, Object>();
    private SidePaneManager sidePaneManager;

    // Returns a collection of AutoCompleters, which are populated from the current database
    public ContentAutoCompleters getAutoCompleters() {
        return autoCompleters;
    }

    private ContentAutoCompleters autoCompleters;

    public BasePanel(JabRefFrame frame, BibtexDatabase db, File file,
            MetaData metaData, String encoding) {
        assert (frame != null);
        assert (db != null);
        //file may be null
        assert (encoding != null);
        assert (metaData != null);

        this.encoding = encoding;
        this.metaData = metaData;
        // System.out.println(encoding);
        //super(JSplitPane.HORIZONTAL_SPLIT, true);
        this.sidePaneManager = Globals.sidePaneManager;
        this.frame = frame;
        database = db;

        setupActions();
        setupMainPanel();

        metaData.setFile(file);

        // ensure that at each addition of a new entry, the entry is added to the groups interface
        db.addDatabaseChangeListener(new GroupTreeUpdater());

        if (file == null) {
            if (!database.getEntries().isEmpty()) {
                // if the database is not empty and no file is assigned,
                // the database came from an import and has to be treated somehow
                // -> mark as changed
                this.baseChanged = true;
            }
        } else {
            // Register so we get notifications about outside changes to the file.
            try {
                fileMonitorHandle = Globals.fileUpdateMonitor.addUpdateListener(this,
                        file);
            } catch (IOException ex) {
                LOGGER.warn("Could not register FileUpdateMonitor", ex);
            }
        }

    }

    public boolean isBaseChanged() {
        return baseChanged;
    }

    public int getMode() {
        return mode;
    }

    //Done by MrDlib
    public void setMode(int mode) {
        this.mode = mode;
    }

    //Done by MrDlib

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
        boolean suppressOutput = false;
        if (!suppressOutput) {
            frame.output(s);
        }
    }

    private void setupActions() {
        saveAction = new SaveDatabaseAction(this);
        CleanUpAction cleanUpAction = new CleanUpAction(this);

        actions.put("undo", undoAction);
        actions.put("redo", redoAction);

        actions.put("focusTable", new BaseAction() {

            @Override
            public void action() throws Throwable {
                new FocusRequester(mainTable);
            }
        });

        // The action for opening an entry editor.
        actions.put("edit", new BaseAction() {

            @Override
            public void action() {
                /*System.out.println(Globals.focusListener.getFocused().getClass().getName());
                if (Globals.focusListener.getFocused() instanceof FieldEditor)
                    new FocusRequester(mainTable);
                else*/
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

        actions.put("test",// new AccessLinksForEntries.SaveWithLinkedFiles(this));
                new FindFullTextAction(this));

        // The action for saving a database.
        actions.put("save", saveAction);

        actions.put("saveAs", new BaseAction() {

            @Override
            public void action() throws Throwable {
                saveAction.saveAs();
            }
        });

        actions.put("saveSelectedAs", new SaveSelectedAction(FileActions.DatabaseSaveType.DEFAULT));

        actions.put("saveSelectedAsPlain", new SaveSelectedAction(FileActions.DatabaseSaveType.PLAIN_BIBTEX));

        // The action for copying selected entries.
        actions.put("copy", new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();

                if ((bes != null) && (bes.length > 0)) {
                    TransferableBibtexEntry trbe
                    = new TransferableBibtexEntry(bes);
                    // ! look at ClipBoardManager
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(trbe, BasePanel.this);
                    output(Globals.lang("Copied") + ' ' + (bes.length > 1 ? bes.length + " "
                            + Globals.lang("entries")
                            : "1 " + Globals.lang("entry") + '.'));
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

                            output(Globals.lang("Copied cell contents") + '.');
                        }
                    }
                }
            }
        });

        actions.put("cut", new BaseAction() {

            @Override
            public void action() throws Throwable {
                runCommand("copy");
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                //int row0 = mainTable.getSelectedRow();
                if ((bes != null) && (bes.length > 0)) {
                    // Create a CompoundEdit to make the action undoable.
                    NamedCompound ce = new NamedCompound
                            (Globals.lang(bes.length > 1 ? "cut entries" : "cut entry"));
                    // Loop through the array of entries, and delete them.
                    for (BibtexEntry be : bes) {
                        database.removeEntry(be.getId());
                        ensureNotShowing(be);
                        ce.addEdit(new UndoableRemoveEntry
                                (database, be, BasePanel.this));
                    }
                    //entryTable.clearSelection();
                    frame.output(Globals.lang("Cut_pr") + ' ' +
                            (bes.length > 1 ? bes.length
                                    + " " + Globals.lang("entries")
                                    : Globals.lang("entry")) + '.');
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

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length > 0)) {

                    boolean goOn = showDeleteConfirmationDialog(bes.length);
                    if (goOn) {
                        // Create a CompoundEdit to make the action undoable.
                        NamedCompound ce = new NamedCompound
                                (Globals.lang(bes.length > 1 ? "delete entries" : "delete entry"));
                        // Loop through the array of entries, and delete them.
                        for (BibtexEntry be : bes) {
                            database.removeEntry(be.getId());
                            ensureNotShowing(be);
                            ce.addEdit(new UndoableRemoveEntry(database, be, BasePanel.this));
                        }
                        markBaseChanged();
                        frame.output(Globals.lang("Deleted") + ' ' +
                                (bes.length > 1 ? bes.length
                                        + " " + Globals.lang("entries")
                                        : Globals.lang("entry")) + '.');
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

            @Override
            public void action() {
                // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
                Transferable content = Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getContents(null);
                if (content != null) {
                    BibtexEntry[] bes = null;
                    if (content.isDataFlavorSupported(TransferableBibtexEntry.entryFlavor)) {
                        // We have determined that the clipboard data is a set of entries.
                        try {
                            bes = (BibtexEntry[]) (content.getTransferData(TransferableBibtexEntry.entryFlavor));

                        } catch (UnsupportedFlavorException ex) {
                            ex.printStackTrace();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        try {
                            BibtexParser bp = new BibtexParser
                                    (new java.io.StringReader((String) (content.getTransferData(
                                            DataFlavor.stringFlavor))));
                            BibtexDatabase db = bp.parse().getDatabase();
                            LOGGER.info("Parsed " + db.getEntryCount() + " entries from clipboard text");
                            if (db.getEntryCount() > 0) {
                                bes = db.getEntries().toArray(new BibtexEntry[db.getEntryCount()]);
                            }
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

                        // Store the first inserted bibtexentry.
                        // bes[0] does not work as bes[0] is first clonded,
                        // then inserted.
                        // This entry is used to open up an entry editor
                        // for the first inserted entry.
                        BibtexEntry firstBE = null;

                        for (BibtexEntry be1 : bes) {
                            try {
                                BibtexEntry be = (BibtexEntry) (be1.clone());
                                if (firstBE == null) {
                                    firstBE = be;
                                }
                                Util.setAutomaticFields(be,
                                        Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER),
                                        Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP));

                                // We have to clone the
                                // entries, since the pasted
                                // entries must exist
                                // independently of the copied
                                // ones.
                                be.setId(IdGenerator.next());
                                database.insertEntry(be);

                                ce.addEdit(new UndoableInsertEntry
                                        (database, be, BasePanel.this));
                            } catch (KeyCollisionException ex) {
                                LOGGER.info("KeyCollisionException... this shouldn't happen.", ex);
                            }
                        }
                        ce.end();
                        undoManager.addEdit(ce);
                        //entryTable.clearSelection();
                        //entryTable.revalidate();
                        output(Globals.lang("Pasted") + ' ' +
                                (bes.length > 1 ? bes.length + " " +
                                        Globals.lang("entries") : "1 " + Globals.lang("entry"))
                                + '.');
                        markBaseChanged();

                        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM)) {
                            selectionListener.editSignalled(firstBE);
                        }
                        highlightEntry(firstBE);
                    }
                }

            }

        });

        actions.put("selectAll", new BaseAction() {

            @Override
            public void action() {
                mainTable.selectAll();
            }
        });

        // The action for opening the preamble editor
        actions.put("editPreamble", new BaseAction() {

            @Override
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

            @Override
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

            @Override
            public void action() {
                sidePaneManager.toggle("groups");
                frame.groupToggle.setSelected(sidePaneManager.isComponentVisible("groups"));
            }
        });

        // The action for toggling the visibility of the toolbar
        actions.put("toggleToolbar", new BaseAction() {

            @Override
            public void action() {
                frame.tlb.setVisible(!frame.tlb.isVisible());
            }
        });

        // action for collecting database strings from user
        actions.put("dbConnect", new DbConnectAction(this));

        // action for exporting database to external SQL database
        actions.put("dbExport", new AbstractWorker() {

            String errorMessage = null;
            boolean connectToDB = false;


            // run first, in EDT:
            @Override
            public void init() {

                DBStrings dbs = metaData.getDBStrings();

                // get DBStrings from user if necessary
                if (!dbs.isConfigValid()) {

                    // init DB strings if necessary
                    if (!dbs.isInitialized()) {
                        dbs.initialize();
                    }

                    // show connection dialog
                    DBConnectDialog dbd = new DBConnectDialog(frame(), dbs);
                    Util.placeDialog(dbd, BasePanel.this);
                    dbd.setVisible(true);

                    connectToDB = dbd.getConnectToDB();

                    // store database strings
                    if (connectToDB) {
                        dbs = dbd.getDBStrings();
                        metaData.setDBStrings(dbs);
                        dbd.dispose();
                    }

                } else {

                    connectToDB = true;

                }

            }

            // run second, on a different thread:
            @Override
            public void run() {

                if (connectToDB) {

                    DBStrings dbs = metaData.getDBStrings();

                    try {
                        /*boolean okToExport = null!=metaData.getFile();
                        if (!okToExport)
                        {
                        	okToExport = false;
                        	int response = JOptionPane.showConfirmDialog(null, "You need to save your database in the disk \n" +
                        			"before saving. Save it now?", "Database is not saved",
                        	        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        		if(response == JOptionPane.YES_OPTION)
                        		{
                        			try {
                        				saveAction.saveAs();
                        				okToExport = (null!=metaData.getFile());
                        			} catch (Throwable e) {
                        			e.printStackTrace();
                        		}
                        	}
                        }
                        if (okToExport)
                        {*/
                        frame.output(Globals.lang("Attempting SQL export..."));
                        DBExporterAndImporterFactory factory = new DBExporterAndImporterFactory();
                        DBExporter exporter = factory.getExporter(dbs.getServerType());
                        exporter.exportDatabaseToDBMS(database, metaData, null, dbs, frame);
                        dbs.isConfigValid(true);
                        //}
                        //else
                        //	errorMessage = "Database was not exported. Your database must be saved \nbefore exporting to a SQL database";
                    } catch (Exception ex) {
                        String preamble = "Could not export to SQL database for the following reason:";
                        errorMessage = SQLUtil.getExceptionMessage(ex);
                        ex.printStackTrace();
                        dbs.isConfigValid(false);
                        JOptionPane.showMessageDialog(frame, Globals.lang(preamble)
                                + '\n' + errorMessage, Globals.lang("Export to SQL database"),
                                JOptionPane.ERROR_MESSAGE);
                    }

                    metaData.setDBStrings(dbs);

                }

            }

            // run third, on EDT:
            @Override
            public void update() {

                // if no error, report success
                if (errorMessage == null) {
                    if (connectToDB) {
                        frame.output(Globals.lang("%0 export successful"));
                    }
                }

                // show an error dialog if an error occurred
                else {

                    String preamble = "Could not export to SQL database for the following reason:";
                    frame.output(Globals.lang(preamble)
                            + "  " + errorMessage);

                    JOptionPane.showMessageDialog(frame, Globals.lang(preamble)
                            + '\n' + errorMessage, Globals.lang("Export to SQL database"),
                            JOptionPane.ERROR_MESSAGE);

                    errorMessage = null;

                }
            }

        });

        actions.put(FindUnlinkedFilesDialog.ACTION_COMMAND, new BaseAction() {

            @Override
            public void action() throws Throwable {
                FindUnlinkedFilesDialog dialog = new FindUnlinkedFilesDialog(frame, frame, BasePanel.this);
                Util.placeDialog(dialog, frame);
                dialog.setVisible(true);
            }
        });

        // The action for auto-generating keys.
        actions.put("makeKey", new AbstractWorker() {

            //int[] rows;
            List<BibtexEntry> entries;
            int numSelected;
            boolean cancelled = false;


            // Run first, in EDT:
            @Override
            public void init() {
                entries = new ArrayList<BibtexEntry>(Arrays.asList(getSelectedEntries()));
                //rows = entryTable.getSelectedRows() ;
                numSelected = entries.size();

                if (entries.isEmpty()) { // None selected. Inform the user to select entries first.
                    JOptionPane.showMessageDialog(frame, Globals.lang("First select the entries you want keys to be generated for."),
                            Globals.lang("Autogenerate BibTeX key"), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                frame.block();
                output(Globals.lang("Generating BibTeX key for") + ' ' +
                        numSelected + ' ' + (numSelected > 1 ? Globals.lang("entries")
                                : Globals.lang("entry")) + "...");
            }

            // Run second, on a different thread:
            @Override
            public void run() {
                BibtexEntry bes;
                NamedCompound ce = new NamedCompound(Globals.lang("autogenerate keys"));

                // First check if any entries have keys set already. If so, possibly remove
                // them from consideration, or warn about overwriting keys.
                for (Iterator<BibtexEntry> i = entries.iterator(); i.hasNext();) {
                    bes = i.next();
                    if (bes.getField(BibtexFields.KEY_FIELD) != null) {
                        if (Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
                            // Remove the entry, because its key is already set:
                            i.remove();
                        } else if (Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY)) {
                            // Ask if the user wants to cancel the operation:
                            CheckBoxMessage cbm = new CheckBoxMessage(Globals.lang("One or more keys will be overwritten. Continue?"),
                                    Globals.lang("Disable this confirmation dialog"), false);
                            int answer = JOptionPane.showConfirmDialog(frame, cbm, Globals.lang("Overwrite keys"),
                                    JOptionPane.YES_NO_OPTION);
                            if (cbm.isSelected()) {
                                Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, false);
                            }
                            if (answer == JOptionPane.NO_OPTION) {
                                // Ok, break off the operation.
                                cancelled = true;
                                return;
                            }
                            // No need to check more entries, because the user has already confirmed
                            // that it's ok to overwrite keys:
                            break;
                        }
                    }
                }

                HashMap<BibtexEntry, Object> oldvals = new HashMap<BibtexEntry, Object>();
                // Iterate again, removing already set keys. This is skipped if overwriting
                // is disabled, since all entries with keys set will have been removed.
                if (!Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
                    for (BibtexEntry entry : entries) {
                        bes = entry;
                        // Store the old value:
                        oldvals.put(bes, bes.getField(BibtexFields.KEY_FIELD));
                        database.setCiteKeyForEntry(bes.getId(), null);
                    }
                }

                // Finally, set the new keys:
                for (BibtexEntry entry : entries) {
                    bes = entry;
                    bes = LabelPatternUtil.makeLabel(metaData, database, bes);
                    ce.addEdit(new UndoableKeyChange
                            (database, bes.getId(), (String) oldvals.get(bes),
                                    bes.getField(BibtexFields.KEY_FIELD)));
                }
                ce.end();
                undoManager.addEdit(ce);
            }

            // Run third, on EDT:
            @Override
            public void update() {
                database.setFollowCrossrefs(true);
                if (cancelled) {
                    frame.unblock();
                    return;
                }
                markBaseChanged();
                numSelected = entries.size();

                ////////////////////////////////////////////////////////////////////////////////
                //          Prevent selection loss for autogenerated BibTeX-Keys
                ////////////////////////////////////////////////////////////////////////////////
                for (final BibtexEntry bibEntry : entries) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            final int row = mainTable.findEntry(bibEntry);
                            if ((row >= 0) && (mainTable.getSelectedRowCount() < entries.size())) {
                                mainTable.addRowSelectionInterval(row, row);
                            }
                        }
                    });
                }
                ////////////////////////////////////////////////////////////////////////////////
                output(Globals.lang("Generated BibTeX key for") + ' ' +
                        numSelected + ' ' + (numSelected != 1 ? Globals.lang("entries")
                                : Globals.lang("entry")));
                frame.unblock();
            }
        });

        // The action for cleaning up entry.
        actions.put("Cleanup", cleanUpAction);

        actions.put("mergeEntries", new BaseAction() {

            @Override
            public void action() {
                new MergeEntriesDialog(BasePanel.this);
            }
        });

        actions.put("search", new BaseAction() {
        	
        	@Override
            public void action() {
                frame.setSearchBarVisible(true);
                frame.getSearchBar().startSearch();
            }
        });
        
        actions.put("toggleSearch", new BaseAction() {
        	
        	@Override
            public void action() {
        		frame.setSearchBarVisible(! frame.searchBar.isVisible());
	            if (frame.searchBar.isVisible()) {
	            	frame.getSearchBar().startSearch();
	            }
            }
        });

        actions.put("incSearch", new BaseAction() {
			
        	@Override    
			public void action() {
				frame.setSearchBarVisible(true);
				frame.getSearchBar().startIncrementalSearch();
            }
        });

        // The action for copying the selected entry's key.
        actions.put("copyKey", new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length > 0)) {
                    storeCurrentEdit();
                    //String[] keys = new String[bes.length];
                    Vector<Object> keys = new Vector<Object>();
                    // Collect all non-null keys.
                    for (BibtexEntry be : bes) {
                        if (be.getField(BibtexFields.KEY_FIELD) != null) {
                            keys.add(be.getField(BibtexFields.KEY_FIELD));
                        }
                    }
                    if (keys.isEmpty()) {
                        output("None of the selected entries have BibTeX keys.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder((String) keys.elementAt(0));
                    for (int i = 1; i < keys.size(); i++) {
                        sb.append(',');
                        sb.append((String) keys.elementAt(i));
                    }

                    StringSelection ss = new StringSelection(sb.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(ss, BasePanel.this);

                    if (keys.size() == bes.length) {
                        // All entries had keys.
                        output(Globals.lang((bes.length > 1) ? "Copied keys"
                                : "Copied key") + '.');
                    } else {
                        output(Globals.lang("Warning") + ": " + (bes.length - keys.size())
                                + ' ' + Globals.lang("out of") + ' ' + bes.length + ' ' +
                                Globals.lang("entries have undefined BibTeX key") + '.');
                    }
                }
            }
        });

        // The action for copying a cite for the selected entry.
        actions.put("copyCiteKey", new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length > 0)) {
                    storeCurrentEdit();
                    //String[] keys = new String[bes.length];
                    Vector<Object> keys = new Vector<Object>();
                    // Collect all non-null keys.
                    for (BibtexEntry be : bes) {
                        if (be.getField(BibtexFields.KEY_FIELD) != null) {
                            keys.add(be.getField(BibtexFields.KEY_FIELD));
                        }
                    }
                    if (keys.isEmpty()) {
                        output("None of the selected entries have BibTeX keys.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder((String) keys.elementAt(0));
                    for (int i = 1; i < keys.size(); i++) {
                        sb.append(',');
                        sb.append((String) keys.elementAt(i));
                    }

                    StringSelection ss = new StringSelection
                            ("\\cite{" + sb.toString() + '}');
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(ss, BasePanel.this);

                    if (keys.size() == bes.length) {
                        // All entries had keys.
                        output(bes.length > 1 ? Globals.lang("Copied keys")
                                : Globals.lang("Copied key") + '.');
                    } else {
                        output(Globals.lang("Warning") + ": " + (bes.length - keys.size())
                                + ' ' + Globals.lang("out of") + ' ' + bes.length + ' ' +
                                Globals.lang("entries have undefined BibTeX key") + '.');
                    }
                }
            }
        });

        // The action for copying the BibTeX key and the title for the first selected entry
        actions.put("copyKeyAndTitle", new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length > 0)) {
                    storeCurrentEdit();

                    // OK: in a future version, this string should be configurable to allow arbitrary exports
                    StringReader sr = new StringReader("\\bibtexkey - \\begin{title}\\format[RemoveBrackets]{\\title}\\end{title}\n");
                    Layout layout;
                    try {
                        layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    StringBuilder sb = new StringBuilder();

                    int copied = 0;
                    // Collect all non-null keys.
                    for (BibtexEntry be : bes) {
                        if (be.getField(BibtexFields.KEY_FIELD) != null) {
                            copied++;
                            sb.append(layout.doLayout(be, database));
                        }
                    }

                    if (copied == 0) {
                        output("None of the selected entries have BibTeX keys.");
                        return;
                    }

                    StringSelection ss = new StringSelection(sb.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(ss, BasePanel.this);

                    if (copied == bes.length) {
                        // All entries had keys.
                        output(Globals.lang((bes.length > 1) ? "Copied keys"
                                : "Copied key") + '.');
                    } else {
                        output(Globals.lang("Warning") + ": " + (copied)
                                + ' ' + Globals.lang("out of") + ' ' + bes.length + ' ' +
                                Globals.lang("entries have undefined BibTeX key") + '.');
                    }
                }
            }
        });

        actions.put("mergeDatabase", new AppendDatabaseAction(frame, this));

        actions.put("openFile", new BaseAction() {

            @Override
            public void action() {
                JabRefExecutorService.INSTANCE.execute(new Runnable() {

                    @Override
                    public void run() {
                        BibtexEntry[] bes = mainTable.getSelectedEntries();
                        String field = "ps";

                        if ((bes != null) && (bes.length == 1)) {
                            FileListEntry entry = null;
                            FileListTableModel tm = new FileListTableModel();
                            tm.setContent(bes[0].getField("file"));
                            for (int i = 0; i < tm.getRowCount(); i++) {
                                FileListEntry flEntry = tm.getEntry(i);
                                if (flEntry.getType().getName().toLowerCase().equals("pdf")
                                        || flEntry.getType().getName().toLowerCase().equals("ps")) {
                                    entry = flEntry;
                                    break;
                                }
                            }
                            if (entry != null) {
                                try {
                                    Util.openExternalFileAnyFormat(metaData, entry.getLink(), entry.getType());
                                    output(Globals.lang("External viewer called") + '.');
                                } catch (IOException e) {
                                    output(Globals.lang("Could not open link"));
                                    e.printStackTrace();
                                }
                                return;
                            }
                            // If we didn't find anything in the "file" field, check "ps" and "pdf" fields:
                            Object link = bes[0].getField("ps");
                            if (bes[0].getField("pdf") != null) {
                                link = bes[0].getField("pdf");
                                field = "pdf";
                            }
                            String filepath = null;
                            if (link != null) {
                                filepath = link.toString();
                            } else {
                                if (Globals.prefs.getBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH)) {

                                    /*  The search can lead to an unexpected 100% CPU usage which is perceived
                                        as a bug, if the search incidentally starts at a directory with lots
                                        of stuff below. It is now disabled by default. */

                                    // see if we can fall back to a filename based on the bibtex key
                                    final Collection<BibtexEntry> entries = new ArrayList<BibtexEntry>();
                                    entries.add(bes[0]);
                                    ExternalFileType[] types = Globals.prefs.getExternalFileTypeSelection();
                                    ArrayList<File> dirs = new ArrayList<File>();
                                    if (metaData.getFileDirectory(GUIGlobals.FILE_FIELD).length > 0) {
                                        String[] mdDirs = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
                                        for (String mdDir : mdDirs) {
                                            dirs.add(new File(mdDir));

                                        }
                                    }
                                    Collection<String> extensions = new ArrayList<String>();
                                    for (final ExternalFileType type : types) {
                                        extensions.add(type.getExtension());
                                    }
                                    // Run the search operation:
                                    Map<BibtexEntry, List<File>> result;
                                    if (Globals.prefs.getBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY)) {
                                        String regExp = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
                                        result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs, regExp);
                                    } else {
                                        result = Util.findAssociatedFiles(entries, extensions, dirs);
                                    }
                                    if (result.get(bes[0]) != null) {
                                        List<File> res = result.get(bes[0]);
                                        if (!res.isEmpty()) {
                                            filepath = res.get(0).getPath();
                                            int index = filepath.lastIndexOf('.');
                                            if ((index >= 0) && (index < (filepath.length() - 1))) {
                                                String extension = filepath.substring(index + 1);
                                                ExternalFileType type = Globals.prefs.getExternalFileTypeByExt(extension);
                                                if (type != null) {
                                                    try {
                                                        Util.openExternalFileAnyFormat(metaData, filepath, type);
                                                        output(Globals.lang("External viewer called") + '.');
                                                        return;
                                                    } catch (IOException ex) {
                                                        output(Globals.lang("Error") + ": " + ex.getMessage());
                                                    }
                                                }
                                            }

                                            // TODO: add code for opening the file
                                        }
                                    }
                                    /*String basefile;
                                    Object key = bes[0].getField(BibtexFields.KEY_FIELD);
                                    if (key != null) {
                                        basefile = key.toString();
                                        final ExternalFileType[] types = Globals.prefs.getExternalFileTypeSelection();
                                        final String sep = System.getProperty("file.separator");
                                        String dir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
                                        if ((dir != null) && (dir.length() > 0)) {
                                            if (dir.endsWith(sep)) {
                                                dir = dir.substring(0, dir.length() - sep.length());
                                            }
                                            for (int i = 0; i < types.length; i++) {
                                                String found = Util.findPdf(basefile, types[i].getExtension(),
                                                        dir, new OpenFileFilter("." + types[i].getExtension()));
                                                if (found != null) {
                                                    filepath = dir + sep + found;
                                                    break;
                                                }
                                            }
                                        }
                                    }*/
                                }
                            }

                            if (filepath != null) {
                                //output(Globals.lang("Calling external viewer..."));
                                try {
                                    Util.openExternalViewer(metaData(), filepath, field);
                                    output(Globals.lang("External viewer called") + '.');
                                } catch (IOException ex) {
                                    output(Globals.lang("Error") + ": " + ex.getMessage());
                                }
                            } else {
                                output(Globals.lang(
                                        "No pdf or ps defined, and no file matching Bibtex key found") +
                                        '.');
                            }
                        } else {
                            output(Globals.lang("No entries or multiple entries selected."));
                        }
                    }
                });
            }
        });

        actions.put("addFileLink", new AttachFileAction(this));

        actions.put("openExternalFile", new BaseAction() {

            @Override
            public void action() {
                JabRefExecutorService.INSTANCE.execute(new Runnable() {

                    @Override
                    public void run() {
                        BibtexEntry[] bes = mainTable.getSelectedEntries();
                        String field = GUIGlobals.FILE_FIELD;
                        if ((bes != null) && (bes.length == 1)) {
                            Object link = bes[0].getField(field);
                            if (link == null) {
                                runCommand("openFile"); // Fall back on PDF/PS fields???
                                return;
                            }
                            FileListTableModel tableModel = new FileListTableModel();
                            tableModel.setContent((String) link);
                            if (tableModel.getRowCount() == 0) {
                                runCommand("openFile"); // Fall back on PDF/PS fields???
                                return;
                            }
                            FileListEntry flEntry = tableModel.getEntry(0);
                            ExternalFileMenuItem item = new ExternalFileMenuItem
                                    (frame(), bes[0], "",
                                            flEntry.getLink(), flEntry.getType().getIcon(),
                                            metaData(), flEntry.getType());
                            item.openLink();
                        } else {
                            output(Globals.lang("No entries or multiple entries selected."));
                        }
                    }
                });
            }
        });

        actions.put("openFolder", new BaseAction() {

            @Override
            public void action() {
                JabRefExecutorService.INSTANCE.execute(new Runnable() {

                    @Override
                    public void run() {
                        BibtexEntry[] bes = mainTable.getSelectedEntries();
                        List<File> files = Util.getListOfLinkedFiles(bes, metaData().getFileDirectory(GUIGlobals.FILE_FIELD));
                        for (File f : files) {
                            try {
                                Util.openFolderAndSelectFile(f.getAbsolutePath());
                            } catch (IOException e) {
                                LOGGER.info("Could not open folder", e);
                            }
                        }
                    }
                });
            }
        });

        actions.put("openUrl", new BaseAction() {

            @Override
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
                            output(Globals.lang("External viewer called") + '.');
                        } catch (IOException ex) {
                            output(Globals.lang("Error") + ": " + ex.getMessage());
                        }
                    }
                    else {
                        // No URL or DOI found in the "url" and "doi" fields.
                        // Look for web links in the "file" field as a fallback:
                        FileListEntry entry = null;
                        FileListTableModel tm = new FileListTableModel();
                        tm.setContent(bes[0].getField("file"));
                        for (int i = 0; i < tm.getRowCount(); i++) {
                            FileListEntry flEntry = tm.getEntry(i);
                            if (flEntry.getType().getName().toLowerCase().equals("url")
                                    || flEntry.getType().getName().toLowerCase().equals("ps")) {
                                entry = flEntry;
                                break;
                            }
                        }
                        if (entry != null) {
                            try {
                                Util.openExternalFileAnyFormat(metaData, entry.getLink(), entry.getType());
                                output(Globals.lang("External viewer called") + '.');
                            } catch (IOException e) {
                                output(Globals.lang("Could not open link"));
                                e.printStackTrace();
                            }
                        } else {
                            output(Globals.lang("No url defined") + '.');
                        }
                    }
                } else {
                    output(Globals.lang("No entries or multiple entries selected."));
                }
            }
        });

        actions.put("openSpires", new BaseAction() {

            @Override
            public void action() {
                BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length == 1)) {
                    Object link = null;
                    if (bes[0].getField("eprint") != null) {
                        link = SPIRESFetcher.constructUrlFromEprint(bes[0].getField("eprint"));
                    } else if (bes[0].getField("slaccitation") != null) {
                        link = SPIRESFetcher.constructUrlFromSlaccitation(bes[0].getField("slaccitation"));
                    }
                    if (link != null) {
                        //output(Globals.lang("Calling external viewer..."));
                        try {
                            Util.openExternalViewer(metaData(), link.toString(), "url");
                            output(Globals.lang("External viewer called") + '.');
                        } catch (IOException ex) {
                            output(Globals.lang("Error") + ": " + ex.getMessage());
                        }
                    } else {
                        output(Globals.lang("No url defined") + '.');
                    }
                } else {
                    output(Globals.lang("No entries or multiple entries selected."));
                }
            }
        });

        /*
         *  It looks like this action was not being supported for SPIRES anyway
         *  so we don't bother to implement it.
        actions.put("openInspire", new BaseAction() {
        	public void action() {
        		BibtexEntry[] bes = mainTable.getSelectedEntries();
                if ((bes != null) && (bes.length == 1)) {
                	Object link = null;
                    if (bes[0].getField("eprint") != null)
                      link = INSPIREFetcher.constructUrlFromEprint(bes[0].getField("eprint").toString());
                    else if (bes[0].getField("slaccitation") != null)
                        link = INSPIREFetcher.constructUrlFromSlaccitation(bes[0].getField("slaccitation").toString());
                    if (link != null) {
                      //output(Globals.lang("Calling external viewer..."));
                      try {
                        Util.openExternalViewer(metaData(), link.toString(), "url");
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
        	*/

        actions.put("replaceAll", new BaseAction() {

            @Override
            public void action() {
                ReplaceStringDialog rsd = new ReplaceStringDialog(frame);
                rsd.setVisible(true);
                if (!rsd.okPressed()) {
                    return;
                }
                int counter = 0;
                NamedCompound ce = new NamedCompound(Globals.lang("Replace string"));
                if (!rsd.selOnly()) {
                    for (BibtexEntry entry : database.getEntries()) {
                        counter += rsd.replace(entry, ce);
                    }
                } else {
                    BibtexEntry[] bes = mainTable.getSelectedEntries();
                    for (BibtexEntry be : bes) {
                        counter += rsd.replace(be, ce);
                    }
                }

                output(Globals.lang("Replaced") + ' ' + counter + ' ' +
                        Globals.lang(counter == 1 ? "occurence" : "occurences") + '.');
                if (counter > 0) {
                    ce.end();
                    undoManager.addEdit(ce);
                    markBaseChanged();
                }
            }
        });

        actions.put("dupliCheck", new BaseAction() {

            @Override
            public void action() {
                JabRefExecutorService.INSTANCE.execute(new DuplicateSearch(BasePanel.this));
            }
        });

        actions.put("plainTextImport", new BaseAction() {

            @Override
            public void action()
            {
                // get Type of new entry
                EntryTypeDialog etd = new EntryTypeDialog(frame);
                Util.placeDialog(etd, BasePanel.this);
                etd.setVisible(true);
                BibtexEntryType tp = etd.getChoice();
                if (tp == null) {
                    return;
                }

                String id = IdGenerator.next();
                BibtexEntry bibEntry = new BibtexEntry(id, tp);
                TextInputDialog tidialog = new TextInputDialog(frame, BasePanel.this,
                        "import", true,
                        bibEntry);
                Util.placeDialog(tidialog, BasePanel.this);
                tidialog.setVisible(true);

                if (tidialog.okPressed())
                {
                    Util.setAutomaticFields(Collections.singletonList(bibEntry),
                            false, false, false);
                    insertEntry(bibEntry);
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
        actions.put("markEntries", new MarkEntriesAction(frame, 0));

        actions.put("unmarkEntries", new BaseAction() {

            @Override
            public void action() {
                try {
                    BibtexEntry[] bes = mainTable.getSelectedEntries();
                    if (bes.length == 0) {
                        output(Globals.lang("No entries selected."));
                        return;
                    }
                    NamedCompound ce = new NamedCompound(Globals.lang("Unmark entries"));
                    for (BibtexEntry be : bes) {
                        EntryMarker.unmarkEntry(be, false, database, ce);
                    }
                    ce.end();
                    undoManager.addEdit(ce);
                    markBaseChanged();
                    String outputStr;
                    if (bes.length == 1) {
                        outputStr = Globals.lang("Unmarked selected entry");
                    } else {
                        outputStr = Globals.lang("Unmarked all %0 selected entries", Integer.toString(bes.length));
                    }
                    output(outputStr);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });

        actions.put("unmarkAll", new BaseAction() {

            @Override
            public void action() {
                NamedCompound ce = new NamedCompound(Globals.lang("Unmark all"));

                for (BibtexEntry be : database.getEntries()) {
                    EntryMarker.unmarkEntry(be, false, database, ce);
                }
                ce.end();
                undoManager.addEdit(ce);
                markBaseChanged();
                output(Globals.lang("Unmarked all entries"));
            }
        });

        // Note that we can't put the number of entries that have been reverted into the undoText as the concrete number cannot be injected
        actions.put(Relevance.getInstance().getValues().get(0).getActionName(),
                new SpecialFieldAction(frame, Relevance.getInstance(), Relevance.getInstance().getValues().get(0).getFieldValue(), true, Globals.lang("Toggle relevance"), Globals.lang("Toggled relevance for %0 entries")));
        actions.put(Quality.getInstance().getValues().get(0).getActionName(),
                new SpecialFieldAction(frame, Quality.getInstance(), Quality.getInstance().getValues().get(0).getFieldValue(), true, Globals.lang("Toggle quality"), Globals.lang("Toggled quality for %0 entries")));
        actions.put(Printed.getInstance().getValues().get(0).getActionName(),
                new SpecialFieldAction(frame, Printed.getInstance(), Printed.getInstance().getValues().get(0).getFieldValue(), true, Globals.lang("Toggle print status"), Globals.lang("Toggled print status for %0 entries")));

        for (SpecialFieldValue prio : Priority.getInstance().getValues()) {
            actions.put(prio.getActionName(), prio.getAction(this.frame));
        }
        for (SpecialFieldValue rank : Rank.getInstance().getValues()) {
            actions.put(rank.getActionName(), rank.getAction(this.frame));
        }
        for (SpecialFieldValue status : ReadStatus.getInstance().getValues()) {
            actions.put(status.getActionName(), status.getAction(this.frame));
        }

        actions.put("togglePreview", new BaseAction() {

            @Override
            public void action() {
                boolean enabled = !Globals.prefs.getBoolean(JabRefPreferences.PREVIEW_ENABLED);
                Globals.prefs.putBoolean(JabRefPreferences.PREVIEW_ENABLED, enabled);
                frame.setPreviewActive(enabled);
                frame.previewToggle.setSelected(enabled);
            }
        });

        actions.put("toggleHighlightGroupsMatchingAny", new BaseAction() {

            @Override
            public void action() {
                boolean enabled = !Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY);
                Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY, enabled);
                frame.highlightAny.setSelected(enabled);
                if (enabled) {
                    frame.highlightAll.setSelected(false);
                    Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL, false);
                }
                // ping the listener so it updates:
                groupsHighlightListener.listChanged(null);
            }
        });

        actions.put("toggleHighlightGroupsMatchingAll", new BaseAction() {

            @Override
            public void action() {
                boolean enabled = !Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL);
                Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL, enabled);
                frame.highlightAll.setSelected(enabled);
                if (enabled) {
                    frame.highlightAny.setSelected(false);
                    Globals.prefs.putBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY, false);
                }
                // ping the listener so it updates:
                groupsHighlightListener.listChanged(null);
            }
        });

        actions.put("switchPreview", new BaseAction() {

            @Override
            public void action() {
                selectionListener.switchPreview();
            }
        });

        actions.put("manageSelectors", new BaseAction() {

            @Override
            public void action() {
                ContentSelectorDialog2 csd = new ContentSelectorDialog2
                        (frame, frame, BasePanel.this, false, metaData, null);
                Util.placeDialog(csd, frame);
                csd.setVisible(true);
            }
        });

        actions.put("exportToClipboard", new ExportToClipboardAction(frame, database()));
        actions.put("sendAsEmail", new SendAsEMailAction(frame));

        actions.put("writeXMP", new WriteXMPAction(this));

        actions.put("abbreviateIso", new AbbreviateAction(this, true));
        actions.put("abbreviateMedline", new AbbreviateAction(this, false));
        actions.put("unabbreviate", new UnabbreviateAction(this));
        actions.put("autoSetPdf", new AutoSetExternalFileForEntries(this, "pdf"));
        actions.put("autoSetPs", new AutoSetExternalFileForEntries(this, "ps"));
        actions.put("autoSetFile", new SynchronizeFileField(this));

        actions.put("back", new BaseAction() {

            @Override
            public void action() throws Throwable {
                back();
            }
        });
        actions.put("forward", new BaseAction() {

            @Override
            public void action() throws Throwable {
                forward();
            }
        });

        actions.put("resolveDuplicateKeys", new SearchFixDuplicateLabels(this));

        actions.put("addToGroup", new GroupAddRemoveDialog(this, true, false));
        actions.put("removeFromGroup", new GroupAddRemoveDialog(this, false, false));
        actions.put("moveToGroup", new GroupAddRemoveDialog(this, true, true));

        //actions.put("downloadFullText", new FindFullTextAction(this));
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
        //(new Thread() {
        //  public void run() {
        if (actions.get(_command) == null) {
            LOGGER.info("No action defined for '" + _command + '\'');
        } else {
            Object o = actions.get(_command);
            try {
                if (o instanceof BaseAction) {
                    ((BaseAction) o).action();
                } else {
                    // This part uses Spin's features:
                    Worker wrk = ((AbstractWorker) o).getWorker();
                    // The Worker returned by getWorker() has been wrapped
                    // by Spin.off(), which makes its methods be run in
                    // a different thread from the EDT.
                    CallBack clb = ((AbstractWorker) o).getCallBack();

                    ((AbstractWorker) o).init(); // This method runs in this same thread, the EDT.
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

    private boolean saveDatabase(File file, boolean selectedOnly, String encoding, FileActions.DatabaseSaveType saveType) throws SaveException {
        SaveSession session;
        frame.block();
        try {
            if (!selectedOnly) {
                session = FileActions.saveDatabase(database, metaData, file,
                        Globals.prefs, false, false, encoding, false);
            } else {
                session = FileActions.savePartOfDatabase(database, metaData, file,
                        Globals.prefs, mainTable.getSelectedEntries(), encoding, saveType);
            }

        } catch (UnsupportedCharsetException ex2) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Could not save file. "
                    + "Character encoding '%0' is not supported.", encoding),
                    Globals.lang("Save database"), JOptionPane.ERROR_MESSAGE);
            throw new SaveException("rt");
        } catch (SaveException ex) {
            if (ex.specificEntry()) {
                // Error occured during processing of
                // be. Highlight it:
                int row = mainTable.findEntry(ex.getEntry()), topShow = Math.max(0, row - 3);
                mainTable.setRowSelectionInterval(row, row);
                mainTable.scrollTo(topShow);
                showEntry(ex.getEntry());
            } else {
                ex.printStackTrace();
            }

            JOptionPane.showMessageDialog
                    (frame, Globals.lang("Could not save file")
                            + ".\n" + ex.getMessage(),
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
                    String newEncoding = (String) choice;
                    return saveDatabase(file, selectedOnly, newEncoding, saveType);
                } else {
                    commit = false;
                }
            } else if (answer == JOptionPane.CANCEL_OPTION) {
                commit = false;
            }

        }

        if (commit) {
            session.commit();
            this.encoding = encoding; // Make sure to remember which encoding we used.
        } else {
            session.cancel();
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
            String id = IdGenerator.next();
            final BibtexEntry be = new BibtexEntry(id, type);
            try {
                database.insertEntry(be);
                // Set owner/timestamp if options are enabled:
                ArrayList<BibtexEntry> list = new ArrayList<BibtexEntry>();
                list.add(be);
                Util.setAutomaticFields(list, true, true, false);

                // Create an UndoableInsertEntry object.
                undoManager.addEdit(new UndoableInsertEntry(database, be, BasePanel.this));
                output(Globals.lang("Added new") + " '" + type.getName().toLowerCase() + "' "
                        + Globals.lang("entry") + '.');

                // We are going to select the new entry. Before that, make sure that we are in
                // show-entry mode. If we aren't already in that mode, enter the WILL_SHOW_EDITOR
                // mode which makes sure the selection will trigger display of the entry editor
                // and adjustment of the splitter.
                if (mode != BasePanel.SHOWING_EDITOR) {
                    mode = BasePanel.WILL_SHOW_EDITOR;
                }

                int row = mainTable.findEntry(be);
                if (row >= 0) {
                    highlightEntry(be); // Selects the entry. The selection listener will open the editor.
                } else {
                    // The entry is not visible in the table, perhaps due to a filtering search
                    // or group selection. Show the entry editor anyway:
                    showEntry(be);
                }

                markBaseChanged(); // The database just changed.
                new FocusRequester(getEntryEditor(be));

                return be;
            } catch (KeyCollisionException ex) {
                LOGGER.info(ex.getMessage(), ex);
            }
        }
        return null;
    }


    /**
     * This listener is used to add a new entry to a group (or a set of groups)
     * in case the Group View is selected and one or more groups are marked
     */
    private class GroupTreeUpdater implements DatabaseChangeListener {

        @Override
        public void databaseChanged(DatabaseChangeEvent e) {
            if ((e.getType() == ChangeType.ADDED_ENTRY)
                    && (Globals.prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP))
                    && (frame.groupToggle.isSelected())) {
                BibtexEntry[] entries = {e.getEntry()};
                TreePath[] selection = frame.groupSelector.getGroupsTree().getSelectionPaths();
                if (selection != null) {
                    // it is possible that the user selected nothing. Therefore, checked for "!= null"
                    for (TreePath tree : selection) {
                        ((GroupTreeNode) (tree.getLastPathComponent())).addToGroup(entries);
                    }
                }
                //BasePanel.this.updateEntryEditorIfShowing(); // doesn't seem to be necessary
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        BasePanel.this.getGroupSelector().valueChanged(null);
                    }
                });
            }
        }
    }

    /**
     * Ensures that the search auto completer is up to date when entries are changed
     * AKA Let the auto completer, if any, harvest words from the entry
     */
    private class SearchAutoCompleterUpdater implements DatabaseChangeListener {

        @Override
        public void databaseChanged(DatabaseChangeEvent e) {
            if ((e.getType() == ChangeType.CHANGED_ENTRY) || (e.getType() == ChangeType.ADDED_ENTRY)) {
                searchAutoCompleter.addBibtexEntry(e.getEntry());
            }
        }
    }

    /**
     * Ensures that auto completers are up to date when entries are changed
     * AKA Let the auto completer, if any, harvest words from the entry
     */
    private class AutoCompletersUpdater implements DatabaseChangeListener {

        @Override
        public void databaseChanged(DatabaseChangeEvent e) {
            if ((e.getType() == ChangeType.CHANGED_ENTRY) || (e.getType() == ChangeType.ADDED_ENTRY)) {
                BasePanel.this.autoCompleters.addEntry(e.getEntry());
            }
        }
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
                database.insertEntry(bibEntry);
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER)) {
                    // Set owner field to default value
                    Util.setAutomaticFields(bibEntry, true, true);
                }
                // Create an UndoableInsertEntry object.
                undoManager.addEdit(new UndoableInsertEntry(database, bibEntry, BasePanel.this));
                output(Globals.lang("Added new") + " '"
                        + bibEntry.getType().getName().toLowerCase() + "' "
                        + Globals.lang("entry") + '.');

                markBaseChanged(); // The database just changed.
                if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_OPEN_FORM))
                {
                    selectionListener.editSignalled(bibEntry);
                }
                highlightEntry(bibEntry);
            } catch (KeyCollisionException ex) {
                LOGGER.info("Collision for bibtex key" + bibEntry.getId(), ex);
            }
        }
    }

    public void updateTableFont() {
        mainTable.updateFont();
    }

    private void createMainTable() {
        //Comparator comp = new FieldComparator("author");

        GlazedEntrySorter eventList = new GlazedEntrySorter(database.getEntryMap());
        // Must initialize sort columns somehow:

        database.addDatabaseChangeListener(eventList);
        database.addDatabaseChangeListener(SpecialFieldDatabaseChangeListener.getInstance());
        groupFilterList = new FilterList<BibtexEntry>(eventList.getTheList(), NoSearchMatcher.INSTANCE);
        searchFilterList = new FilterList<BibtexEntry>(groupFilterList, NoSearchMatcher.INSTANCE);
        //final SortedList sortedList = new SortedList(searchFilterList, null);
        tableFormat = new MainTableFormat(this);
        tableFormat.updateTableFormat();
        //EventTableModel tableModel = new EventTableModel(sortedList, tableFormat);
        mainTable = new MainTable(tableFormat, searchFilterList, frame, this);

        selectionListener = new MainTableSelectionListener(this, mainTable);
        mainTable.updateFont();
        mainTable.addSelectionListener(selectionListener);
        mainTable.addMouseListener(selectionListener);
        mainTable.addKeyListener(selectionListener);
        mainTable.addFocusListener(selectionListener);

        // Add the listener that will take care of highlighting groups as the selection changes:
        groupsHighlightListener = new ListEventListener<BibtexEntry>() {

            @Override
            public void listChanged(ListEvent<BibtexEntry> listEvent) {
                if (Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ANY)) {
                    getGroupSelector().showMatchingGroups(
                            mainTable.getSelectedEntries(), false);
                } else if (Globals.prefs.getBoolean(JabRefPreferences.HIGHLIGHT_GROUPS_MATCHING_ALL)) {
                    getGroupSelector().showMatchingGroups(
                            mainTable.getSelectedEntries(), true);
                } else {
                    // no highlight
                    getGroupSelector().showMatchingGroups(null, true);
                }
            }
        };
        mainTable.addSelectionListener(groupsHighlightListener);

        mainTable.getActionMap().put("cut", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand("cut");
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });
        mainTable.getActionMap().put("copy", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand("copy");
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });
        mainTable.getActionMap().put("paste", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    runCommand("paste");
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });

        mainTable.addKeyListener(new KeyAdapter() {

            @Override
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
                        if (node != null) {
                            frame.groupSelector.moveNodeUp(node, true);
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        e.consume();
                        if (node != null) {
                            frame.groupSelector.moveNodeDown(node, true);
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        e.consume();
                        if (node != null) {
                            frame.groupSelector.moveNodeLeft(node, true);
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        e.consume();
                        if (node != null) {
                            frame.groupSelector.moveNodeRight(node, true);
                        }
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
                } else if (keyCode == KeyEvent.VK_ENTER) {
                    e.consume();
                    try {
                        runCommand("edit");
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

        for (EntryEditor ee : entryEditors.values()) {
            ee.validateAllFields();
        }

        splitPane.setTopComponent(mainTable.getPane());

        // Remove borders
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        setBorder(BorderFactory.createEmptyBorder());

        //setupTable();
        // If an entry is currently being shown, make sure it stays shown,
        // otherwise set the bottom component to null.
        if (mode == BasePanel.SHOWING_PREVIEW) {
            mode = BasePanel.SHOWING_NOTHING;
            int row = mainTable.findEntry(currentPreview.entry);
            if (row >= 0) {
                mainTable.setRowSelectionInterval(row, row);
            }

        }
        else if (mode == BasePanel.SHOWING_EDITOR) {
            mode = BasePanel.SHOWING_NOTHING;
            /*int row = mainTable.findEntry(currentEditor.entry);
            if (row >= 0)
                mainTable.setRowSelectionInterval(row, row);
            */
            //showEntryEditor(currentEditor);
        } else {
            splitPane.setBottomComponent(null);
        }

        setLayout(new BorderLayout());
        removeAll();
        add(splitPane, BorderLayout.CENTER);

        // Set up name autocompleter for search:
        //if (!Globals.prefs.getBoolean("searchAutoComplete")) {
        instantiateSearchAutoCompleter();
        this.getDatabase().addDatabaseChangeListener(new SearchAutoCompleterUpdater());

        // Set up AutoCompleters for this panel:
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_COMPLETE)) {
            autoCompleters = new ContentAutoCompleters(getDatabase(), metaData);
            // ensure that the autocompleters are in sync with entries
            this.getDatabase().addDatabaseChangeListener(new AutoCompletersUpdater());
        }

        splitPane.revalidate();
        revalidate();
        repaint();
    }

    public void updateSearchManager() {
        frame.getSearchBar().setAutoCompleter(searchAutoCompleter);
    }

    private void instantiateSearchAutoCompleter() {
        searchAutoCompleter = AutoCompleterFactory.getFor("author", "editor");
        for (BibtexEntry entry : database.getEntries()) {
            searchAutoCompleter.addBibtexEntry(entry);
        }
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
        if (preambleEditor != null) {
            preambleEditor.updatePreamble();
        }
    }

    public void assureStringDialogNotEditing() {
        if (stringDialog != null) {
            stringDialog.assureNotEditing();
        }
    }

    public void updateStringDialog() {
        if (stringDialog != null) {
            stringDialog.refreshTable();
        }
    }

    public void updateEntryPreviewToRow(BibtexEntry e) {

    }

    public void adjustSplitter() {
        int mode = getMode();
        if (mode == BasePanel.SHOWING_PREVIEW) {
            splitPane.setDividerLocation(splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT));
        } else {
            splitPane.setDividerLocation(splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT));

        }
    }

    /**
     * Stores the source view in the entry editor, if one is open, has the source view
     * selected and the source has been edited.
     * @return boolean false if there is a validation error in the source panel, true otherwise.
     */
    public boolean entryEditorAllowsChange() {
        Component c = splitPane.getBottomComponent();
        if ((c instanceof EntryEditor)) {
            return ((EntryEditor) c).lastSourceAccepted();
        } else {
            return true;
        }
    }

    public void moveFocusToEntryEditor() {
        Component c = splitPane.getBottomComponent();
        if ((c instanceof EntryEditor)) {
            new FocusRequester(c);
        }
    }

    private boolean isShowingEditor() {
        return ((splitPane.getBottomComponent() != null)
        && (splitPane.getBottomComponent() instanceof EntryEditor));
    }

    public void showEntry(final BibtexEntry be) {

        if (getShowing() == be) {
            if (splitPane.getBottomComponent() == null) {
                // This is the special occasion when showing is set to an
                // entry, but no entry editor is in fact shown. This happens
                // after Preferences dialog is closed, and it means that we
                // must make sure the same entry is shown again. We do this by
                // setting showing to null, and recursively calling this method.
                newEntryShowing(null);
                showEntry(be);
            } else {
                // The correct entry is already being shown. Make sure the editor
                // is updated.
                ((EntryEditor) splitPane.getBottomComponent()).updateAllFields();

            }
            return;

        }

        EntryEditor form;
        int divLoc = -1;
        String visName = null;
        if (getShowing() != null) {
            if (isShowingEditor()) {
                visName = ((EntryEditor) splitPane.getBottomComponent()).getVisiblePanelName();
            }
        }
        if (getShowing() != null) {
            divLoc = splitPane.getDividerLocation();
        }

        if (entryEditors.containsKey(be.getType().getName())) {
            // We already have an editor for this entry type.
            form = entryEditors.get
                    ((be.getType().getName()));
            form.switchTo(be);
            if (visName != null) {
                form.setVisiblePanel(visName);
            }
            splitPane.setBottomComponent(form);
            //highlightEntry(be);
        } else {
            // We must instantiate a new editor for this type.
            form = new EntryEditor(frame, BasePanel.this, be);
            if (visName != null) {
                form.setVisiblePanel(visName);
            }
            splitPane.setBottomComponent(form);

            //highlightEntry(be);
            entryEditors.put(be.getType().getName(), form);

        }
        if (divLoc > 0) {
            splitPane.setDividerLocation(divLoc);
        }
        else {
            splitPane.setDividerLocation
                    (splitPane.getHeight() - Globals.prefs.getInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT));
        //new FocusRequester(form);
        //form.requestFocus();
        }

        newEntryShowing(be);
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
            form = entryEditors.get
                    ((entry.getType().getName()));

            // If the cached editor is not the same as the currently shown one,
            // make sure the current one stores its current edit:
            if ((visibleNow != null) && (form != visibleNow)) {
                visibleNow.storeCurrentEdit();
            }

            form.switchTo(entry);
            //if (visName != null)
            //    form.setVisiblePanel(visName);
        } else {
            // We must instantiate a new editor for this type. First make sure the old one
            // stores its last edit:
            storeCurrentEdit();
            // Then start the new one:
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
        if (mode == BasePanel.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT, splitPane.getHeight() - splitPane.getDividerLocation());
        } else if (mode == BasePanel.SHOWING_PREVIEW) {
            Globals.prefs.putInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT, splitPane.getHeight() - splitPane.getDividerLocation());
        }
        mode = BasePanel.SHOWING_EDITOR;
        currentEditor = editor;
        splitPane.setBottomComponent(editor);
        if (editor.getEntry() != getShowing()) {
            newEntryShowing(editor.getEntry());
        }
        adjustSplitter();

    }

    /**
     * Sets the given preview panel as the bottom component in the split panel.
     * Updates the mode to SHOWING_PREVIEW.
     * @param preview The preview to show.
     */
    public void showPreview(PreviewPanel preview) {
        mode = BasePanel.SHOWING_PREVIEW;
        currentPreview = preview;
        splitPane.setBottomComponent(preview);
    }

    /**
     * Removes the bottom component.
     */
    public void hideBottomComponent() {
        mode = BasePanel.SHOWING_NOTHING;
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
        // Store divider location for next time:
        Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT, splitPane.getHeight() - splitPane.getDividerLocation());
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
        if ((mode == BasePanel.SHOWING_EDITOR) && (currentEditor.getEntry() == be)) {
            selectionListener.entryEditorClosing(currentEditor);
        }
    }

    public void updateEntryEditorIfShowing() {
        if (mode == BasePanel.SHOWING_EDITOR) {
            if (currentEditor.getType() != currentEditor.getEntry().getType()) {
                // The entry has changed type, so we must get a new editor.
                newEntryShowing(null);
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
            EntryEditor editor = (EntryEditor) splitPane.getBottomComponent();
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
        for (Map.Entry<String, EntryEditor> stringEntryEditorEntry : entryEditors.entrySet()) {
            EntryEditor ed = stringEntryEditorEntry.getValue();
            ed.updateAllContentSelectors();
        }
    }

    public void rebuildAllEntryEditors() {
        for (Map.Entry<String, EntryEditor> stringEntryEditorEntry : entryEditors.entrySet()) {
            EntryEditor ed = stringEntryEditorEntry.getValue();
            ed.rebuildPanels();
        }

    }

    public void markBaseChanged() {
        baseChanged = true;

        // Put an asterix behind the file name to indicate the
        // database has changed.
        String oldTitle = frame.getTabTitle(this);
        if (!oldTitle.endsWith("*")) {
            frame.setTabTitle(this, oldTitle + '*', frame.getTabTooltip(this));
            frame.setWindowTitle();
        }
        // If the status line states that the base has been saved, we
        // remove this message, since it is no longer relevant. If a
        // different message is shown, we leave it.
        if (frame.statusLine.getText().startsWith(Globals.lang("Saved database"))) {
            frame.output(" ");
        }

    }

    public void markNonUndoableBaseChanged() {
        nonUndoableChange = true;
        markBaseChanged();
    }

    private synchronized void markChangedOrUnChanged() {
        if (undoManager.hasChanged()) {
            if (!baseChanged) {
                markBaseChanged();
            }
        }
        else if (baseChanged && !nonUndoableChange) {
            baseChanged = false;
            if (getFile() != null) {
                frame.setTabTitle(BasePanel.this, getFile().getName(),
                        getFile().getAbsolutePath());
            } else {
                frame.setTabTitle(BasePanel.this, Globals.lang("untitled"), null);
            }
        }
        frame.setWindowTitle();
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
        showingSearch = true;
    }

    public void setGroupMatcher(Matcher<BibtexEntry> matcher) {
        groupFilterList.setMatcher(matcher);
    }

    public void stopShowingSearchResults() {
        searchFilterList.setMatcher(NoSearchMatcher.INSTANCE);
        showingSearch = false;
    }

    public void stopShowingGroup() {
        groupFilterList.setMatcher(NoSearchMatcher.INSTANCE);
    }

    /**
     * Query whether this BasePanel is in the mode where a float search result is shown.
     * @return true if showing float search, false otherwise.
     */
    public boolean isShowingFloatSearch() {
        return mainTable.isShowingFloatSearch();
    }

    /**
     * Query whether this BasePanel is in the mode where a filter search result is shown.
     * @return true if showing filter search, false otherwise.
     */
    public boolean isShowingFilterSearch() {
        return showingSearch;
    }

    public BibtexDatabase getDatabase() {
        return database;
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

    private void changeType(BibtexEntry[] bes, BibtexEntryType type) {

        if ((bes == null) || (bes.length == 0)) {
            output("First select the entries you wish to change type " +
                    "for.");
            return;
        }
        if (bes.length > 1) {
            int choice = JOptionPane.showConfirmDialog
                    (this, "Multiple entries selected. Do you want to change"
                            + "\nthe type of all these to '" + type.getName() + "'?",
                            "Change type", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.NO_OPTION) {
                return;
            }
        }

        NamedCompound ce = new NamedCompound(Globals.lang("change type"));
        for (BibtexEntry be : bes) {
            ce.addEdit(new UndoableChangeType(be,
                    be.getType(),
                    type));
            be.setType(type);
        }

        output(Globals.lang("Changed type to") + " '" + type.getName() + "' "
                + Globals.lang("for") + ' ' + bes.length
                + ' ' + Globals.lang("entries") + '.');
        ce.end();
        undoManager.addEdit(ce);
        markBaseChanged();
        updateEntryEditorIfShowing();
    }

    public boolean showDeleteConfirmationDialog(int numberOfEntries) {
        if (Globals.prefs.getBoolean(JabRefPreferences.CONFIRM_DELETE)) {
            String msg = Globals.lang("Really delete the selected")
                    + ' ' + Globals.lang("entry") + '?', title = Globals.lang("Delete entry");
            if (numberOfEntries > 1) {
                msg = Globals.lang("Really delete the selected")
                        + ' ' + numberOfEntries + ' ' + Globals.lang("entries") + '?';
                title = Globals.lang("Delete multiple entries");
            }

            CheckBoxMessage cb = new CheckBoxMessage
                    (msg, Globals.lang("Disable this confirmation dialog"), false);

            int answer = JOptionPane.showConfirmDialog(frame, cb, title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (cb.isSelected()) {
                Globals.prefs.putBoolean(JabRefPreferences.CONFIRM_DELETE, false);
            }
            return (answer == JOptionPane.YES_OPTION);
        } else {
            return true;
        }

    }

    /**
     * If the relevant option is set, autogenerate keys for all entries that are
     * lacking keys.
     */
    public void autoGenerateKeysBeforeSaving() {
        if (Globals.prefs.getBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING)) {
            NamedCompound ce = new NamedCompound(Globals.lang("autogenerate keys"));
            boolean any = false;

            for (BibtexEntry bes : database.getEntries()) {
                String oldKey = bes.getCiteKey();
                if ((oldKey == null) || (oldKey.isEmpty())) {
                    LabelPatternUtil.makeLabel(metaData, database, bes);
                    ce.addEdit(new UndoableKeyChange(database, bes.getId(), null,
                            bes.getField(BibtexFields.KEY_FIELD)));
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

    public void setSelectionListenerEnabled(boolean enabled) {
        selectionListener.setEnabled(enabled);
    }

    /**
     * Depending on whether a preview or an entry editor is showing, save the current
     * divider location in the correct preference setting.
     */
    public void saveDividerLocation() {
        if (mode == BasePanel.SHOWING_PREVIEW) {
            Globals.prefs.putInt(JabRefPreferences.PREVIEW_PANEL_HEIGHT, splitPane.getHeight() - splitPane.getDividerLocation());
        } else if (mode == BasePanel.SHOWING_EDITOR) {
            Globals.prefs.putInt(JabRefPreferences.ENTRY_EDITOR_HEIGHT, splitPane.getHeight() - splitPane.getDividerLocation());
        }
    }


    class UndoAction implements BaseAction {

        @Override
        public void action() {
            try {
                JComponent focused = Globals.focusListener.getFocused();
                if ((focused != null) && (focused instanceof FieldEditor) && (focused.hasFocus())) {
                    // User is currently editing a field:
                    // Check if it is the preamble:
                    if ((preambleEditor != null) && (focused == preambleEditor.getFieldEditor())) {
                        preambleEditor.storeCurrentEdit();
                    } else {
                        storeCurrentEdit();
                    }
                }
                String name = undoManager.getUndoPresentationName();
                undoManager.undo();
                markBaseChanged();
                frame.output(name);
            } catch (CannotUndoException ex) {
                ex.printStackTrace();
                frame.output(Globals.lang("Nothing to undo") + '.');
            }
            // After everything, enable/disable the undo/redo actions
            // appropriately.
            //updateUndoState();
            //redoAction.updateRedoState();
            markChangedOrUnChanged();
        }
    }

    class RedoAction implements BaseAction {

        @Override
        public void action() {
            try {

                JComponent focused = Globals.focusListener.getFocused();
                if ((focused != null) && (focused instanceof FieldEditor) && (focused.hasFocus())) {
                    // User is currently editing a field:
                    storeCurrentEdit();
                }

                String name = undoManager.getRedoPresentationName();
                undoManager.redo();
                markBaseChanged();
                frame.output(name);
            } catch (CannotRedoException ex) {
                frame.output(Globals.lang("Nothing to redo") + '.');
            }
            // After everything, enable/disable the undo/redo actions
            // appropriately.
            //updateRedoState();
            //undoAction.updateUndoState();
            markChangedOrUnChanged();
        }
    }


    // Method pertaining to the ClipboardOwner interface.
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    private void setEntryEditorEnabled(boolean enabled) {
        if ((getShowing() != null) && (splitPane.getBottomComponent() instanceof EntryEditor)) {
            EntryEditor ed = (EntryEditor) splitPane.getBottomComponent();
            if (ed.isEnabled() != enabled) {
                ed.setEnabled(enabled);
            }
        }
    }

    public String fileMonitorHandle() {
        return fileMonitorHandle;
    }

    @Override
    public void fileUpdated() {
        if (saving)
         {
            return; // We are just saving the file, so this message is most likely due
        }
        //if (updatedExternally) {
        //  return;
        //}
        // to bad timing. If not, we'll handle it on the next polling.
        //Util.pr("File '"+file.getPath()+"' has been modified.");
        updatedExternally = true;

        final ChangeScanner scanner = new ChangeScanner(frame, BasePanel.this, BasePanel.this.getFile());

        // Adding the sidepane component is Swing work, so we must do this in the Swing
        // thread:
        Runnable t = new Runnable() {

            @Override
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
                //setUpdatedExternally(false);
                //scanner.displayResult();
            }
        };

        // Test: running scan automatically in background
        if ((BasePanel.this.getFile() != null) &&
                !FileBasedLock.waitForFileLock(BasePanel.this.getFile(), 10)) {
            // The file is locked even after the maximum wait. Do nothing.
            System.err.println("File updated externally, but change scan failed because the file is locked.");
            // Perturb the stored timestamp so successive checks are made:
            Globals.fileUpdateMonitor.perturbTimestamp(getFileMonitorHandle());
            return;
        }

        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThreadAndWait(scanner);

        if (scanner.changesFound()) {
            SwingUtilities.invokeLater(t);
        } else {
            setUpdatedExternally(false);
            //System.out.println("No changes found.");
        }
    }

    @Override
    public void fileRemoved() {
        LOGGER.info("File '" + getFile().getPath() + "' has been deleted.");
    }

    /**
     * Perform necessary cleanup when this BasePanel is closed.
     */
    public void cleanUp() {
        if (fileMonitorHandle != null) {
            Globals.fileUpdateMonitor.removeUpdateListener(fileMonitorHandle);
        }
        // Check if there is a FileUpdatePanel for this BasePanel being shown. If so,
        // remove it:
        if (sidePaneManager.hasComponent("fileUpdate")) {
            FileUpdatePanel fup = (FileUpdatePanel) sidePaneManager.getComponent("fileUpdate");
            if (fup.getPanel() == this) {
                sidePaneManager.hideComponent("fileUpdate");
            }
        }
    }

    public void setUpdatedExternally(boolean b) {
        updatedExternally = b;
    }

    /**
     * Get an array containing the currently selected entries.
     * The array is stable and not changed if the selection changes
     *
     * @return An array containing the selected entries. Is never null.
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
        StringBuilder result = new StringBuilder();
        String citeKey;//, message = "";
        boolean first = true;
        for (BibtexEntry bes : mainTable.getSelected()) {
            citeKey = bes.getField(BibtexFields.KEY_FIELD);
            // if the key is empty we give a warning and ignore this entry
            if ((citeKey == null) || citeKey.isEmpty()) {
                continue;
            }
            if (first) {
                result.append(citeKey);
                first = false;
            } else {
                result.append(',').append(citeKey);
            }
        }
        return result.toString();
    }

    public GroupSelector getGroupSelector() {
        return frame.groupSelector;
    }

    public boolean isUpdatedExternally() {
        return updatedExternally;
    }

    public String getFileMonitorHandle() {
        return fileMonitorHandle;
    }

    public void setFileMonitorHandle(String fileMonitorHandle) {
        this.fileMonitorHandle = fileMonitorHandle;
    }

    public SidePaneManager getSidePaneManager() {
        return sidePaneManager;
    }

    public void setNonUndoableChange(boolean nonUndoableChange) {
        this.nonUndoableChange = nonUndoableChange;
    }

    public void setBaseChanged(boolean baseChanged) {
        this.baseChanged = baseChanged;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public boolean isSaving() {
        return saving;
    }

    private BibtexEntry getShowing() {
        return showing;
    }

    /**
     * Update the pointer to the currently shown entry in all cases where the user has
     * moved to a new entry, except when using Back and Forward commands. Also updates
     * history for Back command, and clears history for Forward command.
     * @param entry The entry that is now to be shown.
     */
    public void newEntryShowing(BibtexEntry entry) {
        // If this call is the result of a Back or Forward operation, we must take
        // care not to make any history changes, since the necessary changes will
        // already have been done in the back() or forward() method:
        if (backOrForwardInProgress) {
            showing = entry;
            backOrForwardInProgress = false;
            setBackAndForwardEnabledState();
            return;
        }
        nextEntries.clear();
        if (entry != showing) {
            // Add the entry we are leaving to the history:
            if (showing != null) {
                previousEntries.add(showing);
                if (previousEntries.size() > GUIGlobals.MAX_BACK_HISTORY_SIZE) {
                    previousEntries.remove(0);
                }
            }
            showing = entry;
            setBackAndForwardEnabledState();
        }

    }

    /**
     * Go back (if there is any recorded history) and update the histories for
     * the Back and Forward commands.
     */
    private void back() {
        if (!previousEntries.isEmpty()) {
            BibtexEntry toShow = previousEntries.get(previousEntries.size() - 1);
            previousEntries.remove(previousEntries.size() - 1);
            // Add the entry we are going back from to the Forward history:
            if (showing != null) {
                nextEntries.add(showing);
            }
            backOrForwardInProgress = true; // to avoid the history getting updated erroneously
            //showEntry(toShow);
            highlightEntry(toShow);
        }
    }

    private void forward() {
        if (!nextEntries.isEmpty()) {
            BibtexEntry toShow = nextEntries.get(nextEntries.size() - 1);
            nextEntries.remove(nextEntries.size() - 1);
            // Add the entry we are going forward from to the Back history:
            if (showing != null) {
                previousEntries.add(showing);
            }
            backOrForwardInProgress = true; // to avoid the history getting updated erroneously
            //showEntry(toShow);
            highlightEntry(toShow);
        }
    }

    public void setBackAndForwardEnabledState() {
        frame.back.setEnabled(!previousEntries.isEmpty());
        frame.forward.setEnabled(!nextEntries.isEmpty());
    }


    private class SaveSelectedAction implements BaseAction {

        private final DatabaseSaveType saveType;


        public SaveSelectedAction(DatabaseSaveType saveType) {
            this.saveType = saveType;
        }

        @Override
        public void action() throws Throwable {

            String chosenFile = FileDialogs.getNewFile(frame, new File(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)), ".bib",
                    JFileChooser.SAVE_DIALOG, false);
            if (chosenFile != null) {
                File expFile = new File(chosenFile);
                if (!expFile.exists() ||
                        (JOptionPane.showConfirmDialog
                                (frame, '\'' + expFile.getName() + "' " +
                                        Globals.lang("exists. Overwrite file?"),
                                        Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                            == JOptionPane.OK_OPTION)) {

                    saveDatabase(expFile, true, Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING), saveType);
                    //runCommand("save");
                    frame.getFileHistory().newFile(expFile.getPath());
                    frame.output(Globals.lang("Saved selected to") + " '"
                            + expFile.getPath() + "'.");
                }
            }
        }
    }
}
