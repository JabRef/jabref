/*
 * Copyright (C) 2003 Morten O. Alver, Nizar N. Batada
 *
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package net.sf.jabref;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import net.sf.jabref.autocompleter.AbstractAutoCompleter;
import net.sf.jabref.export.LatexFieldFormatter;
import net.sf.jabref.external.ExternalFilePanel;
import net.sf.jabref.external.WriteXMPEntryEditorAction;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.FileListEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.VerticalLabelUI;
import net.sf.jabref.gui.date.DatePickerButton;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.journals.JournalAbbreviations;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableChangeType;
import net.sf.jabref.undo.UndoableFieldChange;
import net.sf.jabref.undo.UndoableKeyChange;
import net.sf.jabref.undo.UndoableRemoveEntry;

/**
 * GUI component that allows editing of the fields of a BibtexEntry (i.e. the
 * one that shows up, when you double click on an entry in the table)
 * 
 * It hosts the tabs (required, general, optional) and the buttons to the left.
 * 
 * EntryEditor also registers itself as a VetoableChangeListener, receiving
 * events whenever a field of the entry changes, enabling the text fields to
 * update themselves if the change is made from somewhere else.
 */
public class EntryEditor extends JPanel implements VetoableChangeListener {

    // A reference to the entry this object works on.
    private BibtexEntry entry;

    BibtexEntryType type;

    // The action concerned with closing the window.
    CloseAction closeAction;

    // The action that deletes the current entry, and closes the editor.
    DeleteAction deleteAction = new DeleteAction();

    // The action concerned with copying the BibTeX key to the clipboard.
    CopyKeyAction copyKeyAction;

    // The action concerned with copying the BibTeX key to the clipboard.
    AbstractAction nextEntryAction = new NextEntryAction();

    // Actions for switching to next/previous entry.
    AbstractAction prevEntryAction = new PrevEntryAction();

    // The action concerned with storing a field value.
    public StoreFieldAction storeFieldAction;

    // The actions concerned with switching the panels.
    SwitchLeftAction switchLeftAction = new SwitchLeftAction();

    SwitchRightAction switchRightAction = new SwitchRightAction();

    // The action which generates a bibtexkey for this entry.
    public GenerateKeyAction generateKeyAction;

    public AutoSetFileAction autoLink = new AutoSetFileAction();

    public AbstractAction writeXmp;

    SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction();

    JPanel mainPanel = new JPanel();

    JPanel srcPanel = new JPanel();

    EntryEditorTab genPan, optPan, reqPan, absPan;

    JTextField bibtexKey;

    FieldTextField tf;

    JTextArea source;

    JToolBar tlb;

    JTabbedPane tabbed = new JTabbedPane(); // JTabbedPane.RIGHT);

    JLabel lab;

    TypeLabel typeLabel;

    JabRefFrame frame;

    BasePanel panel;

    EntryEditor ths = this;

    HashSet<FieldContentSelector> contentSelectors = new HashSet<FieldContentSelector>();

    Logger logger = Logger.getLogger(EntryEditor.class.getName());

    boolean updateSource = true; // This can be set to false to stop the source
    boolean movingToDifferentEntry = false; // Indicates that we are about to go to the next or previous entry

    List<Object> tabs = new ArrayList<Object>();

    // text area from gettin updated. This is used in cases where the source
    // couldn't be parsed, and the user is given the option to edit it.
    boolean lastSourceAccepted = true; // This indicates whether the last

    // attempt
    // at parsing the source was successful. It is used to determine whether the
    // dialog should close; it should stay open if the user received an error
    // message about the source, whatever he or she chose to do about it.
    String lastSourceStringAccepted = null; // This is used to prevent double

    // fields.
    // These values can be used to calculate the preferred height for the form.
    // reqW starts at 1 because it needs room for the bibtex key field.
    private int sourceIndex = -1; // The index the source panel has in tabbed.

    JabRefPreferences prefs;

    HelpAction helpAction;

    UndoAction undoAction = new UndoAction();

    RedoAction redoAction = new RedoAction();

    TabListener tabListener = new TabListener();

    public EntryEditor(JabRefFrame frame_, BasePanel panel_, BibtexEntry entry_) {

        frame = frame_;
        panel = panel_;
        entry = entry_;
        prefs = Globals.prefs;
        type = entry.getType();

        entry.addPropertyChangeListener(this);

        helpAction = new HelpAction(frame.helpDiag, GUIGlobals.entryEditorHelp, "Help");
        closeAction = new CloseAction();
        copyKeyAction = new CopyKeyAction();
        generateKeyAction = new GenerateKeyAction(frame);
        storeFieldAction = new StoreFieldAction();
        writeXmp = new WriteXMPEntryEditorAction(panel_, this);

        BorderLayout bl = new BorderLayout();
        setLayout(bl);
        setupToolBar();
        setupFieldPanels();
        setupSourcePanel();
        add(tabbed, BorderLayout.CENTER);
        tabbed.addChangeListener(tabListener);
        if (prefs.getBoolean("showSource") && prefs.getBoolean("defaultShowSource"))
            tabbed.setSelectedIndex(sourceIndex);

        updateAllFields();
    }

    private void setupFieldPanels() {
        tabbed.removeAll();
        tabs.clear();
        String[] fields = entry.getRequiredFields();

        List<String> fieldList = null;
        if (fields != null)
            fieldList = java.util.Arrays.asList(fields);
        reqPan = new EntryEditorTab(frame, panel, fieldList, this, true, Globals.lang("Required fields"));
        tabbed.addTab(Globals.lang("Required fields"), GUIGlobals.getImage("required"), reqPan
            .getPane(), Globals.lang("Show required fields"));
        tabs.add(reqPan);

        if ((entry.getOptionalFields() != null) && (entry.getOptionalFields().length >= 1)) {
            if (!prefs.getBoolean("biblatexMode")) {
                optPan = new EntryEditorTab(frame, panel, java.util.Arrays.asList(entry.getOptionalFields()), this,
                    false, Globals.lang("Optional fields"));
                tabbed.addTab(Globals.lang("Optional fields"), GUIGlobals.getImage("optional"), optPan
                    .getPane(), Globals.lang("Show optional fields"));
                tabs.add(optPan);
            }
            else {
                optPan = new CompressedEntryEditorTab(frame, panel,
                        java.util.Arrays.asList(entry.getType().getPrimaryOptionalFields()), this,
                    false, Globals.lang("Optional fields"));
                tabbed.addTab(Globals.lang("Optional fields"), GUIGlobals.getImage("optional"), optPan
                    .getPane(), Globals.lang("Show optional fields"));
                tabs.add(optPan);
                optPan = new CompressedEntryEditorTab(frame, panel,
                        java.util.Arrays.asList(Util.getRemainder(entry.getOptionalFields(),
                                entry.getType().getPrimaryOptionalFields())), this,
                    false, Globals.lang("Optional fields 2"));
                tabbed.addTab(Globals.lang("Optional fields 2"), GUIGlobals.getImage("optional"), optPan
                    .getPane(), Globals.lang("Show optional fields"));
                tabs.add(optPan);

            }
        }

        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        for (int i = 0; i < tabList.getTabCount(); i++) {
            EntryEditorTab newTab = new EntryEditorTab(frame, panel, tabList.getTabFields(i), this, false,
                tabList.getTabName(i));
            tabbed.addTab(tabList.getTabName(i), GUIGlobals.getImage("general"), newTab.getPane());
            tabs.add(newTab);
        }

        srcPanel.setName(Globals.lang("BibTeX source"));
        if (Globals.prefs.getBoolean("showSource")) {
            tabbed.addTab(Globals.lang("BibTeX source"), GUIGlobals.getImage("source"), srcPanel,
                Globals.lang("Show/edit BibTeX source"));
            tabs.add(srcPanel);
        }
        sourceIndex = tabs.size() - 1; // Set the sourceIndex variable.
        srcPanel.setFocusCycleRoot(true);
    }

    public BibtexEntryType getType() {
        return type;
    }

    public BibtexEntry getEntry() {
        return entry;
    }
    
    public BibtexDatabase getDatabase(){
    	return panel.getDatabase();
    }

    private void setupToolBar() {
        JPanel leftPan = new JPanel();
        leftPan.setLayout(new BorderLayout());
        tlb = new JToolBar(JToolBar.VERTICAL);
        //tlb.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        tlb.setBorder(null);
        tlb.setRollover(true);

        tlb.setMargin(new Insets(0, 0, 0, 2));

        // The toolbar carries all the key bindings that are valid for the whole
        // window.
        ActionMap am = tlb.getActionMap();
        InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        im.put(prefs.getKey("Close entry editor"), "close");
        am.put("close", closeAction);
        im.put(prefs.getKey("Entry editor, store field"), "store");
        am.put("store", storeFieldAction);
        im.put(prefs.getKey("Autogenerate BibTeX keys"), "generateKey");
        am.put("generateKey", generateKeyAction);
        im.put(prefs.getKey("Automatically link files"), "autoLink");
        am.put("autoLink", autoLink);
        im.put(prefs.getKey("Entry editor, previous entry"), "prev");
        am.put("prev", prevEntryAction);
        im.put(prefs.getKey("Entry editor, next entry"), "next");
        am.put("next", nextEntryAction);
        im.put(prefs.getKey("Undo"), "undo");
        am.put("undo", undoAction);
        im.put(prefs.getKey("Redo"), "redo");
        am.put("redo", redoAction);
        im.put(prefs.getKey("Help"), "help");
        am.put("help", helpAction);

        tlb.setFloatable(false);

        // Add actions (and thus buttons)
        JButton closeBut = new JButton(closeAction);
        closeBut.setText(null);
        closeBut.setBorder(null);
        leftPan.add(closeBut, BorderLayout.NORTH);

        // Create type-label
        typeLabel = new TypeLabel(entry.getType().getName());
        leftPan.add(typeLabel, BorderLayout.CENTER);

        //tlb.add(typeL);

        tlb.addSeparator();
        
        tlb.add(generateKeyAction);
        tlb.add(autoLink);

        tlb.add(writeXmp);

        tlb.addSeparator();

        tlb.add(deleteAction);
        tlb.add(prevEntryAction);
        tlb.add(nextEntryAction);
        
        tlb.addSeparator();
        
        tlb.add(helpAction);

        Component[] comps = tlb.getComponents();

        for (int i = 0; i < comps.length; i++)
            ((JComponent) comps[i]).setOpaque(false);

        leftPan.add(tlb, BorderLayout.SOUTH);
        add(leftPan, BorderLayout.WEST);
    }

    /**
     * Rebuild the field tabs. This is called e.g. when a new content selector
     * has been added.
     */
    public void rebuildPanels() {
        // Remove change listener, because the rebuilding causes meaningless
        // events and trouble:
        tabbed.removeChangeListener(tabListener);
        
        setupFieldPanels();
        // Add the change listener again:
        tabbed.addChangeListener(tabListener);
        revalidate();
        repaint();
    }

    /**
     * getExtra checks the field name against BibtexFields.getFieldExtras(name).
     * If the name has an entry, the proper component to be shown is created and
     * returned. Otherwise, null is returned. In addition, e.g. listeners can be
     * added to the field editor, even if no component is returned.
     * 
     * @param string
     *            Field name
     * @return Component to show, or null if none.
     */
    public JComponent getExtra(String string, final FieldEditor ed) {

        // fieldName and parameter string identically ????
        final String fieldName = ed.getFieldName();

        String s = BibtexFields.getFieldExtras(string);

        // timestamp or a other field with datepicker command
        if ((fieldName.equals(Globals.prefs.get("timeStampField")))
            || ((s != null) && s.equals("datepicker"))) {
            // double click AND datefield => insert the current date (today)
            ((JTextArea) ed).addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) // double click
                    {
                        String date = Util.easyDateFormat();
                        ed.setText(date);
                    }
                }
            });

            // insert a datepicker, if the extras field contains this command
            if ((s != null) && s.equals("datepicker")) {
                DatePickerButton datePicker = new DatePickerButton(ed);
                return datePicker.getDatePicker();
            }
        }

        if ((s != null) && s.equals("external")) {

            // Add external viewer listener for "pdf" and "url" fields.
            ((JComponent) ed).addMouseListener(new ExternalViewerListener());

            return null;
        } else if ((s != null) && s.equals("journalNames")) {
            // Add controls for switching between abbreviated and full journal
            // names.
            // If this field also has a FieldContentSelector, we need to combine
            // these.
            JPanel controls = new JPanel();
            controls.setLayout(new BorderLayout());
            if (panel.metaData.getData(Globals.SELECTOR_META_PREFIX + ed.getFieldName()) != null) {
                FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, ed,
                    panel.metaData, storeFieldAction, false, ", ");
                contentSelectors.add(ws);
                controls.add(ws, BorderLayout.NORTH);
            }
            controls.add(JournalAbbreviations.getNameSwitcher(this, ed, panel.undoManager),
                BorderLayout.SOUTH);
            return controls;
        } else if (panel.metaData.getData(Globals.SELECTOR_META_PREFIX + ed.getFieldName()) != null) {
            FieldContentSelector ws = new FieldContentSelector(frame, panel, frame, ed,
                panel.metaData, storeFieldAction, false,
                ((ed.getFieldName().equals("author") || ed.getFieldName().equals("editor")) ? " and " : ", "));
            contentSelectors.add(ws);

            return ws;
        } else if ((s != null) && s.equals("browse")) {
            JButton but = new JButton(Globals.lang("Browse"));
            ((JComponent) ed).addMouseListener(new ExternalViewerListener());

            // but.setBackground(GUIGlobals.lightGray);
            but.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String dir = ed.getText();

                    if (dir.equals(""))
                        dir = prefs.get(fieldName + Globals.FILETYPE_PREFS_EXT, "");

                    String chosenFile = FileDialogs.getNewFile(frame, new File(dir), "." + fieldName,
                        JFileChooser.OPEN_DIALOG, false);

                    if (chosenFile != null) {
                        File newFile = new File(chosenFile); // chooser.getSelectedFile();
                        ed.setText(newFile.getPath());
                        prefs.put(fieldName + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
                        updateField(ed);
                    }
                }
            });

            return but;
            // } else if ((s != null) && s.equals("browsePdf")) {
        } else if ((s != null) && (s.equals("browseDoc") || s.equals("browseDocZip"))) {

            final String ext = "." + fieldName.toLowerCase();
            final OpenFileFilter off;
            if (s.equals("browseDocZip"))
                off = new OpenFileFilter(new String[] { ext, ext + ".gz", ext + ".bz2" });
            else
                off = new OpenFileFilter(new String[] { ext });

            ExternalFilePanel pan = new ExternalFilePanel(frame, panel.metaData(), this, fieldName,
                off, ed);
            return pan;
        }
        /*
         * else if ((s != null) && s.equals("browsePs")) { ExternalFilePanel pan =
         * new ExternalFilePanel(frame, this, "ps", off, ed); return pan; }
         */
        else if ((s != null) && s.equals("url")) {
            ((JComponent) ed).setDropTarget(new DropTarget((Component) ed,
                DnDConstants.ACTION_NONE, new SimpleUrlDragDrop(ed, storeFieldAction)));

            return null;
        }

        else if ((s != null) && (s.equals("setOwner"))) {
            JButton button = new JButton(Globals.lang("Auto"));
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    ed.setText(Globals.prefs.get("defaultOwner"));
                    storeFieldAction.actionPerformed(new ActionEvent(ed, 0, ""));
                }
            });
            return button;
        }
        else
            return null;
    }

    private void setupSourcePanel() {
        source = new JTextArea();/* {
            private boolean antialias = Globals.prefs.getBoolean("antialias");

            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                if (antialias)
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                super.paint(g2);
            }
        };*/

        //DefaultFormBuilder builder = new DefaultFormBuilder
        //        (srcPanel, new FormLayout( "fill:pref:grow", "fill:pref:grow"));
        source.setEditable(true); // prefs.getBoolean("enableSourceEditing"));
        source.setLineWrap(true);
        source.setTabSize(GUIGlobals.INDENT);
        source.addFocusListener(new FieldEditorFocusListener());
        // Add the global focus listener, so a menu item can see if this field
        // was focused when
        // an action was called.
        source.addFocusListener(Globals.focusListener);
        source.setFont(new Font("Monospaced", Font.PLAIN, Globals.prefs.getInt("fontSize")));
        setupJTextComponent(source);
        updateSource();

        JScrollPane sp = new JScrollPane(source, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //builder.append(sp);
        
        srcPanel.setLayout(new BorderLayout());
        srcPanel.add(sp, BorderLayout.CENTER);

    }

    public void updateSource() {
        if (updateSource) {
            StringWriter sw = new StringWriter(200);

            try {
                LatexFieldFormatter formatter = new LatexFieldFormatter();
                formatter.setNeverFailOnHashes(true);
                entry.write(sw, formatter, false);

                String srcString = sw.getBuffer().toString();
                source.setText(srcString);
                lastSourceStringAccepted = srcString;
            } catch (IOException ex) {
                source.setText(ex.getMessage() + "\n\n" +
                                        Globals.lang("Correct the entry, and "
                    + "reopen editor to display/edit source."));
                source.setEditable(false);
            }


        }
    }

    /**
     * NOTE: This method is only used for the source panel, not for the
     * other tabs. Look at EntryEditorTab for the setup of text components
     * in the other tabs.
     */
    public void setupJTextComponent(JTextComponent ta) {


        // Set up key bindings and focus listener for the FieldEditor.
        InputMap im = ta.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = ta.getActionMap();

        // im.put(KeyStroke.getKeyStroke(GUIGlobals.closeKey), "close");
        // am.put("close", closeAction);
        im.put(prefs.getKey("Entry editor, store field"), "store");
        am.put("store", storeFieldAction);

        im.put(prefs.getKey("Entry editor, next panel"), "right");
        im.put(prefs.getKey("Entry editor, next panel 2"), "right");
        am.put("right", switchRightAction);

        im.put(prefs.getKey("Entry editor, previous panel"), "left");
        im.put(prefs.getKey("Entry editor, previous panel 2"), "left");
        am.put("left", switchLeftAction);

        im.put(prefs.getKey("Help"), "help");
        am.put("help", helpAction);
        im.put(prefs.getKey("Save database"), "save");
        am.put("save", saveDatabaseAction);

        im.put(Globals.prefs.getKey("Next tab"), "nexttab");
        am.put("nexttab", frame.nextTab);
        im.put(Globals.prefs.getKey("Previous tab"), "prevtab");
        am.put("prevtab", frame.prevTab);
        try {
            HashSet<AWTKeyStroke> keys = new HashSet<AWTKeyStroke>(ta
                .getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
            keys.clear();
            keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
            ta.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
            keys = new HashSet<AWTKeyStroke>(ta
                .getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
            keys.clear();
            keys.add(KeyStroke.getKeyStroke("shift pressed TAB"));
            ta.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
        } catch (Throwable t) {
            System.err.println(t);
        }

        ta.addFocusListener(new FieldListener());
    }

    public void requestFocus() {
        activateVisible();
    }

    private void activateVisible() {
        Object activeTab = tabs.get(tabbed.getSelectedIndex());

        if (activeTab instanceof EntryEditorTab)
            ((EntryEditorTab) activeTab).activate();
        else
            new FocusRequester(source);
        // ((JComponent)activeTab).requestFocus();
    }

    /**
     * Reports the enabled status of the editor, as set by setEnabled()
     */
    public boolean isEnabled() {
        return source.isEnabled();
    }

    /**
     * Sets the enabled status of all text fields of the entry editor.
     */
    public void setEnabled(boolean enabled) {
        for (Iterator<Object> i = tabs.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof EntryEditorTab) {
                ((EntryEditorTab) o).setEnabled(enabled);
            }
        }
        source.setEnabled(enabled);

    }

    /**
     * Centers the given row, and highlights it.
     * 
     * @param row
     *            an <code>int</code> value
     */
    private void scrollTo(int row) {
        movingToDifferentEntry = true;
        panel.mainTable.setRowSelectionInterval(row, row);
        panel.mainTable.ensureVisible(row);
    }

    /**
     * Makes sure the current edit is stored.
     */
    public void storeCurrentEdit() {
        Component comp = Globals.focusListener.getFocused();
        if ((comp == source) || ((comp instanceof FieldEditor) && this.isAncestorOf(comp))) {
            if (comp instanceof FieldEditor)
                ((FieldEditor)comp).clearAutoCompleteSuggestion();
            storeFieldAction.actionPerformed(new ActionEvent(comp, 0, ""));
        }
    }

    /**
     * Returns the index of the active (visible) panel.
     * 
     * @return an <code>int</code> value
     */
    public int getVisiblePanel() {
        return tabbed.getSelectedIndex();
    }

    /** Returns the name of the currently selected component. */
    public String getVisiblePanelName() {
        return tabbed.getSelectedComponent().getName();
    }

    /**
     * Sets the panel with the given index visible.
     * 
     * @param i
     *            an <code>int</code> value
     */
    public void setVisiblePanel(int i) {
        tabbed.setSelectedIndex(Math.min(i, tabbed.getTabCount() - 1));
    }

    public void setVisiblePanel(String name) {
        for (int i = 0; i < tabbed.getTabCount(); ++i) {
            if (name.equals(tabbed.getComponent(i).getName())) {
                tabbed.setSelectedIndex(i);
                return;
            }
        }
        if (tabbed.getTabCount() > 0)
            tabbed.setSelectedIndex(0);
    }

    /**
     * Updates this editor to show the given entry, regardless of type
     * correspondence.
     * 
     * @param be
     *            a <code>BibtexEntry</code> value
     */
    public synchronized void switchTo(BibtexEntry be) {
        if (entry == be)
            return;

        storeCurrentEdit();

        // Remove this instance as property listener for the entry:
        entry.removePropertyChangeListener(this);

        // Register as property listener for the new entry:
        be.addPropertyChangeListener(this);

        entry = be;

        updateAllFields();
        validateAllFields();
        updateSource();
        panel.newEntryShowing(be);

    }

    /**
     * Returns false if the contents of the source panel has not been validated,
     * true othervise.
     */
    public boolean lastSourceAccepted() {
        if (tabbed.getSelectedComponent() == srcPanel)
            storeSource(false);

        return lastSourceAccepted;
    }

    /*
     * public boolean storeSourceIfNeeded() { if (tabbed.getSelectedIndex() ==
     * sourceIndex) return storeSource(); else return true; }
     */
    public boolean storeSource(boolean showError) {
        // Store edited bibtex code.
        BibtexParser bp = new BibtexParser(new java.io.StringReader(source.getText()));

        try {
            BibtexDatabase db = bp.parse().getDatabase();

            if (db.getEntryCount() > 1)
                throw new Exception("More than one entry found.");

            if (db.getEntryCount() < 1)
                throw new Exception("No entries found.");

            NamedCompound compound = new NamedCompound(Globals.lang("source edit"));
            BibtexEntry nu = db.getEntryById(db.getKeySet().iterator().next());
            String id = entry.getId();
            String
            // oldKey = entry.getCiteKey(),
            newKey = nu.getCiteKey();
            boolean anyChanged = false;
            boolean changedType = false;
            boolean duplicateWarning = false;
            boolean emptyWarning = newKey == null || newKey.equals("");

            if (panel.database.setCiteKeyForEntry(id, newKey)) {
                duplicateWarning = true;

                // First, remove fields that the user have removed.
            }

            for (String field : entry.getAllFields()){
                if (BibtexFields.isDisplayableField(field.toString())) {
                    if (nu.getField(field.toString()) == null) {
                        compound.addEdit(new UndoableFieldChange(entry, field.toString(), entry
                            .getField(field.toString()), null));
                        entry.clearField(field.toString());
                        anyChanged = true;
                    }
                }
            }

            // Then set all fields that have been set by the user.
            for (String field : nu.getAllFields()){
                if (entry.getField(field.toString()) != nu.getField(field.toString())) {
                    String toSet = nu.getField(field.toString());

                    // Test if the field is legally set.
                    (new LatexFieldFormatter()).format(toSet, field.toString());

                    compound.addEdit(new UndoableFieldChange(entry, field.toString(), entry
                        .getField(field.toString()), toSet));
                    entry.setField(field.toString(), toSet);
                    anyChanged = true;
                }
            }

            // See if the user has changed the entry type:
            if (nu.getType() != entry.getType()) {
                compound.addEdit(new UndoableChangeType(entry,
                      entry.getType(), nu.getType()));
                entry.setType(nu.getType());
                anyChanged = true;
                changedType = true;
            }
            compound.end();

            if (!anyChanged)
                return true;

            panel.undoManager.addEdit(compound);

            /*
             * if (((oldKey == null) && (newKey != null)) || ((oldKey != null) &&
             * (newKey == null)) || ((oldKey != null) && (newKey != null) &&
             * !oldKey.equals(newKey))) { }
             */
            if (duplicateWarning) {
                warnDuplicateBibtexkey();
            } else if (emptyWarning && showError) {
                warnEmptyBibtexkey();
            } else {
                panel.output(Globals.lang("Stored entry") + ".");
            }

            lastSourceStringAccepted = source.getText();
            if (!changedType) {
                updateAllFields();
                lastSourceAccepted = true;
                updateSource = true;
            }
            else {
                panel.updateEntryEditorIfShowing();
            }
            // TODO: does updating work properly after source stored?
            // panel.tableModel.remap();
            // panel.entryTable.repaint();
            // panel.refreshTable();
            panel.markBaseChanged();

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final int row = panel.mainTable.findEntry(entry);
                    if (row >= 0) {
                        if (panel.mainTable.getSelectedRowCount() == 0)
                            panel.mainTable.setRowSelectionInterval(row, row);
                        panel.mainTable.ensureVisible(row);
                    }
                }
            });
            
            return true;
        } catch (Throwable ex) {
            // ex.printStackTrace();
            // The source couldn't be parsed, so the user is given an
            // error message, and the choice to keep or revert the contents
            // of the source text field.
            updateSource = false;
            lastSourceAccepted = false;
            tabbed.setSelectedComponent(srcPanel);

            if (showError) {
                Object[] options = { Globals.lang("Edit"),
                    Globals.lang("Revert to original source") };

                int answer = JOptionPane.showOptionDialog(frame, Globals.lang("Error") + ": " + ex.getMessage(),
                    Globals.lang("Problem with parsing entry"), JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE, null, options, options[0]);

                if (answer != 0) {
                    updateSource = true;
                    updateSource();
                }
            }

            return false;
        }
    }

    public void setField(String fieldName, String newFieldData) {

        for (Iterator<Object> i = tabs.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof EntryEditorTab) {
                ((EntryEditorTab) o).updateField(fieldName, newFieldData);
            }
        }

    }

    /**
     * Sets all the text areas according to the shown entry.
     */
    public void updateAllFields() {
        for (Iterator<Object> i = tabs.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof EntryEditorTab) {
                ((EntryEditorTab) o).setEntry(entry);
            }
        }
    }

    /**
     * Removes the "invalid field" color from all text areas.
     */
    public void validateAllFields() {
        for (Iterator<Object> i = tabs.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof EntryEditorTab) {
                ((EntryEditorTab) o).validateAllFields();
            }
        }
    }

    public void updateAllContentSelectors() {
        if (contentSelectors.size() > 0) {
            for (Iterator<FieldContentSelector> i = contentSelectors.iterator(); i.hasNext();)
                i.next().rebuildComboBox();
        }
    }

    /*
     * Update the JTextArea when a field has changed.
     * 
     * (non-Javadoc)
     * 
     * @see java.beans.VetoableChangeListener#vetoableChange(java.beans.PropertyChangeEvent)
     */
    public void vetoableChange(PropertyChangeEvent e) {
        String newValue = ((e.getNewValue() != null) ? e.getNewValue().toString() : "");
        setField(e.getPropertyName(), newValue);
    }

    public void updateField(final Object source) {
        storeFieldAction.actionPerformed(new ActionEvent(source, 0, ""));
    }


    public void setMovingToDifferentEntry() {
        movingToDifferentEntry = true;
    }

    private class TypeLabel extends JLabel {
        public TypeLabel(String type) {
            super(type+" ");
            setUI(new VerticalLabelUI(false));
            setForeground(GUIGlobals.validFieldColor);
            setHorizontalAlignment(RIGHT);
            setFont(GUIGlobals.typeNameFont);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    boolean ctrlClick = prefs.getBoolean("ctrlClick");

                    if ((e.getButton() == MouseEvent.BUTTON3)
                        || (ctrlClick && (e.getButton() == MouseEvent.BUTTON1) && e.isControlDown())) {
                        JPopupMenu typeMenu = new JPopupMenu();

                        // typeMenu.addSeparator();
                        for (String s: BibtexEntryType.ALL_TYPES.keySet())
                            typeMenu.add(new ChangeTypeAction(BibtexEntryType.getType(s), panel));

                        typeMenu.show(ths, e.getX(), e.getY());
                    }
                }
            });
        }

        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            //g2.setColor(GUIGlobals.validFieldColor);
            //g2.setFont(GUIGlobals.typeNameFont);
            //FontMetrics fm = g2.getFontMetrics();
            //int width = fm.stringWidth(label);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paintComponent(g2);
            //g2.rotate(-Math.PI / 2, 0, 0);
            //g2.drawString(label, -width - 7, 28);
        }
    }

    class FieldListener extends FocusAdapter {
        /*
         * Focus listener that fires the storeFieldAction when a FieldTextArea
         * loses focus.
         */
        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            // Util.pr("Lost focus "+e.getSource().toString().substring(0,30));
            if (!e.isTemporary())
                updateField(e.getSource());
        }
    }

    class TabListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    activateVisible();
                }
            });

            // After the initial event train has finished, we tell the editor
            // tab to update all
            // its fields. This makes sure they are updated even if the tab we
            // just left contained one
            // or more of the same fields as this one:
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Object activeTab = tabs.get(tabbed.getSelectedIndex());
                    if (activeTab instanceof EntryEditorTab)
                        ((EntryEditorTab) activeTab).updateAll();
                }
            });

        }
    }

    class DeleteAction extends AbstractAction {
        public DeleteAction() {
            super(Globals.lang("Delete"), GUIGlobals.getImage("delete"));
            putValue(SHORT_DESCRIPTION, Globals.lang("Delete entry"));
        }

        public void actionPerformed(ActionEvent e) {
            // Show confirmation dialog if not disabled:
            boolean goOn = panel.showDeleteConfirmationDialog(1);

            if (!goOn)
                return;

            panel.entryEditorClosing(EntryEditor.this);
            panel.database.removeEntry(entry.getId());
            panel.markBaseChanged();
            panel.undoManager.addEdit(new UndoableRemoveEntry(panel.database, entry, panel));
            panel.output(Globals.lang("Deleted") + " " + Globals.lang("entry"));
        }
    }

    class CloseAction extends AbstractAction {
        public CloseAction() {
            super(Globals.lang("Close window"), GUIGlobals.getImage("close"));
            putValue(SHORT_DESCRIPTION, Globals.lang("Close window"));
        }

        public void actionPerformed(ActionEvent e) {
            if (tabbed.getSelectedComponent() == srcPanel) {
                updateField(source);
                if (lastSourceAccepted)
                    panel.entryEditorClosing(EntryEditor.this);
            } else
                panel.entryEditorClosing(EntryEditor.this);
        }
    }

    class CopyKeyAction extends AbstractAction {
        public CopyKeyAction() {
            super("Copy BibTeX key to clipboard");
            putValue(SHORT_DESCRIPTION, "Copy BibTeX key to clipboard (Ctrl-K)");
            // putValue(MNEMONIC_KEY, GUIGlobals.copyKeyCode);
        }

        public void actionPerformed(ActionEvent e) {
            String s = (entry.getField(BibtexFields.KEY_FIELD));
            StringSelection ss = new StringSelection(s);

            if (s != null)
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        }
    }

    public class StoreFieldAction extends AbstractAction {
        public StoreFieldAction() {
            super("Store field value");
            putValue(SHORT_DESCRIPTION, "Store field value");
        }

        public void actionPerformed(ActionEvent e) {
            boolean movingAway = movingToDifferentEntry;
            movingToDifferentEntry = false;

            if (e.getSource() instanceof FieldTextField) {
                // Storage from bibtex key field.
                FieldTextField fe = (FieldTextField) e.getSource();
                String oldValue = entry.getCiteKey();
                String newValue = fe.getText();

                if (newValue.equals(""))
                    newValue = null;

                if (((oldValue == null) && (newValue == null))
                    || ((oldValue != null) && (newValue != null) && oldValue.equals(newValue)))
                    return; // No change.

                // Make sure the key is legal:
                String cleaned = Util.checkLegalKey(newValue);
                if ((cleaned != null) && !cleaned.equals(newValue)) {
                    JOptionPane.showMessageDialog(frame, Globals.lang("Invalid BibTeX key"),
                        Globals.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                    fe.setBackground(GUIGlobals.invalidFieldBackground);
                    return;
                } else {
                    fe.setBackground(/*
                                         * fe.getTextComponent().hasFocus() ?
                                         * GUIGlobals.activeEditor :
                                         */
                    GUIGlobals.validFieldBackground);
                }

                boolean isDuplicate = panel.database.setCiteKeyForEntry(entry.getId(), newValue);

                if (newValue != null) {
                    if (isDuplicate)
                        warnDuplicateBibtexkey();
                    else
                        panel.output(Globals.lang("BibTeX key is unique."));
                } else { // key is null/empty
                    warnEmptyBibtexkey();
                }

                // Add an UndoableKeyChange to the baseframe's undoManager.
                panel.undoManager.addEdit(new UndoableKeyChange(panel.database, entry.getId(),
                    oldValue, newValue));

                if ((newValue != null) && (newValue.length() > 0))
                    // fe.setLabelColor(GUIGlobals.validFieldColor);
                    fe.setBackground(GUIGlobals.validFieldBackground);
                else
                    // fe.setLabelColor(GUIGlobals.nullFieldColor);
                    fe.setBackground(GUIGlobals.validFieldBackground);

                if (fe.getTextComponent().hasFocus())
                    fe.setBackground(GUIGlobals.activeEditor);
                updateSource();
                panel.markBaseChanged();
            }
            else if (e.getSource() instanceof FieldEditor) {
                String toSet = null;
                FieldEditor fe = (FieldEditor) e.getSource();
                boolean set;
                // Trim the whitespace off this value
                String currentText = fe.getText();
                String trim = currentText.trim();
                if (trim.length() > 0) {
                    toSet = trim;
                }

                // We check if the field has changed, since we don't want to
                // mark the base as changed unless we have a real change.
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

                if (set) {
                    try {
                        // The following statement attempts to write the
                        // new contents into a StringWriter, and this will
                        // cause an IOException if the field is not
                        // properly formatted. If that happens, the field
                        // is not stored and the textarea turns red.
                        if (toSet != null)
                            (new LatexFieldFormatter()).format(toSet, fe.getFieldName());

                        String oldValue = entry.getField(fe.getFieldName());

                        if (toSet != null)
                            entry.setField(fe.getFieldName(), toSet);
                        else
                            entry.clearField(fe.getFieldName());

                        if ((toSet != null) && (toSet.length() > 0))
                            // fe.setLabelColor(GUIGlobals.validFieldColor);
                            fe.setBackground(GUIGlobals.validFieldBackground);
                        else
                            // fe.setLabelColor(GUIGlobals.nullFieldColor);
                            fe.setBackground(GUIGlobals.validFieldBackground);

                        // See if we need to update an AutoCompleter instance:
                        AbstractAutoCompleter aComp = panel.getAutoCompleter(fe.getFieldName());
                        if (aComp != null)
                            aComp.addBibtexEntry(entry);

                        // Add an UndoableFieldChange to the baseframe's
                        // undoManager.
                        panel.undoManager.addEdit(new UndoableFieldChange(entry, fe.getFieldName(),
                            oldValue, toSet));
                        updateSource();
                        panel.markBaseChanged();

                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(frame, Globals.lang("Error") + ": " + ex.getMessage(), Globals
                            .lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                        fe.setBackground(GUIGlobals.invalidFieldBackground);
                    }
                } else {
                    // set == false
                    // We set the field and label color.
                    fe.setBackground(GUIGlobals.validFieldBackground);
                }
                if (fe.getTextComponent().hasFocus())
                    fe.setBackground(GUIGlobals.activeEditor);
            } else if ((source.isEditable())
                && (!source.getText().equals(lastSourceStringAccepted))) {
                boolean accepted = storeSource(true);

                if (accepted) {
                }
            }

            // Make sure we scroll to the entry if it moved in the table.
            // Should only be done if this editor is currently showing:
            //System.out.println(getType().getName()+": movingAway="+movingAway+", isShowing="+isShowing());
            if (!movingAway && isShowing()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final int row = panel.mainTable.findEntry(entry);
                        if (row >= 0) {
                            if (panel.mainTable.getSelectedRowCount() == 0)
                                panel.mainTable.setRowSelectionInterval(row, row);
                            panel.mainTable.ensureVisible(row);
                        }
                    }
                });
            }
        }
    }

    class SwitchLeftAction extends AbstractAction {
        public SwitchLeftAction() {
            super("Switch to the panel to the left");
        }

        public void actionPerformed(ActionEvent e) {
            // System.out.println("switch left");
            int i = tabbed.getSelectedIndex();
            tabbed.setSelectedIndex(((i > 0) ? (i - 1) : (tabbed.getTabCount() - 1)));

            activateVisible();
        }
    }

    class SwitchRightAction extends AbstractAction {
        public SwitchRightAction() {
            super("Switch to the panel to the right");
        }

        public void actionPerformed(ActionEvent e) {
            // System.out.println("switch right");
            int i = tabbed.getSelectedIndex();
            tabbed.setSelectedIndex((i < (tabbed.getTabCount() - 1)) ? (i + 1) : 0);
            activateVisible();

        }
    }

    class NextEntryAction extends AbstractAction {
        public NextEntryAction() {
            super(Globals.lang("Next entry"), GUIGlobals.getImage("down"));

            putValue(SHORT_DESCRIPTION, Globals.lang("Next entry"));
        }

        public void actionPerformed(ActionEvent e) {

            int thisRow = panel.mainTable.findEntry(entry);
            int newRow = -1;

            if ((thisRow + 1) < panel.database.getEntryCount())
                newRow = thisRow + 1;
            else if (thisRow > 0)
                newRow = 0;
            else
                return; // newRow is still -1, so we can assume the database has
                        // only one entry.

            scrollTo(newRow);
            panel.mainTable.setRowSelectionInterval(newRow, newRow);

        }
    }

    class PrevEntryAction extends AbstractAction {
        public PrevEntryAction() {
            super(Globals.lang("Previous entry"), GUIGlobals.getImage("up"));

            putValue(SHORT_DESCRIPTION, Globals.lang("Previous entry"));
        }

        public void actionPerformed(ActionEvent e) {
            int thisRow = panel.mainTable.findEntry(entry);
            int newRow = -1;

            if ((thisRow - 1) >= 0)
                newRow = thisRow - 1;
            else if (thisRow != (panel.database.getEntryCount() - 1))
                newRow = panel.database.getEntryCount() - 1;
            else
                return; // newRow is still -1, so we can assume the database has
                        // only one entry.
            // id = panel.tableModel.getIdForRow(newRow);
            // switchTo(id);

            scrollTo(newRow);
            panel.mainTable.setRowSelectionInterval(newRow, newRow);

        }
    }

    class GenerateKeyAction extends AbstractAction {
        JabRefFrame parent;

        BibtexEntry selectedEntry;

        public GenerateKeyAction(JabRefFrame parentFrame) {
            super(Globals.lang("Generate BibTeX key"), GUIGlobals.getImage("makeKey"));
            parent = parentFrame;

            // selectedEntry = newEntry ;
            putValue(SHORT_DESCRIPTION, Globals.lang("Generate BibTeX key"));

            // putValue(MNEMONIC_KEY, GUIGlobals.showGenKeyCode);
        }

        public void actionPerformed(ActionEvent e) {
            // 1. get Bitexentry for selected index (already have)
            // 2. run the LabelMaker by it
            try {
                // Store the current edit in case this action is called during the
                // editing of a field:
                storeCurrentEdit();

                // this updates the table automatically, on close, but not
                // within the tab
                Object oldValue = entry.getField(BibtexFields.KEY_FIELD);

                // entry = frame.labelMaker.applyRule(entry, panel.database) ;
                LabelPatternUtil.makeLabel(prefs.getKeyPattern(), panel.database, entry);

                // Store undo information:
                panel.undoManager.addEdit(new UndoableKeyChange(panel.database, entry.getId(),
                    (String) oldValue, entry.getField(BibtexFields.KEY_FIELD)));

                // here we update the field
                String bibtexKeyData = entry.getField(BibtexFields.KEY_FIELD);

                // set the field named for "bibtexkey"
                setField(BibtexFields.KEY_FIELD, bibtexKeyData);
                updateSource();
                panel.markBaseChanged();
            } catch (Throwable t) {
                System.err.println("error setting key: " + t);
            }
        }
    }

    class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo", GUIGlobals.getImage("undo"));
            putValue(SHORT_DESCRIPTION, "Undo");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                panel.runCommand("undo");
            } catch (Throwable ex) {
            }
        }
    }

    class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Undo", GUIGlobals.getImage("redo"));
            putValue(SHORT_DESCRIPTION, "Redo");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                panel.runCommand("redo");
            } catch (Throwable ex) {
            }
        }
    }

    class SaveDatabaseAction extends AbstractAction {
        public SaveDatabaseAction() {
            super("Save database");
        }

        public void actionPerformed(ActionEvent e) {
            Object activeTab = tabs.get(tabbed.getSelectedIndex());
            if (activeTab instanceof EntryEditorTab) {
                // Normal panel.
                EntryEditorTab fp = (EntryEditorTab) activeTab;
                FieldEditor fe = fp.getActive();
                fe.clearAutoCompleteSuggestion();
                updateField(fe);
            } else
                // Source panel.
                updateField(activeTab);

            try {
                panel.runCommand("save");
            } catch (Throwable ex) {
            }
        }
    }

    class ExternalViewerListener extends MouseAdapter {
        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                FieldTextArea tf = (FieldTextArea) evt.getSource();

                if (tf.getText().equals(""))
                    return;

                tf.selectAll();

                String link = tf.getText(); // get selected ? String

                // getSelectedText()
                try {
                    Util.openExternalViewer(panel.metaData(), link, tf.getFieldName());
                } catch (IOException ex) {
                    System.err.println("Error opening file.");
                }
            }
        }
    }

    class ChangeTypeAction extends AbstractAction {
        BibtexEntryType type;

        BasePanel panel;

        public ChangeTypeAction(BibtexEntryType type, BasePanel bp) {
            super(type.getName());
            this.type = type;
            panel = bp;
        }

        public void actionPerformed(ActionEvent evt) {
            panel.changeType(entry, type);
        }
    }

    private void warnDuplicateBibtexkey() {
        panel.output(Globals.lang("Warning") + ": " + Globals.lang("Duplicate BibTeX key."));

        if (prefs.getBoolean("dialogWarningForDuplicateKey")) {
            // JZTODO lyrics
            CheckBoxMessage jcb = new CheckBoxMessage(Globals.lang("Warning") + ": "
                + Globals.lang("Duplicate BibTeX key. Grouping may not work for this entry."),
                Globals.lang("Disable this warning dialog"), false);
            JOptionPane.showMessageDialog(frame, jcb, Globals.lang("Warning"),
                JOptionPane.WARNING_MESSAGE);

            if (jcb.isSelected())
                prefs.putBoolean("dialogWarningForDuplicateKey", false);
        }
    }

    private void warnEmptyBibtexkey() {
        // JZTODO lyrics
        panel.output(Globals.lang("Warning") + ": " + Globals.lang("Empty BibTeX key."));

        if (prefs.getBoolean("dialogWarningForEmptyKey")) {
            // JZTODO lyrics
            CheckBoxMessage jcb = new CheckBoxMessage(Globals.lang("Warning") + ": "
                + Globals.lang("Empty BibTeX key. Grouping may not work for this entry."), Globals
                .lang("Disable this warning dialog"), false);
            JOptionPane.showMessageDialog(frame, jcb, Globals.lang("Warning"),
                JOptionPane.WARNING_MESSAGE);

            if (jcb.isSelected())
                prefs.putBoolean("dialogWarningForEmptyKey", false);
        }
    }


    class AutoSetFileAction extends AbstractAction {
        public AutoSetFileAction() {
            putValue(SMALL_ICON, GUIGlobals.getImage("autoGroup"));
            putValue(SHORT_DESCRIPTION, Globals.lang("Automatically set file links for this entry")
                    +" (Alt-F)");
        }

        public void actionPerformed(ActionEvent event) {
            JDialog diag = new JDialog(frame, true);
            final FileListTableModel tableModel = new FileListTableModel();
            tableModel.setContent(entry.getField(GUIGlobals.FILE_FIELD));
            FileListEditor.autoSetLinks(entry, tableModel, panel.metaData(), new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getID() > 0) {
                        entry.setField(GUIGlobals.FILE_FIELD, tableModel.getStringRepresentation());
                        frame.output(Globals.lang("Finished autosetting external links."));
                    }
                    else frame.output(Globals.lang("Finished autosetting external links.")
                        +" "+Globals.lang("No files found."));
                }
            }, diag);
        }
    }
}
