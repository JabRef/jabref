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

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;

import net.sf.jabref.export.LatexFieldFormatter;
import net.sf.jabref.groups.*;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.labelPattern.LabelPatternUtil;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.undo.*;


public class EntryEditor extends JPanel implements VetoableChangeListener {
  /*
   * GUI component that allows editing of the fields of a BibtexEntry.
   * EntryTypeForm also registers itself as a VetoableChangeListener, receiving
   * events whenever a field of the entry changes, enabling the text fields to
   * update themselves if the change is made from somewhere else.
   */

  // A reference to the entry this object works on.
  BibtexEntry entry;
  BibtexEntryType type;
  CloseAction closeAction;

  // The action concerned with closing the window.
  DeleteAction deleteAction = new DeleteAction();

  // The action that deletes the current entry, and closes the editor.
  CopyKeyAction copyKeyAction;

  // The action concerned with copying the BibTeX key to the clipboard.
  AbstractAction nextEntryAction = new NextEntryAction();

  // The action concerned with copying the BibTeX key to the clipboard.
  AbstractAction prevEntryAction = new PrevEntryAction();

  // Actions for switching to next/previous entry.
  StoreFieldAction storeFieldAction;

  // The action concerned with storing a field value.
  SwitchLeftAction switchLeftAction = new SwitchLeftAction();
  SwitchRightAction switchRightAction = new SwitchRightAction();

  // The actions concerned with switching the panels.
  GenerateKeyAction generateKeyAction;

  // The action which generates a bibtexkey for this entry.
  SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction();
  JPanel mainPanel = new JPanel();
  JPanel srcPanel = new JPanel();
    EntryEditorTab genPan, optPan, reqPan, absPan;

  JTextField bibtexKey;
  FieldTextField tf;
  JTextArea source;
  JToolBar tlb;
  JTabbedPane tabbed = new JTabbedPane(); //JTabbedPane.RIGHT);
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints con;
  JLabel lab;
  TypeLabel typeLabel;
  JabRefFrame frame;
  BasePanel panel;
  EntryEditor ths = this;
  HashSet contentSelectors = new HashSet();
  Logger logger = Logger.getLogger(EntryEditor.class.getName());
  boolean updateSource = true; // This can be set to false to stop the source

    List tabs = new ArrayList();

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

  //private final int REQ=0, OPT=1, GEN=2, FIELD_WIDTH=40, FIELD_HEIGHT=2;
  private final String KEY_PROPERTY = "bibtexkey";
  JabRefPreferences prefs;
  HelpAction helpAction;
  UndoAction undoAction = new UndoAction();
  RedoAction redoAction = new RedoAction();

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

    BorderLayout bl = new BorderLayout();
    setLayout(bl);
    setupToolBar();
    setupFieldPanels();
    setupSourcePanel();
    tabbed.addChangeListener(new TabListener());
    add(tabbed, BorderLayout.CENTER);

    if (prefs.getBoolean("defaultShowSource"))
      tabbed.setSelectedIndex(sourceIndex);

    updateAllFields();
  }

    private void setupFieldPanels() {
	tabbed.removeAll();
	tabs.clear();
	String[] fields = entry.getRequiredFields();
	if (fields != null) {
	    reqPan = new EntryEditorTab(java.util.Arrays.asList(fields), this, true);
	    tabbed.addTab(Globals.lang("Required fields"),
			  new ImageIcon(GUIGlobals.showReqIconFile), reqPan.getPane(),
			  Globals.lang("Show required fields"));
	    tabs.add(reqPan);
	}
	if ((entry.getOptionalFields() != null) && (entry.getOptionalFields().length >= 1)) {
	    optPan = new EntryEditorTab(java.util.Arrays.asList(entry.getOptionalFields()), this, false);
	    tabbed.addTab(Globals.lang("Optional fields"),
			  new ImageIcon(GUIGlobals.showOptIconFile), optPan.getPane(),
			  Globals.lang("Show optional fields"));
	    tabs.add(optPan);
	}
	
	EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
	for (int i=0; i<tabList.getTabCount(); i++) {
	    EntryEditorTab newTab = new EntryEditorTab(tabList.getTabFields(i), this, false);
	    tabbed.addTab(tabList.getTabName(i), new ImageIcon(GUIGlobals.showGenIconFile), newTab.getPane());
	    tabs.add(newTab);
	}
	/*
	if ((entry.getGeneralFields() != null) && (entry.getGeneralFields().length >= 1)) {
	    
	    genPan = new EntryEditorTab(java.util.Arrays.asList(entry.getGeneralFields()), this, false);
	    
	    tabbed.addTab(Globals.lang("General fields"),
			  new ImageIcon(GUIGlobals.showGenIconFile), genPan.getPane(),
			  Globals.lang("Show general fields"));
	    tabs.add(genPan);    
	}
	
	String[] absFields = new String[] {"abstract", "annote"};
	absPan = new EntryEditorTab(java.util.Arrays.asList(absFields), this, false);	
	tabbed.addTab("Abstract", new ImageIcon(GUIGlobals.showAbsIconFile),
		      absPan.getPane(), Globals.lang("Show abstract"));
		      tabs.add(absPan);*/
	tabbed.addTab(Globals.lang("BibTeX source"),
		      new ImageIcon(GUIGlobals.sourceIconFile), srcPanel,
		      Globals.lang("Show/edit BibTeX source"));
	tabs.add(srcPanel);
	sourceIndex = tabs.size() - 1; // Set the sourceIndex variable.
	
    }

  public BibtexEntryType getType() {
    return type;
  }

  private void setupToolBar() {
    tlb = new JToolBar(JToolBar.VERTICAL);

    //tlb.setMargin(new Insets(2,2,2,2));
    tlb.setMargin(new Insets(0, 0, 0, 2));

    // The toolbar carries all the key bindings that are valid for the whole
    // window.
    //tlb.setBackground(GUIGlobals.lightGray);//Color.white);
    ActionMap am = tlb.getActionMap();
    InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

    im.put(prefs.getKey("Close entry editor"), "close");
    am.put("close", closeAction);
    im.put(prefs.getKey("Entry editor, store field"), "store");
    am.put("store", storeFieldAction);
    im.put(prefs.getKey("Autogenerate BibTeX keys"), "generateKey");
    am.put("generateKey", generateKeyAction);
    im.put(prefs.getKey("Entry editor, previous panel"), "left");
    am.put("left", switchLeftAction);
    im.put(prefs.getKey("Entry editor, next panel"), "right");
    am.put("right", switchRightAction);
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
    tlb.add(closeAction);

    setLabel();
    tlb.add(typeLabel);

    //tlb.addSeparator();
    //tlb.add(copyKeyAction);
    tlb.addSeparator();
    tlb.add(generateKeyAction);
    tlb.addSeparator();

    //tlb.add(undoAction);
    //tlb.add(redoAction);
    tlb.add(deleteAction);
    tlb.add(prevEntryAction);

    tlb.add(nextEntryAction);
    tlb.addSeparator();
    tlb.add(helpAction);

    Component[] comps = tlb.getComponents();

    for (int i = 0; i < comps.length; i++)
      ((JComponent) comps[i]).setOpaque(false);

    add(tlb, BorderLayout.WEST);
  }

  private void setLabel() {
    typeLabel = new TypeLabel(entry.getType().getName());
  }

  public void rebuildPanels() {
      setupFieldPanels();//reqPanel, optPanel, genPanel, absPanel);
    revalidate();
    repaint();
  }

 
  /**
   * getExtra checks the field name against GUIGlobals.FIELD_EXTRAS. If the name
   * has an entry, the proper component to be shown is created and returned.
   * Otherwise, null is returned. In addition, e.g. listeners can be added to
   * the field editor, even if no component is returned.
   *
   * @param string
   *          Field name
   * @return Component to show, or null if none.
   */
  public JComponent getExtra(String string, FieldEditor editor) {
    final FieldEditor ed = editor;
    Object o = GUIGlobals.FIELD_EXTRAS.get(string);
    final String fieldName = editor.getFieldName();

    //if (o == null)
    //  return null;
    String s = (String) o;

    if ((s != null) && s.equals("external")) {
      // Add external viewer listener for "pdf" and "url" fields.
      ((JComponent) editor).addMouseListener(new ExternalViewerListener());

      return null;
    } else if (panel.metaData.getData(Globals.SELECTOR_META_PREFIX
          + editor.getFieldName()) != null) {
      FieldContentSelector ws = new FieldContentSelector(this, editor, panel.metaData);
      contentSelectors.add(ws);

      return ws;
    } else if ((s != null) && s.equals("browse")) {
      JButton but = new JButton(Globals.lang("Browse"));
      ((JComponent) editor).addMouseListener(new ExternalViewerListener());

      //but.setBackground(GUIGlobals.lightGray);
      but.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String dir = ed.getText();

            if (dir.equals(""))
              dir = prefs.get(fieldName + Globals.FILETYPE_PREFS_EXT, "");

            String chosenFile =
              Globals.getNewFile(frame, prefs, new File(dir), "." + fieldName,
                JFileChooser.OPEN_DIALOG, false);

            /*
             * JabRefFileChooser chooser = new JabRefFileChooser (new
             * File(ed.getText())); if (ed.getText().equals("")) {
             * chooser.setCurrentDirectory(new File(prefs.get(fieldName +
             * Globals.FILETYPE_PREFS_EXT, ""))); }
             * //chooser.addChoosableFileFilter(new OpenFileFilter()); //nb nov2
             * int returnVal = chooser.showOpenDialog(null); if (returnVal ==
             * JFileChooser.APPROVE_OPTION) {
             */
            if (chosenFile != null) {
              File newFile = new File(chosenFile); //chooser.getSelectedFile();
              ed.setText(newFile.getPath());
              prefs.put(fieldName + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
              updateField(ed);
            }
          }
        });

      return but;
    } else if ((s != null) && s.equals("browsePdf")) {
      JPanel pan = new JPanel();
      pan.setLayout(new GridLayout(2, 1));

      JButton but = new JButton(Globals.lang("Browse"));
      JButton download = new JButton(Globals.lang("Download"));

      //       auto = new JButton( Globals.lang( "Auto" ) ) ;
      ((JComponent) editor).addMouseListener(new ExternalViewerListener());

      //but.setBackground(GUIGlobals.lightGray);
      but.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String pdfDir = prefs.get("pdfDirectory");
            String dir = ed.getText();

            if (dir.equals("") || !(new File(dir)).isAbsolute()) {
              if (pdfDir != null)
                dir = pdfDir;
              else
                dir = prefs.get(fieldName + Globals.FILETYPE_PREFS_EXT, "");
            }

            String chosenFile =
              Globals.getNewFile(frame, prefs, new File(dir), ".pdf",
                JFileChooser.OPEN_DIALOG, false);

            if (chosenFile != null) {
              File newFile = new File(chosenFile);
              String position = newFile.getParent();

              if ((pdfDir != null) && position.startsWith(pdfDir)) {
                // Construct path relative to pdf base dir
                String relPath =
                  position.substring(pdfDir.length(), position.length()) + File.separator
                  + newFile.getName();

                // Remove leading path separator
                if (relPath.startsWith(File.separator)) {
                  relPath = relPath.substring(File.separator.length(), relPath.length());

                  // Set relative path as field value
                }

                ed.setText(relPath);
              } else
                ed.setText(newFile.getPath());

              prefs.put(fieldName + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
              storeFieldAction.actionPerformed(new ActionEvent(ed, 0, ""));
            }
          }
        });
      download.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String res =
              JOptionPane.showInputDialog((Component) ed,
                Globals.lang("Enter URL to download"));

            if (res != null) {
              URL url;

              try {
                url = new URL(res);

                File file =
                  new File(new File(prefs.get("pdfDirectory")),
                    entry.getField(Globals.KEY_FIELD) + ".pdf");
                URLDownload udl = new URLDownload((Component) ed, url, file);
                frame.output(Globals.lang("Downloading..."));

                try {
                  udl.download();
                } catch (IOException e2) {
                  JOptionPane.showMessageDialog((Component) ed, "Invalid URL",
                    "Download file", JOptionPane.ERROR_MESSAGE);
                  logger.log(java.util.logging.Level.WARNING,
                    "Error while downloading " + url.toString(), e2);
                }

                frame.output(Globals.lang("Download completed"));
                ed.setText(file.toURL().toString());
                updateField(ed);
              } catch (MalformedURLException e1) {
                JOptionPane.showMessageDialog((Component) ed, "Invalid URL",
                  "Download file", JOptionPane.ERROR_MESSAGE);
              }
            }
          }
        });

      /*
       * Erik: I propose to use the Download instead... auto.addActionListener(
       * new ActionListener() { public void actionPerformed( ActionEvent e ) {
       * Object o = entry.getField( Globals.KEY_FIELD ) ; if ( ( o == null ) || (
       * prefs.get( "pdfDirectory" ) == null ) ) { frame.output( Globals.lang(
       * "You must set both bibtex key and PDF directory" ) + "." ) ; return ; }
       * panel.output( Globals.lang( "Searching for PDF file" ) + " '" + o +
       * ".pdf'..." ) ; ( new Thread() { public void run() { Object o =
       * entry.getField( Globals.KEY_FIELD ) ; String found = Util.findPdf( (
       * String ) o, prefs.get( "pdfDirectory" ) ) ; if ( found != null ) {
       * ed.setText( found ) ; updateField(ed); panel.output( Globals.lang( "PDF
       * field set" ) + "." ) ; } else { panel.output( Globals.lang( "No PDF
       * found" ) + "." ) ; } } } ).start() ; } } ) ; pan.add( auto ) ;
       */
      pan.add(but);
      pan.add(download);

      // Add drag and drop support to the field
      ((JComponent) editor).setDropTarget(new DropTarget((Component) editor,
          DnDConstants.ACTION_NONE, new UrlDragDrop(this, frame, editor)));

      return pan;
    } else if ((s != null) && s.equals("url")) {
      ((JComponent) editor).setDropTarget(new DropTarget((Component) editor,
          DnDConstants.ACTION_NONE, new SimpleUrlDragDrop(editor, storeFieldAction)));

      return null;
    } else
      return null;
  }

  private void setupSourcePanel() {
    source =
      new JTextArea() {
          public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
              RenderingHints.VALUE_RENDER_QUALITY);
            super.paintComponent(g2);
          }
        };
    con = new GridBagConstraints();
    con.insets = new Insets(10, 10, 10, 10);
    con.fill = GridBagConstraints.BOTH;
    con.gridwidth = GridBagConstraints.REMAINDER;
    con.gridheight = GridBagConstraints.REMAINDER;
    con.weightx = 1;
    con.weighty = 1;
    srcPanel.setLayout(gbl);
    source.setEditable(true); //prefs.getBoolean("enableSourceEditing"));
    source.setLineWrap(true);
    source.setTabSize(GUIGlobals.INDENT);

    // Add the global focus listener, so a menu item can see if this field
    // was focused when
    // an action was called.
    source.addFocusListener(Globals.focusListener);

    setupJTextComponent(source);
    updateSource();

    JScrollPane sp =
      new JScrollPane(source, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    gbl.setConstraints(sp, con);
    srcPanel.add(sp);
  }

  private void updateSource() {
    if (updateSource) {
      StringWriter sw = new StringWriter(200);

      try {
        entry.write(sw, new net.sf.jabref.export.LatexFieldFormatter(), false);

        String srcString = sw.getBuffer().toString();
        source.setText(srcString);
        lastSourceStringAccepted = srcString;
      } catch (IOException ex) {
        source.setText("Error: " + ex.getMessage() + "\n\n" + "Correct the entry, and "
          + "reopen editor to display/edit source.");
        source.setEditable(false);
      }
    }
  }

  public void setupJTextComponent(JTextComponent ta) {

    /*
     * if ((ta instanceof FieldTextArea) && (prefs.getBoolean("autoComplete"))) {
     * FieldTextArea fta = (FieldTextArea)ta; Completer comp =
     * baseFrame.getAutoCompleter(fta.getFieldName()); if (comp != null)
     * fta.setAutoComplete(comp); }
     */

    // Set up key bindings and focus listener for the FieldEditor.
    InputMap im = ta.getInputMap(JComponent.WHEN_FOCUSED);
    ActionMap am = ta.getActionMap();

    //im.put(KeyStroke.getKeyStroke(GUIGlobals.closeKey), "close");
    //am.put("close", closeAction);
    im.put(prefs.getKey("Entry editor, store field"), "store");
    am.put("store", storeFieldAction);
    im.put(prefs.getKey("Entry editor, next panel"), "right");
    am.put("left", switchLeftAction);
    im.put(prefs.getKey("Entry editor, previous panel"), "left");
    am.put("right", switchRightAction);
    im.put(prefs.getKey("Help"), "help");
    am.put("help", helpAction);
    im.put(prefs.getKey("Save database"), "save");
    am.put("save", saveDatabaseAction);

    try {
      HashSet keys =
        new HashSet(ta.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
      keys.clear();
      keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
      ta.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
      keys =
        new HashSet(ta.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
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
	    ((EntryEditorTab)activeTab).activate();
	else
	    ((JComponent)activeTab).requestFocus();
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
      for (Iterator i=tabs.iterator(); i.hasNext();) {
	  Object o = i.next();
	  if (o instanceof EntryEditorTab) {
	      ((EntryEditorTab)o).setEnabled(enabled);
	  }
      }
      source.setEnabled(enabled);

  }

  /**
   * Centers the given row, and highlights it.
   *
   * @param row
   *          an <code>int</code> value
   */
  private void scrollTo(int row) {
    panel.entryTable.setRowSelectionInterval(row, row);
    panel.entryTable.ensureVisible(row);
  }

  /**
   * Switches the entry for this editor to the one with the given id. If the
   * target entry is of the same type as the current, field values are simply
   * updated. Otherwise, a new editor created to replace this one.
   *
   * @param id
   *          a <code>String</code> value
   */
  private void switchTo(String id) {
    // Make sure the current edit is stored.
      Util.pr("frilp");
    Object activeTab = tabs.get(tabbed.getSelectedIndex());
    if (activeTab instanceof EntryEditorTab) {
        updateField(((EntryEditorTab)activeTab).getActive());
    }

    BibtexEntry be = panel.database.getEntryById(id);

    // If the entry we are switching to is of the same type as
    // this one, we can make the switch more elegant by keeping this
    // same dialog, and updating it.
    if (entry.getType() == be.getType())
      switchTo(be);
    else
      panel.showEntry(be);
  }

  /**
   * Returns the index of the active (visible) panel.
   *
   * @return an <code>int</code> value
   */
  public int getVisiblePanel() {
    return tabbed.getSelectedIndex();
  }

  /**
   * Sets the panel with the given index visible.
   *
   * @param i
   *          an <code>int</code> value
   */
  public void setVisiblePanel(int i) {
    if (i < tabbed.getTabCount())
      tabbed.setSelectedIndex(i);
    else {
      while (i >= tabbed.getTabCount())
        i--;

      tabbed.setSelectedIndex(i);
    }
  }

  /**
   * Updates this editor to show the given entry, regardless of type
   * correspondence.
   *
   * @param be
   *          a <code>BibtexEntry</code> value
   */
  public void switchTo(BibtexEntry be) {
    entry = be;
    updateAllFields();
    validateAllFields();
    updateSource();
    panel.showing = be;

    activateVisible();
  }

  /**
   * Returns false if the contents of the source panel has not been validated,
   * true othervise.
   */
  public boolean lastSourceAccepted() {
    //Util.pr("Sourceaccepted ....");
    if (tabbed.getSelectedComponent() == srcPanel)
      storeSource(false);

    return lastSourceAccepted;
  }

  /*
   * public boolean storeSourceIfNeeded() { if (tabbed.getSelectedIndex() ==
   * sourceIndex) return storeSource(); else return true; }
   */
  public boolean storeSource(boolean showError) {
    //Util.pr("StoreSource");
    // Store edited bibtex code.
    BibtexParser bp = new BibtexParser(new java.io.StringReader(source.getText()));

    try {
      BibtexDatabase db = bp.parse().getDatabase();

      if (db.getEntryCount() > 1)
        throw new Exception("More than one entry found.");

      if (db.getEntryCount() < 1)
        throw new Exception("No entries found.");

      NamedCompound compound = new NamedCompound("source edit");
      BibtexEntry nu = db.getEntryById((String) db.getKeySet().iterator().next());
      String id = entry.getId();
      String 
      //oldKey = entry.getCiteKey(),
      newKey = nu.getCiteKey();
      boolean anyChanged = false;
      boolean duplicateWarning = false;
      boolean emptyWarning = newKey == null || newKey.equals(""); 

      if (panel.database.setCiteKeyForEntry(id, newKey)) {
        duplicateWarning = true;

        // First, remove fields that the user have removed.
      }

      Object[] fields = entry.getAllFields();

      for (int i = 0; i < fields.length; i++) {
        if (GUIGlobals.isWriteableField(fields[i].toString())) {
          if (nu.getField(fields[i].toString()) == null) {
            compound.addEdit(new UndoableFieldChange(entry, fields[i].toString(),
                entry.getField(fields[i].toString()), (Object) null));
            entry.clearField(fields[i].toString());
            anyChanged = true;
          }
        }
      }

      // Then set all fields that have been set by the user.
      fields = nu.getAllFields();

      for (int i = 0; i < fields.length; i++) {
        if (entry.getField(fields[i].toString()) != nu.getField(fields[i].toString())) {
          String toSet = (String) nu.getField(fields[i].toString());

          // Test if the field is legally set.
          (new LatexFieldFormatter()).format(toSet,
            GUIGlobals.isStandardField(fields[i].toString()));

          compound.addEdit(new UndoableFieldChange(entry, fields[i].toString(),
              entry.getField(fields[i].toString()), toSet));
          entry.setField(fields[i].toString(), toSet);
          anyChanged = true;
        }
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
      } else if (emptyWarning) {
        warnEmptyBibtexkey();
      } else {
        panel.output(Globals.lang("Stored entry") + ".");
      }
      
      lastSourceStringAccepted = source.getText();
      updateAllFields();
      lastSourceAccepted = true;
      updateSource = true;

      //panel.refreshTable();
      panel.markBaseChanged();

      return true;
    } catch (Throwable ex) {
      //ex.printStackTrace();
      // The source couldn't be parsed, so the user is given an
      // error message, and the choice to keep or revert the contents
      // of the source text field.
      updateSource = false;
      lastSourceAccepted = false;
      tabbed.setSelectedComponent(srcPanel);

      if (showError) {
        Object[] options =
          { Globals.lang("Edit"), Globals.lang("Revert to original source") };

        int answer =
          JOptionPane.showOptionDialog(frame, "Error: " + ex.getMessage(),
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
      
      for (Iterator i=tabs.iterator(); i.hasNext();) {
	  Object o = i.next();
	  if (o instanceof EntryEditorTab) {
	      ((EntryEditorTab)o).updateField(fieldName, newFieldData);
	  }
      }
      
  }

  /**
   * Sets all the text areas according to the shown entry.
   */
  public void updateAllFields() {

      for (Iterator i=tabs.iterator(); i.hasNext();) {
	  Object o = i.next();
	  if (o instanceof EntryEditorTab) {
	      ((EntryEditorTab)o).setEntry(entry);
	  }
      }

  }

  /**
   * Removes the "invalid field" color from all text areas.
   */
  public void validateAllFields() {
      for (Iterator i=tabs.iterator(); i.hasNext();) {
	  Object o = i.next();
	  if (o instanceof EntryEditorTab) {
	      ((EntryEditorTab)o).validateAllFields();
	  }
      }
  }

  public void updateAllContentSelectors() {
    if (contentSelectors.size() > 0) {
      for (Iterator i = contentSelectors.iterator(); i.hasNext();)
        ((FieldContentSelector) i.next()).updateList();
    }
  }

  // Update the JTextArea when a field has changed.
  public void vetoableChange(PropertyChangeEvent e) {
    String newValue = ((e.getNewValue() != null) ? e.getNewValue().toString() : "");
    setField(e.getPropertyName(), newValue);

    //Util.pr(e.getPropertyName());
  }

  /**
   * @param ed
   */
  public void updateField(final Object source) {
    storeFieldAction.actionPerformed(new ActionEvent(source, 0, ""));
  }

  private class TypeLabel extends JPanel {
    private String label;

    public TypeLabel(String type) {
      label = type;
      addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent e) {
            boolean ctrlClick = prefs.getBoolean("ctrlClick");

            if ((e.getButton() == MouseEvent.BUTTON3)
                || (ctrlClick && (e.getButton() == MouseEvent.BUTTON1)
                && e.isControlDown())) {
              JPopupMenu typeMenu = new JPopupMenu();

              //typeMenu.add(new JLabel(Globals.lang("Set entry
              // type")));
              //typeMenu.addSeparator();
              for (Iterator i = BibtexEntryType.ALL_TYPES.keySet().iterator();
                  i.hasNext();)
                typeMenu.add(new ChangeTypeAction(BibtexEntryType.getType(
                      (String) i.next()), panel));

              typeMenu.show(ths, e.getX(), e.getY());
            }
          }
        });
    }

    public void paint(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(GUIGlobals.validFieldColor);
      g2.setFont(GUIGlobals.typeNameFont);

      FontMetrics fm = g2.getFontMetrics();
      int width = fm.stringWidth(label);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
      g2.rotate(-Math.PI / 2, 0, 0);
      g2.drawString(label, -width - 7, 28);
    }
  }

  class FieldListener extends FocusAdapter {
    /*
     * Focus listener that fires the storeFieldAction when a FieldTextArea loses
     * focus.
     */
    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
      //Util.pr("Lost focus "+e.getSource().toString().substring(0,30));
      if (!e.isTemporary())
        updateField(e.getSource());
    }
  }


  class TabListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
	activateVisible();
    }
  }

    class DeleteAction extends AbstractAction {
    public DeleteAction() {
      super(Globals.lang("Delete"), new ImageIcon(GUIGlobals.removeIconFile));
      putValue(SHORT_DESCRIPTION, Globals.lang("Delete entry"));
    }

    public void actionPerformed(ActionEvent e) {
      // Show confirmation dialog if not disabled:
      boolean goOn = panel.showDeleteConfirmationDialog(1);

      if (!goOn)
        return;

      panel.hideEntryEditor();
      panel.database.removeEntry(entry.getId());
      panel.markBaseChanged();
      panel.refreshTable();
      panel.undoManager.addEdit(new UndoableRemoveEntry(panel.database, entry, panel));
      panel.output(Globals.lang("Deleted") + " " + Globals.lang("entry"));
    }
  }

  class CloseAction extends AbstractAction {
    public CloseAction() {
      super(Globals.lang("Close window"), new ImageIcon(GUIGlobals.closeIconFile));
      putValue(SHORT_DESCRIPTION, Globals.lang("Close window"));
    }

    public void actionPerformed(ActionEvent e) {
      /*
       * Thread t = new Thread() { public void run() { panel.hideEntryEditor(); } };
       */
      if (tabbed.getSelectedComponent() == srcPanel) {
        updateField(source);

        if (lastSourceAccepted)
          //SwingUtilities.invokeLater(t);
          panel.hideEntryEditor();
      } else
        //SwingUtilities.invokeLater(t);
        panel.hideEntryEditor();
    }
  }

  class CopyKeyAction extends AbstractAction {
    public CopyKeyAction() {
      super("Copy BibTeX key to clipboard", new ImageIcon(GUIGlobals.copyKeyIconFile));
      putValue(SHORT_DESCRIPTION, "Copy BibTeX key to clipboard (Ctrl-K)");

      //putValue(MNEMONIC_KEY, GUIGlobals.copyKeyCode);
    }

    public void actionPerformed(ActionEvent e) {
      String s = (String) (entry.getField(KEY_PROPERTY));
      StringSelection ss = new StringSelection(s);

      if (s != null)
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
    }
  }

  class StoreFieldAction extends AbstractAction {
    public StoreFieldAction() {
      super("Store field value");
      putValue(SHORT_DESCRIPTION, "Store field value");
    }

    public void actionPerformed(ActionEvent e) {
      if (e.getSource() instanceof FieldTextArea) {
        String toSet = null;
        FieldEditor fe = (FieldEditor) e.getSource();
        boolean set;

        // Trim the whitespace off this value
        fe.setText(fe.getText().trim());

        if (fe.getText().length() > 0) {
          toSet = fe.getText();

          // We check if the field has changed, since we don't want to
          // mark the
          // base as changed unless we have a real change.
        }

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
              (new LatexFieldFormatter()).format(toSet,
                GUIGlobals.isStandardField(fe.getFieldName()));

            Object oldValue = entry.getField(fe.getFieldName());

            if (toSet != null)
              entry.setField(fe.getFieldName(), toSet);
            else
              entry.clearField(fe.getFieldName());

            if ((toSet != null) && (toSet.length() > 0))
              //fe.setLabelColor(GUIGlobals.validFieldColor);
              fe.setBackground(GUIGlobals.validFieldBackground);
            else
              //fe.setLabelColor(GUIGlobals.nullFieldColor);
              fe.setBackground(GUIGlobals.validFieldBackground);

            // Add an UndoableFieldChange to the baseframe's
            // undoManager.
            panel.undoManager.addEdit(new UndoableFieldChange(entry, fe.getFieldName(),
                oldValue, toSet));
            updateSource();
            panel.refreshTable();
            panel.markBaseChanged();
          } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(),
              Globals.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
            fe.setBackground(GUIGlobals.invalidFieldBackground);
          }
        }
        else {
          // set == false
          // We set the field and label color.
          fe.setBackground(GUIGlobals.validFieldBackground);

          /*
           * fe.setLabelColor((toSet == null) ? GUIGlobals.nullFieldColor :
           * GUIGlobals.validFieldColor);
           */
        }
      } else if (e.getSource() instanceof FieldTextField) {
        // Storage from bibtex key field.
        FieldTextField fe = (FieldTextField) e.getSource();
        String oldValue = entry.getCiteKey();
        String newValue = fe.getText();

        if (newValue.equals(""))
          newValue = null;

        if (((oldValue == null) && (newValue == null))
            || ((oldValue != null) && (newValue != null) && oldValue.equals(newValue)))
          return; // No change.

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
          //fe.setLabelColor(GUIGlobals.validFieldColor);
          fe.setBackground(GUIGlobals.validFieldBackground);
        else
          //fe.setLabelColor(GUIGlobals.nullFieldColor);
          fe.setBackground(GUIGlobals.validFieldBackground);

        updateSource();
        panel.refreshTable();
        panel.markBaseChanged();
      } else if ((source.isEditable())
          && (!source.getText().equals(lastSourceStringAccepted))) {
        boolean accepted = storeSource(true);

        if (accepted) {
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
      tabbed.setSelectedIndex(((i > 0) ? (i - 1) : (tabbed.getTabCount() - 1)));

      activateVisible();
    }
  }

  class SwitchRightAction extends AbstractAction {
    public SwitchRightAction() {
      super("Switch to the panel to the right");
    }

    public void actionPerformed(ActionEvent e) {
      int i = tabbed.getSelectedIndex();
      tabbed.setSelectedIndex((i < (tabbed.getTabCount() - 1)) ? (i + 1) : 0);
      activateVisible();

    }
  }

  class NextEntryAction extends AbstractAction {
    public NextEntryAction() {
      super(Globals.lang("Next entry"), new ImageIcon(GUIGlobals.downIconFile));

      putValue(SHORT_DESCRIPTION, Globals.lang("Next entry"));
    }

    public void actionPerformed(ActionEvent e) {
      int thisRow = panel.tableModel.getNumberFromName(entry.getId());
      String id = null;
      int newRow = -1;

      if ((thisRow + 1) < panel.database.getEntryCount())
        newRow = thisRow + 1;
      else if (thisRow > 0)
        newRow = 0;
      else
        return; // newRow is still -1, so we can assume the database

      // has only one entry.
      id = panel.tableModel.getNameFromNumber(newRow);
      switchTo(id);

      final int nr = newRow;
      (new Thread() {
          public void run() {
            scrollTo(nr);
          }
        }).start();
    }
  }

  class PrevEntryAction extends AbstractAction {
    public PrevEntryAction() {
      super(Globals.lang("Previous entry"), new ImageIcon(GUIGlobals.upIconFile));

      putValue(SHORT_DESCRIPTION, Globals.lang("Previous entry"));
    }

    public void actionPerformed(ActionEvent e) {
      int thisRow = panel.tableModel.getNumberFromName(entry.getId());
      String id = null;
      int newRow = -1;

      if ((thisRow - 1) >= 0)
        newRow = thisRow - 1;
      else if (thisRow != (panel.database.getEntryCount() - 1))
        newRow = panel.database.getEntryCount() - 1;
      else
        return; // newRow is still -1, so we can assume the database

      // has only one entry.
      id = panel.tableModel.getNameFromNumber(newRow);
      switchTo(id);

      final int nr = newRow;
      (new Thread() {
          public void run() {
            scrollTo(nr);
          }
        }).start();
    }
  }

  class GenerateKeyAction extends AbstractAction {
    JabRefFrame parent;
    BibtexEntry selectedEntry;

    public GenerateKeyAction(JabRefFrame parentFrame) {
      super(Globals.lang("Generate BibTeX key"), new ImageIcon(GUIGlobals.genKeyIconFile));
      parent = parentFrame;

      //            selectedEntry = newEntry ;
      putValue(SHORT_DESCRIPTION, Globals.lang("Generate BibTeX key"));

      //        putValue(MNEMONIC_KEY, GUIGlobals.showGenKeyCode);
    }

    public void actionPerformed(ActionEvent e) {
      // 1. get Bitexentry for selected index (already have)
      // 2. run the LabelMaker by it
      try {
        // this updates the table automatically, on close, but not
        // within the tab
        Object oldValue = entry.getField(GUIGlobals.KEY_FIELD);

        //entry = frame.labelMaker.applyRule(entry, panel.database) ;
        entry = LabelPatternUtil.makeLabel(prefs.getKeyPattern(), panel.database, entry);

        // Store undo information:
        panel.undoManager.addEdit(new UndoableKeyChange(panel.database, entry.getId(),
            (String) oldValue, (String) entry.getField(GUIGlobals.KEY_FIELD)));

        // here we update the field
        String bibtexKeyData = (String) entry.getField(Globals.KEY_FIELD);

        // set the field named for "bibtexkey"
        setField(Globals.KEY_FIELD, bibtexKeyData);
        updateSource();
        panel.markBaseChanged();
        panel.refreshTable();
      } catch (Throwable t) {
        System.err.println("error setting key: " + t);
      }
    }
  }

  class UndoAction extends AbstractAction {
    public UndoAction() {
      super("Undo", new ImageIcon(GUIGlobals.undoIconFile));
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
      super("Undo", new ImageIcon(GUIGlobals.redoIconFile));
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
	      EntryEditorTab fp = (EntryEditorTab)activeTab;
	      updateField(fp.getActive());
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
          Util.openExternalViewer(link, tf.getFieldName(), prefs);
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
  
  /**
   * Scans all groups.
   * @return true if the specified entry is contained in any ExplicitGroup,
   * false otherwise. 
   */
  private boolean containedInExplicitGroup(BibtexEntry entry) {
      AbstractGroup[] matchingGroups = panel.getGroupSelector().getGroupTreeRoot().
      getMatchingGroups(entry);
      for (int i = 0; i < matchingGroups.length; ++i) {
          if (matchingGroups[i] instanceof ExplicitGroup)
              return true;
      }
      return false;
  }
  
  private void warnDuplicateBibtexkey() {
        panel.output(Globals.lang("Warning") + ": "
                + Globals.lang("duplicate BibTeX key."));

        if (prefs.getBoolean("dialogWarningForDuplicateKey")) {
            CheckBoxMessage jcb = new CheckBoxMessage(Globals.lang("Warning")
                    + ": " + Globals.lang("duplicate BibTeX key."), Globals
                    .lang("Disable this warning dialog"), false);
            JOptionPane.showMessageDialog(frame, jcb, Globals.lang("Warning"),
                    JOptionPane.WARNING_MESSAGE);

            if (jcb.isSelected())
                prefs.putBoolean("dialogWarningForDuplicateKey", false);

            // JZTODO lyrics
            if (containedInExplicitGroup(entry)) {
                JOptionPane.showMessageDialog(
                                frame,
                                "Groups assignment for this entry cannot be restored after next load due to duplicate key",
                                Globals.lang("Warning"),
                                JOptionPane.WARNING_MESSAGE);
            }
        }
    }

  private void warnEmptyBibtexkey() {
      // JZTODO lyrics
      if (containedInExplicitGroup(entry)) {
          JOptionPane.showMessageDialog(frame, 
                  "Groups assignment for this entry cannot be restored after next load due to lacking key", 
                  Globals.lang("Warning"),
                  JOptionPane.WARNING_MESSAGE);
      }
  }
}
