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
package net.sf.jabref.gui.entryeditor;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import net.sf.jabref.*;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.fieldeditors.*;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.menus.ChangeEntryTypeMenu;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.external.WriteXMPEntryEditorAction;
import net.sf.jabref.gui.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelPattern.LabelPatternUtil;
import net.sf.jabref.logic.search.SearchQueryHighlightListener;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.*;
import net.sf.jabref.specialfields.SpecialFieldUpdateListener;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableChangeType;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.undo.UndoableKeyChange;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * GUI component that allows editing of the fields of a BibEntry (i.e. the
 * one that shows up, when you double click on an entry in the table)
 * <p>
 * It hosts the tabs (required, general, optional) and the buttons to the left.
 * <p>
 * EntryEditor also registers itself as a VetoableChangeListener, receiving
 * events whenever a field of the entry changes, enabling the text fields to
 * update themselves if the change is made from somewhere else.
 */
public class EntryEditor extends JPanel implements VetoableChangeListener, EntryContainer {
    private static final Log LOGGER = LogFactory.getLog(EntryEditor.class);

    // A reference to the entry this object works on.
    private BibEntry entry;

    // The action concerned with closing the window.
    private final CloseAction closeAction;

    // The action that deletes the current entry, and closes the editor.
    private final DeleteAction deleteAction = new DeleteAction();

    // The action concerned with copying the BibTeX key to the clipboard.
    final AbstractAction nextEntryAction = new NextEntryAction();

    // Actions for switching to next/previous entry.
    final AbstractAction prevEntryAction = new PrevEntryAction();

    // The action concerned with storing a field value.
    public final StoreFieldAction storeFieldAction;

    // The actions concerned with switching the panels.
    final SwitchLeftAction switchLeftAction = new SwitchLeftAction();

    final SwitchRightAction switchRightAction = new SwitchRightAction();

    // The action which generates a bibtexkey for this entry.
    public final GenerateKeyAction generateKeyAction;

    // UGLY HACK to have a pointer to the fileListEditor to call autoSetLinks()
    private FileListEditor fileListEditor;
    private final AutoLinkAction autoLinkAction = new AutoLinkAction();

    private final AbstractAction writeXmp;

    final SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction();

    private final JPanel srcPanel = new JPanel();

    private JTextArea source;

    private final JTabbedPane tabbed = new JTabbedPane(); // JTabbedPane.RIGHT);

    private final JabRefFrame frame;

    private final BasePanel panel;

    private final EntryEditor ths = this;

    private final Set<FieldContentSelector> contentSelectors = new HashSet<>();

    private boolean updateSource = true; // This can be set to false to stop the source
    private boolean movingToDifferentEntry; // Indicates that we are about to go to the next or previous entry

    private final List<Object> tabs = new ArrayList<>();

    // text area from gettin updated. This is used in cases where the source
    // couldn't be parsed, and the user is given the option to edit it.
    private boolean lastSourceAccepted = true; // This indicates whether the last

    // attempt
    // at parsing the source was successful. It is used to determine whether the
    // dialog should close; it should stay open if the user received an error
    // message about the source, whatever he or she chose to do about it.
    private String lastSourceStringAccepted; // This is used to prevent double

    // fields.
    // These values can be used to calculate the preferred height for the form.
    // reqW starts at 1 because it needs room for the bibtex key field.
    private int sourceIndex = -1; // The index the source panel has in tabbed.

    private final JabRefPreferences prefs;

    final HelpAction helpAction;

    private final UndoAction undoAction = new UndoAction();

    private final RedoAction redoAction = new RedoAction();

    private final TabListener tabListener = new TabListener();


    public EntryEditor(JabRefFrame frame, BasePanel panel, BibEntry entry) {

        this.frame = frame;
        this.panel = panel;
        this.entry = entry;
        prefs = Globals.prefs;

        this.entry.addPropertyChangeListener(this);
        this.entry.addPropertyChangeListener(SpecialFieldUpdateListener.getInstance());

        helpAction = new HelpAction(HelpFiles.entryEditorHelp, IconTheme.JabRefIcon.HELP.getIcon());
        closeAction = new CloseAction();
        generateKeyAction = new GenerateKeyAction();
        storeFieldAction = new StoreFieldAction();
        writeXmp = new WriteXMPEntryEditorAction(panel, this);

        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);
        setupToolBar();
        setupFieldPanels();
        setupSourcePanel();
        add(tabbed, BorderLayout.CENTER);
        tabbed.addChangeListener(tabListener);
        if (prefs.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE)) {
            tabbed.setSelectedIndex(sourceIndex);
        }

        updateAllFields();
    }

    private void setupFieldPanels() {
        tabbed.removeAll();
        tabs.clear();

        EntryType type = EntryTypes.getTypeOrDefault(entry.getType(), this.frame.getCurrentBasePanel().getBibDatabaseContext().getMode());

        // required fields
        List<String> requiredFields = type.getRequiredFieldsFlat();
        EntryEditorTab requiredPanel = new EntryEditorTab(frame, panel, requiredFields, this, true, false, Localization.lang("Required fields"));
        if (requiredPanel.fileListEditor != null) {
            fileListEditor = requiredPanel.fileListEditor;
        }
        tabbed.addTab(Localization.lang("Required fields"), IconTheme.JabRefIcon.REQUIRED.getSmallIcon(), requiredPanel
                .getPane(), Localization.lang("Show required fields"));
        tabs.add(requiredPanel);

        // optional fields
        List<String> displayedOptionalFields = new ArrayList<>();

        if ((type.getOptionalFields() != null) && (type.getOptionalFields().size() >= 1)) {
            EntryEditorTab optionalPanel;

            if (!this.frame.getCurrentBasePanel().getBibDatabaseContext().isBiblatexMode()) {
                optionalPanel = new EntryEditorTab(frame, panel, type.getOptionalFields(), this,
                        false, false, Localization.lang("Optional fields"));
                if (optionalPanel.fileListEditor != null) {
                    fileListEditor = optionalPanel.fileListEditor;
                }
                tabbed.addTab(Localization.lang("Optional fields"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(), optionalPanel
                        .getPane(), Localization.lang("Show optional fields"));
                tabs.add(optionalPanel);
            } else {
                displayedOptionalFields.addAll(type.getPrimaryOptionalFields());
                displayedOptionalFields.addAll(type.getSecondaryOptionalFields());

                optionalPanel = new EntryEditorTab(frame, panel, type.getPrimaryOptionalFields(), this,
                        false, true, Localization.lang("Optional fields"));
                if (optionalPanel.fileListEditor != null) {
                    fileListEditor = optionalPanel.fileListEditor;
                }
                tabbed.addTab(Localization.lang("Optional fields"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(), optionalPanel
                        .getPane(), Localization.lang("Show optional fields"));
                tabs.add(optionalPanel);

                Set<String> deprecatedFields = new HashSet<>(EntryConverter.FIELD_ALIASES_TEX_TO_LTX.keySet());
                deprecatedFields.add("year");
                deprecatedFields.add("month");
                List<String> secondaryOptionalFields = type.getSecondaryOptionalFields();
                List<String> temp = EntryUtil.getRemainder((secondaryOptionalFields), new ArrayList<>(deprecatedFields));
                String[] optionalFieldsNotPrimaryOrDeprecated = temp.toArray(new String[temp.size()]);

                // Get list of all optional fields of this entry and their aliases
                Set<String> optionalFieldsAndAliases = new HashSet<>();
                for (String field : type.getOptionalFields()) {
                    optionalFieldsAndAliases.add(field);
                    if (EntryConverter.FIELD_ALIASES_LTX_TO_TEX.containsKey(field)) {
                        optionalFieldsAndAliases.add(EntryConverter.FIELD_ALIASES_LTX_TO_TEX.get(field));
                    }
                }

                // Get all optional fields which are deprecated
                Set<String> usedOptionalFieldsDeprecated = new HashSet<>(deprecatedFields);
                usedOptionalFieldsDeprecated.retainAll(optionalFieldsAndAliases);

                // Add tabs
                EntryEditorTab optPan2 = new EntryEditorTab(frame, panel,
                        java.util.Arrays.asList(optionalFieldsNotPrimaryOrDeprecated), this,
                        false, true, Localization.lang("Optional fields 2"));
                if (optPan2.fileListEditor != null) {
                    fileListEditor = optPan2.fileListEditor;
                }
                tabbed.addTab(Localization.lang("Optional fields 2"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(), optPan2
                        .getPane(), Localization.lang("Show optional fields"));
                tabs.add(optPan2);

                if (!usedOptionalFieldsDeprecated.isEmpty()) {
                    EntryEditorTab optPan3;
                    optPan3 = new EntryEditorTab(frame, panel,
                            java.util.Arrays.asList(usedOptionalFieldsDeprecated.toArray(new String[usedOptionalFieldsDeprecated.size()])), this,
                            false, true, Localization.lang("Deprecated fields"));
                    if (optPan3.fileListEditor != null) {
                        fileListEditor = optPan3.fileListEditor;
                    }
                    tabbed.addTab(Localization.lang("Deprecated fields"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(), optPan3
                            .getPane(), Localization.lang("Show deprecated bibtex fields"));
                    tabs.add(optPan3);
                }
            }
        }

        // other fields
        List<String> displayedFields = Stream.concat(requiredFields.stream(), displayedOptionalFields.stream()).collect(Collectors.toList());
        List<String> otherFields = this.entry.getFieldNames().stream().filter(f -> !displayedFields.contains(f)).collect(Collectors.toList());
        otherFields.remove("bibtexkey");

        if(!otherFields.isEmpty()) {
            EntryEditorTab otherPanel = new EntryEditorTab(frame, panel, otherFields, this,
                    false, false, Localization.lang("Other fields"));
            if (otherPanel.fileListEditor != null) {
                fileListEditor = otherPanel.fileListEditor;
            }
            tabbed.addTab(Localization.lang("Other fields"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(), otherPanel
                    .getPane(), Localization.lang("Show remaining fields"));
            tabs.add(otherPanel);
        }

        EntryEditorTabList tabList = Globals.prefs.getEntryEditorTabList();
        for (int i = 0; i < tabList.getTabCount(); i++) {
            EntryEditorTab newTab = new EntryEditorTab(frame, panel, tabList.getTabFields(i), this, false,
                    false, tabList.getTabName(i));
            if (newTab.fileListEditor != null) {
                fileListEditor = newTab.fileListEditor;
            }
            tabbed.addTab(tabList.getTabName(i), newTab.getPane());
            tabs.add(newTab);
        }

        srcPanel.setName(Localization.lang("BibTeX source"));
        tabbed.addTab(Localization.lang("BibTeX source"), IconTheme.JabRefIcon.SOURCE.getSmallIcon(), srcPanel,
                Localization.lang("Show/edit BibTeX source"));
        tabs.add(srcPanel);
        sourceIndex = tabs.size() - 1; // Set the sourceIndex variable.
        srcPanel.setFocusCycleRoot(true);
    }

    public String getType() {
        return entry.getType();
    }

    /**
     * @return reference to the currently edited entry
     */
    @Override
    public BibEntry getEntry() {
        return entry;
    }

    public BibDatabase getDatabase() {
        return panel.getDatabase();
    }

    private void setupToolBar() {
        JPanel leftPan = new JPanel();
        leftPan.setLayout(new BorderLayout());
        JToolBar toolBar = new OSXCompatibleToolbar(SwingConstants.VERTICAL);

        toolBar.setBorder(null);
        toolBar.setRollover(true);

        toolBar.setMargin(new Insets(0, 0, 0, 2));

        // The toolbar carries all the key bindings that are valid for the whole
        // window.
        ActionMap actionMap = toolBar.getActionMap();
        InputMap inputMap = toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_ENTRY_EDITOR), "close");
        actionMap.put("close", closeAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_STORE_FIELD), "store");
        actionMap.put("store", storeFieldAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.AUTOGENERATE_BIBTEX_KEYS), "generateKey");
        actionMap.put("generateKey", generateKeyAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.AUTOMATICALLY_LINK_FILES), "autoLink");
        actionMap.put("autoLink", autoLinkAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_ENTRY), "prev");
        actionMap.put("prev", prevEntryAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_ENTRY), "next");
        actionMap.put("next", nextEntryAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.UNDO), "undo");
        actionMap.put("undo", undoAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.REDO), "redo");
        actionMap.put("redo", redoAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.HELP), "help");
        actionMap.put("help", helpAction);

        toolBar.setFloatable(false);

        // Add actions (and thus buttons)
        JButton closeBut = new JButton(closeAction);
        closeBut.setText(null);
        closeBut.setBorder(null);
        closeBut.setMargin(new Insets(8, 0, 8, 0));
        leftPan.add(closeBut, BorderLayout.NORTH);

        // Create type-label
        TypedBibEntry typedEntry = new TypedBibEntry(entry, Optional.empty(), panel.getBibDatabaseContext().getMode());
        leftPan.add(new TypeLabel(typedEntry.getTypeForDisplay()), BorderLayout.CENTER);
        TypeButton typeButton = new TypeButton();

        toolBar.add(typeButton);
        toolBar.add(generateKeyAction);
        toolBar.add(autoLinkAction);

        toolBar.add(writeXmp);

        toolBar.addSeparator();

        toolBar.add(deleteAction);
        toolBar.add(prevEntryAction);
        toolBar.add(nextEntryAction);

        toolBar.addSeparator();

        toolBar.add(helpAction);

        Component[] comps = toolBar.getComponents();

        for (Component comp : comps) {
            ((JComponent) comp).setOpaque(false);
        }

        leftPan.add(toolBar, BorderLayout.SOUTH);
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
     * getExtra checks the field name against InternalBibtexFields.getFieldExtras(name).
     * If the name has an entry, the proper component to be shown is created and
     * returned. Otherwise, null is returned. In addition, e.g. listeners can be
     * added to the field editor, even if no component is returned.
     *
     * @param editor Field editor
     * @return Component to show, or null if none.
     */
    public Optional<JComponent> getExtra(final FieldEditor editor) {
        final String fieldName = editor.getFieldName();

        final String fieldExtras = InternalBibtexFields.getFieldExtras(fieldName);

        // timestamp or a other field with datepicker command
        if (Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD).equals(fieldName)
                || InternalBibtexFields.EXTRA_DATEPICKER.equals(fieldExtras)) {
            // double click AND datefield => insert the current date (today)
            return FieldExtraComponents.getDateTimeExtraComponent(editor,
                    InternalBibtexFields.EXTRA_DATEPICKER.equals(fieldExtras));
        } else if (InternalBibtexFields.EXTRA_EXTERNAL.equals(fieldExtras)) {
            // Add external viewer listener for "pdf" and "url" fields.
            return FieldExtraComponents.getExternalExtraComponent(editor, this);
        } else if (InternalBibtexFields.EXTRA_JOURNAL_NAMES.equals(fieldExtras)) {
            // Add controls for switching between abbreviated and full journal names.
            // If this field also has a FieldContentSelector, we need to combine these.
            return FieldExtraComponents.getJournalExtraComponent(frame, panel, editor, entry, contentSelectors,
                    storeFieldAction);
        } else if (panel.getBibDatabaseContext().getMetaData().getData(Globals.SELECTOR_META_PREFIX + fieldName) != null) {
            return FieldExtraComponents.getSelectorExtraComponent(frame, panel, editor, contentSelectors,
                    storeFieldAction);
        } else if (InternalBibtexFields.EXTRA_BROWSE.equals(fieldExtras)) {
            return FieldExtraComponents.getBrowseExtraComponent(frame, editor, this);
        } else if (InternalBibtexFields.EXTRA_BROWSE_DOC.equals(fieldExtras)
                || InternalBibtexFields.EXTRA_BROWSE_DOC_ZIP.equals(fieldExtras)) {
            return FieldExtraComponents.getBrowseDocExtraComponent(frame, panel, editor, this,
                    InternalBibtexFields.EXTRA_BROWSE_DOC_ZIP.equals(fieldExtras));
        } else if (InternalBibtexFields.EXTRA_URL.equals(fieldExtras)) {
            return FieldExtraComponents.getURLExtraComponent(editor, storeFieldAction);
        } else if (InternalBibtexFields.EXTRA_SET_OWNER.equals(fieldExtras)) {
            return FieldExtraComponents.getSetOwnerExtraComponent(editor, storeFieldAction);
        } else if (InternalBibtexFields.EXTRA_YES_NO.equals(fieldExtras)) {
            return FieldExtraComponents.getYesNoExtraComponent(editor, this);
        } else if (InternalBibtexFields.EXTRA_MONTH.equals(fieldExtras)) {
            return FieldExtraComponents.getMonthExtraComponent(editor, this, this.frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
        }
        return Optional.empty();
    }

    private void setupSourcePanel() {
        source = new JTextAreaWithHighlighting();
        panel.getSearchBar().getSearchQueryHighlightObservable().addSearchListener((SearchQueryHighlightListener) source);

        source.setEditable(true);
        source.setLineWrap(true);
        source.setTabSize(GUIGlobals.INDENT);
        source.addFocusListener(new FieldEditorFocusListener());
        // Add the global focus listener, so a menu item can see if this field
        // was focused when
        // an action was called.
        source.addFocusListener(Globals.focusListener);
        source.setFont(new Font("Monospaced", Font.PLAIN, Globals.prefs.getInt(JabRefPreferences.FONT_SIZE)));
        setupJTextComponent(source);
        updateSource();

        JScrollPane scrollPane = new JScrollPane(source, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        srcPanel.setLayout(new BorderLayout());
        srcPanel.add(scrollPane, BorderLayout.CENTER);

    }

    public void updateSource() {
        if (updateSource) {

            try {
                String srcString = getSourceString(entry, panel.getBibDatabaseContext().getMode());
                source.setText(srcString);
                lastSourceStringAccepted = srcString;

                // Set the current Entry to be selected.
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        final int row = panel.mainTable.findEntry(entry);
                        if (row >= 0) {
                            if (panel.mainTable.getSelectedRowCount() == 0) {
                                panel.mainTable.setRowSelectionInterval(row, row);
                            }
                            //scrollTo(row);
                            panel.mainTable.ensureVisible(row);
                        }
                    }
                });
            } catch (IOException ex) {
                source.setText(ex.getMessage() + "\n\n" +
                        Localization.lang("Correct the entry, and "
                                + "reopen editor to display/edit source."));
                source.setEditable(false);
            }

        }
    }

    public static String getSourceString(BibEntry entry, BibDatabaseMode type) throws IOException {
        StringWriter stringWriter = new StringWriter(200);
        LatexFieldFormatter formatter = LatexFieldFormatter.buildIgnoreHashes();
        new BibEntryWriter(formatter, false).writeWithoutPrependedNewlines(entry, stringWriter, type);

        return stringWriter.getBuffer().toString();
    }

    /**
     * NOTE: This method is only used for the source panel, not for the
     * other tabs. Look at EntryEditorTab for the setup of text components
     * in the other tabs.
     */
    private void setupJTextComponent(JTextComponent textComponent) {
        // Set up key bindings and focus listener for the FieldEditor.
        InputMap inputMap = textComponent.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = textComponent.getActionMap();

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_STORE_FIELD), "store");
        actionMap.put("store", storeFieldAction);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_PANEL), "right");
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_PANEL_2), "right");
        actionMap.put("right", switchRightAction);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_PANEL), "left");
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_PANEL_2), "left");
        actionMap.put("left", switchLeftAction);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.HELP), "help");
        actionMap.put("help", helpAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.SAVE_DATABASE), "save");
        actionMap.put("save", saveDatabaseAction);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.NEXT_TAB), "nexttab");
        actionMap.put("nexttab", frame.nextTab);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.PREVIOUS_TAB), "prevtab");
        actionMap.put("prevtab", frame.prevTab);


        HashSet<AWTKeyStroke> keys = new HashSet<>(textComponent
                .getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.clear();
        keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
        textComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
        keys = new HashSet<>(textComponent
                .getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.clear();
        keys.add(KeyStroke.getKeyStroke("shift pressed TAB"));
        textComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

        textComponent.addFocusListener(new FieldListener());
    }

    @Override
    public void requestFocus() {
        activateVisible();
    }

    private void activateVisible() {
        Object activeTab = tabs.get(tabbed.getSelectedIndex());

        if (activeTab instanceof EntryEditorTab) {
            ((EntryEditorTab) activeTab).activate();
        } else {
            new FocusRequester(source);
        }
    }

    /**
     * Reports the enabled status of the editor, as set by setEnabled()
     */
    @Override
    public boolean isEnabled() {
        return source.isEnabled();
    }

    /**
     * Sets the enabled status of all text fields of the entry editor.
     */
    @Override
    public void setEnabled(boolean enabled) {
        for (Object tab : tabs) {
            if (tab instanceof EntryEditorTab) {
                ((EntryEditorTab) tab).setEnabled(enabled);
            }
        }
        source.setEnabled(enabled);

    }

    /**
     * Centers the given row, and highlights it.
     *
     * @param row an <code>int</code> value
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
            if (comp instanceof FieldEditor) {
                ((FieldEditor) comp).clearAutoCompleteSuggestion();
            }
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

    /**
     * Returns the name of the currently selected component.
     */
    public String getVisiblePanelName() {
        return tabbed.getSelectedComponent().getName();
    }

    /**
     * Sets the panel with the given index visible.
     *
     * @param i an <code>int</code> value
     */
    public void setVisiblePanel(int i) {
        tabbed.setSelectedIndex(Math.min(i, tabbed.getTabCount() - 1));
    }

    public void setVisiblePanel(String name) {
        for (int i = 0; i < tabbed.getTabCount(); ++i) {
            if ((tabbed.getComponent(i).getName() != null) && tabbed.getComponent(i).getName().equals(name)) {
                tabbed.setSelectedIndex(i);
                return;
            }
        }
        if (tabbed.getTabCount() > 0) {
            tabbed.setSelectedIndex(0);
        }
    }

    public void setFocusToField(String fieldName) {
        for (Object tab : tabs) {
            if (tab instanceof EntryEditorTab) {
                if (((EntryEditorTab) tab).getFields().contains(fieldName)) {
                    EntryEditorTab entryEditorTab = (EntryEditorTab) tab;
                    setVisiblePanel(entryEditorTab.getTabTitle());
                    entryEditorTab.setActive(fieldName);
                    entryEditorTab.activate();
                }
            }
        }

    }

    /**
     * Updates this editor to show the given entry, regardless of type
     * correspondence.
     *
     * @param switchEntry a <code>BibEntry</code> value
     */
    public synchronized void switchTo(BibEntry switchEntry) {
        storeCurrentEdit();

        // Remove this instance as property listener for the entry:
        this.entry.removePropertyChangeListener(this);

        // Register as property listener for the new entry:
        switchEntry.addPropertyChangeListener(this);

        this.entry = switchEntry;

        updateAllFields();
        validateAllFields();
        updateSource();
        panel.newEntryShowing(switchEntry);

    }

    private boolean storeSource() {
        // Store edited bibtex code.
        BibtexParser bibtexParser = new BibtexParser(new java.io.StringReader(source.getText()));

        try {
            ParserResult parserResult = bibtexParser.parse();
            BibDatabase database = parserResult.getDatabase();

            if (database.getEntryCount() > 1) {
                throw new IllegalStateException("More than one entry found.");
            }

            if (database.getEntryCount() < 1) {
                if (parserResult.hasWarnings()) {
                    // put the warning into as exception text -> it will be displayed to the user
                    throw new IllegalStateException(parserResult.warnings().get(0));
                } else {
                    throw new IllegalStateException("No entries found.");
                }
            }

            NamedCompound compound = new NamedCompound(Localization.lang("source edit"));
            BibEntry newEntry = database.getEntryById(database.getKeySet().iterator().next());
            String id = entry.getId();
            String newKey = newEntry.getCiteKey();
            boolean anyChanged = false;
            boolean changedType = false;
            boolean duplicateWarning = false;
            boolean emptyWarning = (newKey == null) || newKey.isEmpty();

            if (panel.getDatabase().setCiteKeyForEntry(id, newKey)) {
                duplicateWarning = true;

                // First, remove fields that the user have removed.
            }

            for (String field : entry.getFieldNames()) {
                if (InternalBibtexFields.isDisplayableField(field) && !newEntry.hasField(field)) {
                    compound.addEdit(new UndoableFieldChange(entry, field, entry.getField(field), null));
                    entry.clearField(field);
                    anyChanged = true;
                }
            }

            // Then set all fields that have been set by the user.
            for (String field : newEntry.getFieldNames()) {
                String oldValue = entry.getField(field);
                String newValue = newEntry.getField(field);
                if ((oldValue == null) || !oldValue.equals(newValue)) {
                    // Test if the field is legally set.
                    new LatexFieldFormatter().format(newValue, field);

                    compound.addEdit(new UndoableFieldChange(entry, field, oldValue, newValue));
                    entry.setField(field, newValue);
                    anyChanged = true;
                }
            }

            // See if the user has changed the entry type:
            if (newEntry.getType() != entry.getType()) {
                compound.addEdit(new UndoableChangeType(entry,
                        entry.getType(), newEntry.getType()));
                entry.setType(newEntry.getType());
                anyChanged = true;
                changedType = true;
            }
            compound.end();

            if (!anyChanged) {
                return true;
            }

            panel.undoManager.addEdit(compound);

            if (duplicateWarning) {
                warnDuplicateBibtexkey();
            } else if (emptyWarning) {
                warnEmptyBibtexkey();
            } else {
                panel.output(Localization.lang("Stored entry") + '.');
            }

            lastSourceStringAccepted = source.getText();
            if (changedType) {
                panel.updateEntryEditorIfShowing();
            } else {
                updateAllFields();
                lastSourceAccepted = true;
                updateSource = true;
            }
            // TODO: does updating work properly after source stored?
            panel.markBaseChanged();
            ///////////////////////////////////////////////////////
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    final int row = panel.mainTable.findEntry(entry);
                    if (row >= 0) {
                        panel.mainTable.ensureVisible(row);
                    }
                }
            });

            return true;
        } catch (IllegalStateException | IOException ex) {
            // The source couldn't be parsed, so the user is given an
            // error message, and the choice to keep or revert the contents
            // of the source text field.
            updateSource = false;
            lastSourceAccepted = false;
            tabbed.setSelectedComponent(srcPanel);

            Object[] options = {Localization.lang("Edit"), Localization.lang("Revert to original source")};

            int answer = JOptionPane.showOptionDialog(frame, Localization.lang("Error") + ": " + ex.getMessage(),
                    Localization.lang("Problem with parsing entry"), JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE, null, options, options[0]);

            if (answer != 0) {
                updateSource = true;
                updateSource();
            }

            return false;
        }
    }

    private void setField(String fieldName, String newFieldData) {

        for (Object tab : tabs) {
            if (tab instanceof EntryEditorTab) {
                ((EntryEditorTab) tab).updateField(fieldName, newFieldData);
            }
        }

    }

    /**
     * Sets all the text areas according to the shown entry.
     */
    public void updateAllFields() {
        for (Object tab : tabs) {
            if (tab instanceof EntryEditorTab) {
                ((EntryEditorTab) tab).setEntry(entry);
            }
        }
    }

    /**
     * Removes the "invalid field" color from all text areas.
     */
    public void validateAllFields() {
        for (Object tab : tabs) {
            if (tab instanceof EntryEditorTab) {
                ((EntryEditorTab) tab).validateAllFields();
            }
        }
    }

    public void updateAllContentSelectors() {
        if (!contentSelectors.isEmpty()) {
            for (FieldContentSelector contentSelector : contentSelectors) {
                contentSelector.rebuildComboBox();
            }
        }
    }

    /**
     * Update the JTextArea when a field has changed.
     *
     * @see java.beans.VetoableChangeListener#vetoableChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void vetoableChange(PropertyChangeEvent e) {
        String newValue = e.getNewValue() == null ? "" : e.getNewValue().toString();
        setField(e.getPropertyName(), newValue);
    }

    public void updateField(final Object sourceObject) {
        storeFieldAction.actionPerformed(new ActionEvent(sourceObject, 0, ""));
    }

    public void setMovingToDifferentEntry() {
        movingToDifferentEntry = true;
    }


    private class TypeButton extends JButton {
        public TypeButton() {
            super(IconTheme.JabRefIcon.EDIT.getIcon());
            setToolTipText(Localization.lang("Change entry type"));
            addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    showChangeEntryTypePopupMenu();
                }
            });
        }
    }

    private void showChangeEntryTypePopupMenu() {
        JPopupMenu typeMenu = new ChangeEntryTypeMenu().getChangeentryTypePopupMenu(panel);
        typeMenu.show(ths, 0, 0);
    }

    private class TypeLabel extends JLabel {
        public TypeLabel(String type) {
            super(type);
            setUI(new VerticalLabelUI(false));
            setForeground(GUIGlobals.entryEditorLabelColor);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(GUIGlobals.typeNameFont);

            // Add a mouse listener so the user can right-click the type label to change the entry type:
            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() || (e.getButton() == MouseEvent.BUTTON3)) {
                        handleTypeChange();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isPopupTrigger() || (e.getButton() == MouseEvent.BUTTON3)) {
                        handleTypeChange();
                    }
                }

                private void handleTypeChange() {
                    showChangeEntryTypePopupMenu();
                }
            });
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paintComponent(g2);
        }
    }

    private class FieldListener extends FocusAdapter {

        /*
         * Focus listener that fires the storeFieldAction when a TextArea
         * loses focus.
         */
        @Override
        public void focusGained(FocusEvent e) {
            // Do nothing
        }

        @Override
        public void focusLost(FocusEvent event) {
            if (!event.isTemporary()) {
                updateField(event.getSource());
            }
        }
    }

    private class TabListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent event) {
            // We tell the editor tab to update all its fields.
            //  This makes sure they are updated even if the tab we
            // just left contained one
            // or more of the same fields as this one:
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    Object activeTab = tabs.get(tabbed.getSelectedIndex());
                    if (activeTab instanceof EntryEditorTab) {
                        ((EntryEditorTab) activeTab).updateAll();
                    }
                }
            });

        }
    }

    class DeleteAction extends AbstractAction {
        public DeleteAction() {
            super(Localization.lang("Delete"), IconTheme.JabRefIcon.DELETE_ENTRY.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Delete entry"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Show confirmation dialog if not disabled:
            boolean goOn = panel.showDeleteConfirmationDialog(1);

            if (!goOn) {
                return;
            }

            panel.entryEditorClosing(EntryEditor.this);
            panel.getDatabase().removeEntry(entry);
            panel.markBaseChanged();
            panel.undoManager.addEdit(new UndoableRemoveEntry(panel.getDatabase(), entry, panel));
            panel.output(Localization.lang("Deleted entry"));
        }
    }

    class CloseAction extends AbstractAction {
        public CloseAction() {
            super(Localization.lang("Close window"), IconTheme.JabRefIcon.CLOSE.getSmallIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close window"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (tabbed.getSelectedComponent() == srcPanel) {
                updateField(source);
                if (lastSourceAccepted) {
                    panel.entryEditorClosing(EntryEditor.this);
                }
            } else {
                panel.entryEditorClosing(EntryEditor.this);
            }
        }
    }

    public class StoreFieldAction extends AbstractAction {
        public StoreFieldAction() {
            super("Store field value");
            putValue(Action.SHORT_DESCRIPTION, "Store field value");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            boolean movingAway = movingToDifferentEntry;
            movingToDifferentEntry = false;

            if (event.getSource() instanceof TextField) {
                // Storage from bibtex key field.
                TextField textField = (TextField) event.getSource();
                String oldValue = entry.getCiteKey();
                String newValue = textField.getText();

                if (newValue.isEmpty()) {
                    newValue = null;
                }

                if (((oldValue == null) && (newValue == null))
                        || ((oldValue != null) && (newValue != null) && oldValue.equals(newValue))) {
                    return; // No change.
                }

                // Make sure the key is legal:
                String cleaned = net.sf.jabref.util.Util.checkLegalKey(newValue);
                if ((cleaned != null) && !cleaned.equals(newValue)) {
                    JOptionPane.showMessageDialog(frame, Localization.lang("Invalid BibTeX key"),
                            Localization.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                    textField.setInvalidBackgroundColor();
                    return;
                } else {
                    textField.setValidBackgroundColor();
                }

                boolean isDuplicate = panel.getDatabase().setCiteKeyForEntry(entry.getId(), newValue);

                if (newValue == null) {
                    warnEmptyBibtexkey();
                } else {
                    if (isDuplicate) {
                        warnDuplicateBibtexkey();
                    } else {
                        panel.output(Localization.lang("BibTeX key is unique."));
                    }
                }

                // Add an UndoableKeyChange to the baseframe's undoManager.
                UndoableKeyChange undoableKeyChange = new UndoableKeyChange(panel.getDatabase(), entry.getId(), oldValue, newValue);
                if (net.sf.jabref.util.Util.updateTimeStampIsSet()) {
                    NamedCompound ce = net.sf.jabref.util.Util.doUpdateTimeStamp(entry, undoableKeyChange);
                    panel.undoManager.addEdit(ce);
                } else {
                    panel.undoManager.addEdit(undoableKeyChange);
                }

                textField.setValidBackgroundColor();

                if (textField.getTextComponent().hasFocus()) {
                    textField.setActiveBackgroundColor();
                }
                updateSource();
                panel.markBaseChanged();
            } else if (event.getSource() instanceof FieldEditor) {
                String toSet = null;
                FieldEditor fieldEditor = (FieldEditor) event.getSource();
                boolean set;
                // Trim the whitespace off this value
                String currentText = fieldEditor.getText();
                String trim = currentText.trim();
                if (!trim.isEmpty()) {
                    toSet = trim;
                }

                // We check if the field has changed, since we don't want to
                // mark the base as changed unless we have a real change.
                if (toSet == null) {
                    set = entry.hasField(fieldEditor.getFieldName());
                } else {
                    set = !((entry.hasField(fieldEditor.getFieldName()))
                            && toSet.equals(entry.getField(fieldEditor.getFieldName())));
                }

                if (set) {
                    try {
                        // The following statement attempts to write the
                        // new contents into a StringWriter, and this will
                        // cause an IOException if the field is not
                        // properly formatted. If that happens, the field
                        // is not stored and the textarea turns red.
                        if (toSet != null) {
                            new LatexFieldFormatter().format(toSet, fieldEditor.getFieldName());
                        }

                        String oldValue = entry.getField(fieldEditor.getFieldName());

                        if (toSet == null) {
                            entry.clearField(fieldEditor.getFieldName());
                        } else {
                            entry.setField(fieldEditor.getFieldName(), toSet);
                        }

                        fieldEditor.setValidBackgroundColor();

                        // See if we need to update an AutoCompleter instance:
                        AutoCompleter<String> aComp = panel.getAutoCompleters().get(fieldEditor.getFieldName());
                        if (aComp != null) {
                            aComp.addBibtexEntry(entry);
                        }

                        // Add an UndoableFieldChange to the baseframe's undoManager.
                        UndoableFieldChange undoableFieldChange = new UndoableFieldChange(entry, fieldEditor.getFieldName(), oldValue, toSet);
                        if (net.sf.jabref.util.Util.updateTimeStampIsSet()) {
                            NamedCompound ce = net.sf.jabref.util.Util.doUpdateTimeStamp(entry, undoableFieldChange);
                            panel.undoManager.addEdit(ce);
                        } else {
                            panel.undoManager.addEdit(undoableFieldChange);
                        }
                        updateSource();
                        panel.markBaseChanged();
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("Error") + ": " + ex.getMessage(),
                                Localization.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                        fieldEditor.setInvalidBackgroundColor();
                    }
                } else {
                    // set == false
                    // We set the field and label color.
                    fieldEditor.setValidBackgroundColor();
                }
                if (fieldEditor.getTextComponent().hasFocus()) {
                    fieldEditor.setBackground(GUIGlobals.activeEditor);
                }
            } else if (source.isEditable()
                    && !source.getText().equals(lastSourceStringAccepted)) {
                storeSource();
            }

            // Make sure we scroll to the entry if it moved in the table.
            // Should only be done if this editor is currently showing:
            if (!movingAway && isShowing()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        final int row = panel.mainTable.findEntry(entry);
                        if (row >= 0) {
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

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = tabbed.getSelectedIndex();
            tabbed.setSelectedIndex(i > 0 ? i - 1 : tabbed.getTabCount() - 1);

            activateVisible();
        }
    }

    class SwitchRightAction extends AbstractAction {
        public SwitchRightAction() {
            super("Switch to the panel to the right");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = tabbed.getSelectedIndex();
            tabbed.setSelectedIndex(i < (tabbed.getTabCount() - 1) ? i + 1 : 0);
            activateVisible();

        }
    }

    class NextEntryAction extends AbstractAction {
        public NextEntryAction() {
            super(Localization.lang("Next entry"), IconTheme.JabRefIcon.DOWN.getIcon());

            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Next entry"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            int thisRow = panel.mainTable.findEntry(entry);
            int newRow;

            if ((thisRow + 1) < panel.getDatabase().getEntryCount()) {
                newRow = thisRow + 1;
            } else if (thisRow > 0) {
                newRow = 0;
            } else {
                return; // newRow is still -1, so we can assume the database has
                // only one entry.
            }

            scrollTo(newRow);
            panel.mainTable.setRowSelectionInterval(newRow, newRow);

        }
    }

    class PrevEntryAction extends AbstractAction {
        public PrevEntryAction() {
            super(Localization.lang("Previous entry"), IconTheme.JabRefIcon.UP.getIcon());

            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Previous entry"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int thisRow = panel.mainTable.findEntry(entry);
            int newRow;

            if ((thisRow - 1) >= 0) {
                newRow = thisRow - 1;
            } else if (thisRow != (panel.getDatabase().getEntryCount() - 1)) {
                newRow = panel.getDatabase().getEntryCount() - 1;
            } else {
                return; // newRow is still -1, so we can assume the database has
                // only one entry.

            }

            scrollTo(newRow);
            panel.mainTable.setRowSelectionInterval(newRow, newRow);

        }
    }

    class GenerateKeyAction extends AbstractAction {

        public GenerateKeyAction() {
            super(Localization.lang("Generate BibTeX key"), IconTheme.JabRefIcon.MAKE_KEY.getIcon());

            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Generate BibTeX key"));

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // 1. get BibEntry for selected index (already have)
            // 2. update label

            // Store the current edit in case this action is called during the
            // editing of a field:
            storeCurrentEdit();

            // This is a partial clone of net.sf.jabref.gui.BasePanel.setupActions().new AbstractWorker() {...}.run()

            // this updates the table automatically, on close, but not
            // within the tab
            Object oldValue = entry.getCiteKey();

            if (oldValue != null) {
                if (Globals.prefs.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY)) {
                    panel.output(Localization.lang("Not overwriting existing key. To change this setting, open Options -> Prefererences -> BibTeX key generator"));
                    return;
                } else if (Globals.prefs.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY)) {
                    CheckBoxMessage cbm = new CheckBoxMessage(Localization.lang("The current BibTeX key will be overwritten. Continue?"),
                            Localization.lang("Disable this confirmation dialog"), false);
                    int answer = JOptionPane.showConfirmDialog(frame, cbm, Localization.lang("Overwrite key"),
                            JOptionPane.YES_NO_OPTION);
                    if (cbm.isSelected()) {
                        Globals.prefs.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, false);
                    }
                    if (answer == JOptionPane.NO_OPTION) {
                        // Ok, break off the operation.
                        return;
                    }
                }
            }

            LabelPatternUtil.makeLabel(panel.getBibDatabaseContext().getMetaData(), panel.getDatabase(), entry);

            // Store undo information:
            panel.undoManager.addEdit(new UndoableKeyChange(panel.getDatabase(), entry.getId(), (String) oldValue, entry.getCiteKey()));

            // here we update the field
            String bibtexKeyData = entry.getCiteKey();

            // set the field named for "bibtexkey"
            setField(BibEntry.KEY_FIELD, bibtexKeyData);
            updateSource();
            panel.markBaseChanged();

        }
    }

    class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo", IconTheme.JabRefIcon.UNDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, "Undo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.runCommand(Actions.UNDO);
        }
    }

    class RedoAction extends AbstractAction {
        public RedoAction() {
            super("Redo", IconTheme.JabRefIcon.REDO.getIcon());
            putValue(Action.SHORT_DESCRIPTION, "Redo");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.runCommand(Actions.REDO);
        }
    }

    class SaveDatabaseAction extends AbstractAction {
        public SaveDatabaseAction() {
            super("Save database");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object activeTab = tabs.get(tabbed.getSelectedIndex());
            if (activeTab instanceof EntryEditorTab) {
                // Normal panel.
                EntryEditorTab fp = (EntryEditorTab) activeTab;
                FieldEditor fe = fp.getActive();
                fe.clearAutoCompleteSuggestion();
                updateField(fe);
            } else {
                // Source panel.
                updateField(activeTab);
            }


            panel.runCommand(Actions.SAVE);

        }
    }

    class ExternalViewerListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                TextArea tf = (TextArea) evt.getSource();

                if (tf.getText().isEmpty()) {
                    return;
                }

                tf.selectAll();

                String link = tf.getText(); // get selected ? String

                try {
                    JabRefDesktop.openExternalViewer(panel.getBibDatabaseContext().getMetaData(), link, tf.getFieldName());
                } catch (IOException ex) {
                    LOGGER.warn("Error opening file.", ex);
                }
            }
        }
    }

    class ChangeTypeAction extends AbstractAction {

        private final String changeType;

        private final BasePanel changeTypePanel;


        public ChangeTypeAction(EntryType type, BasePanel bp) {
            super(type.getName());
            this.changeType = type.getName();
            changeTypePanel = bp;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            changeTypePanel.changeType(entry, changeType);
        }
    }


    private void warnDuplicateBibtexkey() {
        panel.output(Localization.lang("Duplicate BibTeX key.")+" "+Localization.lang("Grouping may not work for this entry."));
    }

    private void warnEmptyBibtexkey() {
        panel.output(Localization.lang("Empty BibTeX key")+". "+Localization.lang("Grouping may not work for this entry."));
    }


    private class AutoLinkAction extends AbstractAction {
        public AutoLinkAction() {
            putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.AUTO_FILE_LINK.getIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Automatically set file links for this entry") + " (Alt-F)");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            FileListEditor localFileListEditor = EntryEditor.this.fileListEditor;
            if (localFileListEditor == null) {
                LOGGER.warn("No file list editor found.");
            } else {
                localFileListEditor.autoSetLinks();
            }
        }
    }

}
