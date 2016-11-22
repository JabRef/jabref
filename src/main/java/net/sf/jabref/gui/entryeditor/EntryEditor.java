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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.EntryContainer;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.OSXCompatibleToolbar;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.externalfiles.WriteXMPEntryEditorAction;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditorFocusListener;
import net.sf.jabref.gui.fieldeditors.FileListEditor;
import net.sf.jabref.gui.fieldeditors.JTextAreaWithHighlighting;
import net.sf.jabref.gui.fieldeditors.TextField;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.menus.ChangeEntryTypeMenu;
import net.sf.jabref.gui.specialfields.SpecialFieldUpdateListener;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableChangeType;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.undo.UndoableKeyChange;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.component.CheckBoxMessage;
import net.sf.jabref.gui.util.component.VerticalLabelUI;
import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.logic.bibtex.BibEntryWriter;
import net.sf.jabref.logic.bibtex.LatexFieldFormatter;
import net.sf.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQueryHighlightListener;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.logic.util.date.EasyDateFormat;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryConverter;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FieldProperty;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.model.entry.event.FieldChangedEvent;
import net.sf.jabref.preferences.JabRefPreferences;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * GUI component that allows editing of the fields of a BibEntry (i.e. the
 * one that shows up, when you double click on an entry in the table)
 * <p>
 * It hosts the tabs (required, general, optional) and the buttons to the left.
 * <p>
 * EntryEditor also registers itself to the event bus, receiving
 * events whenever a field of the entry changes, enabling the text fields to
 * update themselves if the change is made from somewhere else.
 */
public class EntryEditor extends JPanel implements EntryContainer {
    private static final Log LOGGER = LogFactory.getLog(EntryEditor.class);

    /** A reference to the entry this object works on. */
    private BibEntry entry;
    /** The currently displayed type */
    private final String displayedBibEntryType;

    /** The action concerned with closing the window. */
    private final CloseAction closeAction = new CloseAction();
    /** The action that deletes the current entry, and closes the editor. */
    private final DeleteAction deleteAction = new DeleteAction();

    /** The action for switching to the next entry. */
    private final AbstractAction nextEntryAction = new NextEntryAction();
    /** The action for switching to the previous entry. */
    private final AbstractAction prevEntryAction = new PrevEntryAction();

    /** The action concerned with storing a field value. */
    private final StoreFieldAction storeFieldAction = new StoreFieldAction();

    /** The action for switching to the next tab */
    private final SwitchLeftAction switchLeftAction = new SwitchLeftAction();
    /** The action for switching to the previous tab */
    private final SwitchRightAction switchRightAction = new SwitchRightAction();

    /** The action which generates a BibTeX key for this entry. */
    private final GenerateKeyAction generateKeyAction = new GenerateKeyAction();

    // UGLY HACK to have a pointer to the fileListEditor to call autoSetLinks()
    private FileListEditor fileListEditor;
    private final AutoLinkAction autoLinkAction = new AutoLinkAction();

    private final AbstractAction writeXmp;

    private final SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction();

    private final JPanel srcPanel = new JPanel();

    private JTextArea source;

    private final JTabbedPane tabbed = new JTabbedPane();

    private final JabRefFrame frame;

    private final BasePanel panel;

    /**
     * This can be set to false to stop the source text area from getting updated. This is used in cases where the
     * source couldn't be parsed, and the user is given the option to edit it.
     */
    private boolean updateSource = true;
    /** Indicates that we are about to go to the next or previous entry */
    private boolean movingToDifferentEntry;
    private boolean validEntry = true;

    private final List<Object> tabs = new ArrayList<>();

    private boolean lastFieldAccepted = true;

    /**
     *  This indicates whether the last attempt at parsing the source was successful. It is used to determine whether
     *  the dialog should close; it should stay open if the user received an error message about the source,
     *  whatever he or she chose to do about it.
     */
    private boolean lastSourceAccepted = true;

    /** This is used to prevent double updates after editing source. */
    private String lastSourceStringAccepted;

    /** The index the source panel has in tabbed. */
    private int sourceIndex = -1;

    private final HelpAction helpAction = new HelpAction(HelpFile.ENTRY_EDITOR, IconTheme.JabRefIcon.HELP.getIcon());

    private final UndoAction undoAction = new UndoAction();

    private final RedoAction redoAction = new RedoAction();

    private final TabListener tabListener = new TabListener();

    private final List<SearchQueryHighlightListener> searchListeners = new ArrayList<>();


    public EntryEditor(JabRefFrame frame, BasePanel panel, BibEntry entry) {
        this.frame = frame;
        this.panel = panel;
        this.entry = entry;

        entry.registerListener(this);
        entry.registerListener(SpecialFieldUpdateListener.getInstance());

        displayedBibEntryType = entry.getType();

        writeXmp = new WriteXMPEntryEditorAction(panel, this);

        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);
        setupToolBar();
        setupFieldPanels();
        setupSourcePanel();
        add(tabbed, BorderLayout.CENTER);
        tabbed.addChangeListener(tabListener);
        if (Globals.prefs.getBoolean(JabRefPreferences.DEFAULT_SHOW_SOURCE)) {
            tabbed.setSelectedIndex(sourceIndex);
        }

        updateAllFields();
        if (this.fileListEditor != null){
            this.fileListEditor.adjustColumnWidth();
        }
    }

    private void setupFieldPanels() {
        tabbed.removeAll();
        tabs.clear();

        EntryType type = EntryTypes.getTypeOrDefault(entry.getType(),
                this.frame.getCurrentBasePanel().getBibDatabaseContext().getMode());

        // required fields
        addRequiredTab(type);

        // optional fields
        Set<String> deprecatedFields = new HashSet<>(EntryConverter.FIELD_ALIASES_TEX_TO_LTX.keySet());
        Set<String> usedOptionalFieldsDeprecated = new HashSet<>(deprecatedFields);

        if ((type.getOptionalFields() != null) && !type.getOptionalFields().isEmpty()) {
            if (!frame.getCurrentBasePanel().getBibDatabaseContext().isBiblatexMode()) {
                addOptionalTab(type);
            } else {
                addOptionalTab(type);

                deprecatedFields.add(FieldName.YEAR);
                deprecatedFields.add(FieldName.MONTH);
                List<String> secondaryOptionalFields = type.getSecondaryOptionalFields();
                List<String> optionalFieldsNotPrimaryOrDeprecated = new ArrayList<>(secondaryOptionalFields);
                optionalFieldsNotPrimaryOrDeprecated.removeAll(deprecatedFields);

                // Get list of all optional fields of this entry and their aliases
                Set<String> optionalFieldsAndAliases = new HashSet<>();
                for (String field : type.getOptionalFields()) {
                    optionalFieldsAndAliases.add(field);
                    if (EntryConverter.FIELD_ALIASES_LTX_TO_TEX.containsKey(field)) {
                        optionalFieldsAndAliases.add(EntryConverter.FIELD_ALIASES_LTX_TO_TEX.get(field));
                    }
                }

                // Get all optional fields which are deprecated
                usedOptionalFieldsDeprecated.retainAll(optionalFieldsAndAliases);

                // Get other deprecated fields
                usedOptionalFieldsDeprecated.add(FieldName.MONTH);

                // Add tabs
                EntryEditorTab optPan2 = new EntryEditorTab(frame, panel, optionalFieldsNotPrimaryOrDeprecated, this,
                        false, true, Localization.lang("Optional fields 2"));
                if (optPan2.fileListEditor != null) {
                    fileListEditor = optPan2.fileListEditor;
                }
                tabbed.addTab(Localization.lang("Optional fields 2"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(),
                        optPan2.getPane(), Localization.lang("Show optional fields"));
                tabs.add(optPan2);

                if (!usedOptionalFieldsDeprecated.isEmpty()) {
                    EntryEditorTab optPan3;
                    optPan3 = new EntryEditorTab(frame, panel, new ArrayList<>(usedOptionalFieldsDeprecated), this,
                            false, true, Localization.lang("Deprecated fields"));
                    if (optPan3.fileListEditor != null) {
                        fileListEditor = optPan3.fileListEditor;
                    }
                    tabbed.addTab(Localization.lang("Deprecated fields"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(),
                            optPan3.getPane(), Localization.lang("Show deprecated BibTeX fields"));
                    tabs.add(optPan3);
                }
            }
        }

        // other fields
        List<String> displayedFields = type.getAllFields().stream().map(String::toLowerCase)
                .collect(Collectors.toList());
        List<String> otherFields = entry.getFieldNames().stream().map(String::toLowerCase)
                .filter(f -> !displayedFields.contains(f)).collect(Collectors.toList());
        if (!usedOptionalFieldsDeprecated.isEmpty()) {
            otherFields.removeAll(usedOptionalFieldsDeprecated);
        }
        otherFields.remove(BibEntry.KEY_FIELD);
        otherFields.removeAll(Globals.prefs.getCustomTabFieldNames());

        if (!otherFields.isEmpty()) {
            addOtherTab(otherFields);
        }

        // general fields from preferences
        addGeneralTabs();
        // source tab
        addSourceTab();
    }

    private void addGeneralTabs() {
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
    }

    private void addSourceTab() {
        String panelName = Localization.lang("%0 source", panel.getBibDatabaseContext().getMode().getFormattedName());
        String toolTip = Localization.lang("Show/edit %0 source", panel.getBibDatabaseContext().getMode().getFormattedName());
        srcPanel.setName(panelName);
        tabbed.addTab(panelName, IconTheme.JabRefIcon.SOURCE.getSmallIcon(), srcPanel, toolTip);
        tabs.add(srcPanel);
        sourceIndex = tabs.size() - 1;
        srcPanel.setFocusCycleRoot(true);
    }

    private void addOtherTab(List<String> otherFields) {
        EntryEditorTab otherPanel = new EntryEditorTab(frame, panel, otherFields, this,
                false, false, Localization.lang("Other fields"));
        if (otherPanel.fileListEditor != null) {
            fileListEditor = otherPanel.fileListEditor;
        }
        tabbed.addTab(Localization.lang("Other fields"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(), otherPanel
                .getPane(), Localization.lang("Show remaining fields"));
        tabs.add(otherPanel);
    }

    private List<String> addRequiredTab(EntryType type) {
        List<String> requiredFields = type.getRequiredFieldsFlat();

        EntryEditorTab requiredPanel = new EntryEditorTab(frame, panel, requiredFields, this, true, false, Localization.lang("Required fields"));
        if (requiredPanel.fileListEditor != null) {
            fileListEditor = requiredPanel.fileListEditor;
        }
        tabbed.addTab(Localization.lang("Required fields"), IconTheme.JabRefIcon.REQUIRED.getSmallIcon(), requiredPanel
                .getPane(), Localization.lang("Show required fields"));
        tabs.add(requiredPanel);
        return requiredFields;
    }

    private void addOptionalTab(EntryType type) {
        EntryEditorTab optionalPanel = new EntryEditorTab(frame, panel, type.getPrimaryOptionalFields(), this,
                false, true, Localization.lang("Optional fields"));

        if (optionalPanel.fileListEditor != null) {
            fileListEditor = optionalPanel.fileListEditor;
        }
        tabbed.addTab(Localization.lang("Optional fields"), IconTheme.JabRefIcon.OPTIONAL.getSmallIcon(), optionalPanel
                .getPane(), Localization.lang("Show optional fields"));
        tabs.add(optionalPanel);
    }

    public String getDisplayedBibEntryType() {
        return displayedBibEntryType;
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

        // The toolbar carries all the key bindings that are valid for the whole window.
        ActionMap actionMap = toolBar.getActionMap();
        InputMap inputMap = toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_ENTRY_EDITOR), "close");
        actionMap.put("close", closeAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_STORE_FIELD), "store");
        actionMap.put("store", getStoreFieldAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.AUTOGENERATE_BIBTEX_KEYS), "generateKey");
        actionMap.put("generateKey", getGenerateKeyAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.AUTOMATICALLY_LINK_FILES), "autoLink");
        actionMap.put("autoLink", autoLinkAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_ENTRY), "prev");
        actionMap.put("prev", getPrevEntryAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_ENTRY), "next");
        actionMap.put("next", getNextEntryAction());
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.UNDO), "undo");
        actionMap.put("undo", undoAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.REDO), "redo");
        actionMap.put("redo", redoAction);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.HELP), "help");
        actionMap.put("help", getHelpAction());

        toolBar.setFloatable(false);

        // Add actions (and thus buttons)
        JButton closeBut = new JButton(closeAction);
        closeBut.setText(null);
        closeBut.setBorder(null);
        closeBut.setMargin(new Insets(8, 0, 8, 0));
        leftPan.add(closeBut, BorderLayout.NORTH);

        // Create type-label
        TypedBibEntry typedEntry = new TypedBibEntry(entry, panel.getBibDatabaseContext().getMode());
        leftPan.add(new TypeLabel(typedEntry.getTypeForDisplay()), BorderLayout.CENTER);
        TypeButton typeButton = new TypeButton();

        toolBar.add(typeButton);
        toolBar.add(getGenerateKeyAction());
        toolBar.add(autoLinkAction);

        toolBar.add(writeXmp);

        toolBar.addSeparator();

        toolBar.add(deleteAction);
        toolBar.add(getPrevEntryAction());
        toolBar.add(getNextEntryAction());

        toolBar.addSeparator();

        toolBar.add(getHelpAction());

        Component[] comps = toolBar.getComponents();

        for (Component comp : comps) {
            ((JComponent) comp).setOpaque(false);
        }

        leftPan.add(toolBar, BorderLayout.SOUTH);
        add(leftPan, BorderLayout.WEST);
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

        final Set<FieldProperty> fieldExtras = InternalBibtexFields.getFieldProperties(fieldName);

        // timestamp or a other field with datepicker command
        if (Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD).equals(fieldName)
                || fieldExtras.contains(FieldProperty.DATE)) {
            // double click AND datefield => insert the current date (today)
            return FieldExtraComponents.getDateTimeExtraComponent(editor,
                    fieldExtras.contains(FieldProperty.DATE), fieldExtras.contains(FieldProperty.ISO_DATE));
        } else if (fieldExtras.contains(FieldProperty.EXTERNAL)) {
            return FieldExtraComponents.getExternalExtraComponent(panel, editor);
        } else if (fieldExtras.contains(FieldProperty.JOURNAL_NAME)) {
            // Add controls for switching between abbreviated and full journal names.
            // If this field also has a FieldContentSelector, we need to combine these.
            return FieldExtraComponents.getJournalExtraComponent(panel, editor, entry, getStoreFieldAction());
        } else if (fieldExtras.contains(FieldProperty.DOI)) {
            return FieldExtraComponents.getDoiExtraComponent(panel, this, editor);
        } else if (fieldExtras.contains(FieldProperty.EPRINT)) {
            return FieldExtraComponents.getEprintExtraComponent(panel, this, editor);
        } else if (fieldExtras.contains(FieldProperty.ISBN)) {
            return FieldExtraComponents.getIsbnExtraComponent(panel, this, editor);
        } else if (fieldExtras.contains(FieldProperty.OWNER)) {
            return FieldExtraComponents.getSetOwnerExtraComponent(editor, getStoreFieldAction());
        } else if (fieldExtras.contains(FieldProperty.YES_NO)) {
            return FieldExtraComponents.getYesNoExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.MONTH)) {
            return FieldExtraComponents.getMonthExtraComponent(editor, this, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
        } else if (fieldExtras.contains(FieldProperty.GENDER)) {
            return FieldExtraComponents.getGenderExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.EDITOR_TYPE)) {
            return FieldExtraComponents.getEditorTypeExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.PAGINATION)) {
            return FieldExtraComponents.getPaginationExtraComponent(editor, this);
        } else if (fieldExtras.contains(FieldProperty.TYPE)) {
            return FieldExtraComponents.getTypeExtraComponent(editor, this, "patent".equalsIgnoreCase(entry.getType()));
        }
        return Optional.empty();
    }

    private void setupSourcePanel() {
        source = new JTextAreaWithHighlighting();
        addSearchListener((SearchQueryHighlightListener) source);

        source.setEditable(true);
        source.setLineWrap(true);
        source.addFocusListener(new FieldEditorFocusListener());
        // Add the global focus listener, so a menu item can see if this field was focused when an action was called.
        source.addFocusListener(Globals.getFocusListener());
        source.setFont(new Font("Monospaced", Font.PLAIN, Globals.prefs.getInt(JabRefPreferences.FONT_SIZE)));
        setupJTextComponent(source);
        updateSource();

        JScrollPane scrollPane = new JScrollPane(source, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        srcPanel.setLayout(new BorderLayout());
        srcPanel.add(scrollPane, BorderLayout.CENTER);
    }

    void addSearchListener(SearchQueryHighlightListener listener) {
        searchListeners.add(listener);
        panel.frame().getGlobalSearchBar().getSearchQueryHighlightObservable().addSearchListener(listener);
    }

    private void removeSearchListeners() {
        for (SearchQueryHighlightListener listener : searchListeners) {
            panel.frame().getGlobalSearchBar().getSearchQueryHighlightObservable().removeSearchListener(listener);
        }
    }

    public void updateSource() {
        if (updateSource) {

            try {
                String srcString = getSourceString(entry, panel.getBibDatabaseContext().getMode());
                source.setText(srcString);
                lastSourceStringAccepted = srcString;

                // Set the current Entry to be selected.
                // Fixes the bug of losing selection after, e.g. an autogeneration of a BibTeX key.
                panel.highlightEntry(entry);
            } catch (IOException ex) {
                source.setText(ex.getMessage() + "\n\n" +
                        Localization.lang("Correct the entry, and reopen editor to display/edit source."));
                source.setEditable(false);
                LOGGER.debug("Incorrect entry", ex);
            }

        }
    }

    private static String getSourceString(BibEntry entry, BibDatabaseMode type) throws IOException {
        StringWriter stringWriter = new StringWriter(200);
        LatexFieldFormatter formatter = LatexFieldFormatter
                .buildIgnoreHashes(Globals.prefs.getLatexFieldFormatterPreferences());
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
        actionMap.put("store", getStoreFieldAction());

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_PANEL), "right");
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_NEXT_PANEL_2), "right");
        actionMap.put("right", getSwitchRightAction());

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_PANEL), "left");
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.ENTRY_EDITOR_PREVIOUS_PANEL_2), "left");
        actionMap.put("left", getSwitchLeftAction());

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.HELP), "help");
        actionMap.put("help", getHelpAction());

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.NEXT_TAB), "nexttab");
        actionMap.put("nexttab", frame.nextTab);
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.PREVIOUS_TAB), "prevtab");
        actionMap.put("prevtab", frame.prevTab);

        Set<AWTKeyStroke> keys = new HashSet<>(
                textComponent.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
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
            ((EntryEditorTab) activeTab).focus();
        } else {
            source.requestFocus();
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
     * Makes sure the current edit is stored.
     */
    public void storeCurrentEdit() {
        Component comp = Globals.getFocusListener().getFocused();
        if (Objects.equals(comp, source) || ((comp instanceof FieldEditor) && this.isAncestorOf(comp))) {
            if (comp instanceof FieldEditor) {
                ((FieldEditor) comp).clearAutoCompleteSuggestion();
            }
            getStoreFieldAction().actionPerformed(new ActionEvent(comp, 0, ""));
        }
    }

    /**
     * Returns the name of the currently selected component.
     */
    public String getVisiblePanelName() {
        return tabbed.getSelectedComponent().getName();
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
            if ((tab instanceof EntryEditorTab) && ((EntryEditorTab) tab).getFields().contains(fieldName)) {
                EntryEditorTab entryEditorTab = (EntryEditorTab) tab;
                setVisiblePanel(entryEditorTab.getTabTitle());
                entryEditorTab.setActive(fieldName);
                entryEditorTab.focus();
            }
        }
    }

    private boolean storeSource() {
        BibtexParser bibtexParser = new BibtexParser(Globals.prefs.getImportFormatPreferences());
        try {
            ParserResult parserResult = bibtexParser.parse(new StringReader(source.getText()));
            BibDatabase database = parserResult.getDatabase();

            if (database.getEntryCount() > 1) {
                throw new IllegalStateException("More than one entry found.");
            }

            if (!database.hasEntries()) {
                if (parserResult.hasWarnings()) {
                    // put the warning into as exception text -> it will be displayed to the user
                    throw new IllegalStateException(parserResult.warnings().get(0));
                } else {
                    throw new IllegalStateException("No entries found.");
                }
            }

            NamedCompound compound = new NamedCompound(Localization.lang("source edit"));
            BibEntry newEntry = database.getEntries().get(0);
            String newKey = newEntry.getCiteKeyOptional().orElse(null);
            boolean entryChanged = false;
            boolean emptyWarning = (newKey == null) || newKey.isEmpty();

            if (newKey != null) {
                entry.setCiteKey(newKey);
            } else {
                entry.clearCiteKey();
            }

            // First, remove fields that the user has removed.
            for (Entry<String, String> field : entry.getFieldMap().entrySet()) {
                String fieldName = field.getKey();
                String fieldValue = field.getValue();

                if (InternalBibtexFields.isDisplayableField(fieldName) && !newEntry.hasField(fieldName)) {
                    compound.addEdit(
                            new UndoableFieldChange(entry, fieldName, fieldValue, null));
                    entry.clearField(fieldName);
                    entryChanged = true;
                }
            }

            // Then set all fields that have been set by the user.
            for (Entry<String, String> field : newEntry.getFieldMap().entrySet()) {
                String fieldName = field.getKey();
                String oldValue = entry.getField(fieldName).orElse(null);
                String newValue = field.getValue();
                if (!Objects.equals(oldValue, newValue)) {
                    // Test if the field is legally set.
                    new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences())
                            .format(newValue, fieldName);

                    compound.addEdit(new UndoableFieldChange(entry, fieldName, oldValue, newValue));
                    entry.setField(fieldName, newValue);
                    entryChanged = true;
                }
            }

            // See if the user has changed the entry type:
            if (!Objects.equals(newEntry.getType(), entry.getType())) {
                compound.addEdit(new UndoableChangeType(entry, entry.getType(), newEntry.getType()));
                entry.setType(newEntry.getType());
                entryChanged = true;
            }
            compound.end();

            if (!entryChanged) {
                return true;
            }

            panel.getUndoManager().addEdit(compound);

            if (panel.getDatabase().getDuplicationChecker().isDuplicateCiteKeyExisting(entry)) {
                warnDuplicateBibtexkey();
            } else if (emptyWarning) {
                warnEmptyBibtexkey();
            } else {
                panel.output(Localization.lang("Stored entry") + '.');
            }

            lastSourceStringAccepted = source.getText();
            // Update UI
            // TODO: we need to repaint the entryeditor if fields that are not displayed have been added
            panel.updateEntryEditorIfShowing();
            lastSourceAccepted = true;
            updateSource = true;
            // TODO: does updating work properly after source stored?
            panel.markBaseChanged();

            panel.highlightEntry(entry);

            return true;
        } catch (IllegalStateException | IOException ex) {
            // The source couldn't be parsed, so the user is given an
            // error message, and the choice to keep or revert the contents
            // of the source text field.
            updateSource = false;
            lastSourceAccepted = false;
            tabbed.setSelectedComponent(srcPanel);

            Object[] options = {Localization.lang("Edit"), Localization.lang("Revert to original source")};

            if (!SwingUtilities.isEventDispatchThread()) {
                int answer = JOptionPane.showOptionDialog(frame, Localization.lang("Error") + ": " + ex.getMessage(),
                        Localization.lang("Problem with parsing entry"), JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE, null, options, options[0]);

                if (answer != 0) {
                    updateSource = true;
                    lastSourceAccepted = true;
                    updateSource();
                }
            }

            LOGGER.debug("Incorrect source", ex);

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
     * Update the JTextArea when a field has changed.
     */
    @Subscribe
    @SuppressWarnings("unused")
    public void listen(FieldChangedEvent fieldChangedEvent) {
        String newValue = fieldChangedEvent.getNewValue() == null ? "" : fieldChangedEvent.getNewValue();
        if (SwingUtilities.isEventDispatchThread()) {
            setField(fieldChangedEvent.getFieldName(), newValue);
        } else {
            SwingUtilities.invokeLater(() -> setField(fieldChangedEvent.getFieldName(), newValue));
        }
    }

    public void updateField(final Object sourceObject) {
        getStoreFieldAction().actionPerformed(new ActionEvent(sourceObject, 0, ""));
    }

    public void setMovingToDifferentEntry() {
        movingToDifferentEntry = true;
        unregisterListeners();
    }

    private void unregisterListeners() {
        entry.unregisterListener(this);
        removeSearchListeners();
    }

    private class TypeButton extends JButton {
        public TypeButton() {
            super(IconTheme.JabRefIcon.EDIT.getIcon());
            setToolTipText(Localization.lang("Change entry type"));
            addActionListener(e -> showChangeEntryTypePopupMenu());
        }
    }

    private void showChangeEntryTypePopupMenu() {
        JPopupMenu typeMenu = new ChangeEntryTypeMenu().getChangeentryTypePopupMenu(panel);
        typeMenu.show(this, 0, 0);
    }

    private class TypeLabel extends JLabel {
        public TypeLabel(String type) {
            super(type);
            setUI(new VerticalLabelUI(false));
            setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(new Font("dialog", Font.ITALIC + Font.BOLD, 18));

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

    /**
     * Focus listener that fires the storeFieldAction when a TextArea loses focus.
     */
    private class FieldListener extends FocusAdapter {
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
            // We tell the editor tab to update all its fields. This makes sure they are updated even if the tab we
            // just left contained one or more of the same fields as this one:
            SwingUtilities.invokeLater(() -> {
                Object activeTab = tabs.get(tabbed.getSelectedIndex());
                if (activeTab instanceof EntryEditorTab) {
                    ((EntryEditorTab) activeTab).updateAll();
                    activateVisible();
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
            panel.getUndoManager().addEdit(new UndoableRemoveEntry(panel.getDatabase(), entry, panel));
            panel.output(Localization.lang("Deleted entry"));
        }
    }

    public void close() {
        if (tabbed.getSelectedComponent() == srcPanel) {
            updateField(source);
            if (lastSourceAccepted) {
                panel.entryEditorClosing(EntryEditor.this);
            } else {
                panel.runCommand(Actions.SAVE);
                lastSourceAccepted = true;
            }
        } else {
            if (lastFieldAccepted) {
                panel.entryEditorClosing(EntryEditor.this);
            } else {
                panel.runCommand(Actions.SAVE);
                lastFieldAccepted = true;
            }
        }
    }

    class CloseAction extends AbstractAction {
        public CloseAction() {
            super(Localization.lang("Close window"), IconTheme.JabRefIcon.CLOSE.getSmallIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close window"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }

    class StoreFieldAction extends AbstractAction {

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
                String oldValue = entry.getCiteKeyOptional().orElse(null);
                String newValue = textField.getText();

                if (newValue.isEmpty()) {
                    newValue = null;
                }

                if (((oldValue == null) && (newValue == null)) || (Objects.equals(oldValue, newValue))) {
                    return; // No change.
                }

                // Make sure the key is legal:
                String cleaned = BibtexKeyPatternUtil.checkLegalKey(newValue,
                        Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
                if ((cleaned == null) || cleaned.equals(newValue)) {
                    textField.setValidBackgroundColor();
                } else {
                    lastFieldAccepted = false;
                    textField.setInvalidBackgroundColor();
                    if (!SwingUtilities.isEventDispatchThread()) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("Invalid BibTeX key"),
                                Localization.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                        requestFocus();
                    }
                    return;
                }

                if (newValue == null) {
                    entry.clearCiteKey();
                    warnEmptyBibtexkey();
                } else {
                    entry.setCiteKey(newValue);
                    boolean isDuplicate = panel.getDatabase().getDuplicationChecker().isDuplicateCiteKeyExisting(entry);
                    if (isDuplicate) {
                        warnDuplicateBibtexkey();
                    } else {
                        panel.output(Localization.lang("BibTeX key is unique."));
                    }
                }

                // Add an UndoableKeyChange to the baseframe's undoManager.
                UndoableKeyChange undoableKeyChange = new UndoableKeyChange(entry, oldValue, newValue);
                if (updateTimeStampIsSet()) {
                    NamedCompound ce = new NamedCompound(undoableKeyChange.getPresentationName());
                    ce.addEdit(undoableKeyChange);
                    doUpdateTimeStamp().ifPresent(fieldChange -> ce.addEdit(new UndoableFieldChange(fieldChange)));
                    ce.end();
                    panel.getUndoManager().addEdit(ce);
                } else {
                    panel.getUndoManager().addEdit(undoableKeyChange);
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
                String currentText = fieldEditor.getText().trim();
                if (!currentText.isEmpty()) {
                    toSet = currentText;
                }

                // We check if the field has changed, since we don't want to
                // mark the base as changed unless we have a real change.
                if (toSet == null) {
                    set = entry.hasField(fieldEditor.getFieldName());
                } else {
                    set = !((entry.hasField(fieldEditor.getFieldName()))
                            && toSet.equals(entry.getField(fieldEditor.getFieldName()).orElse(null)));
                }

                if (!set) {
                    // We set the field and label color.
                    fieldEditor.setValidBackgroundColor();
                } else {
                    try {
                        // The following statement attempts to write the new contents into a StringWriter, and this will
                        // cause an IOException if the field is not properly formatted. If that happens, the field
                        // is not stored and the textarea turns red.
                        if (toSet != null) {
                            new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()).format(toSet,
                                    fieldEditor.getFieldName());
                        }

                        String oldValue = entry.getField(fieldEditor.getFieldName()).orElse(null);

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
                        UndoableFieldChange undoableFieldChange = new UndoableFieldChange(entry,
                                fieldEditor.getFieldName(), oldValue, toSet);
                        if (updateTimeStampIsSet()) {
                            NamedCompound ce = new NamedCompound(undoableFieldChange.getPresentationName());
                            ce.addEdit(undoableFieldChange);

                            doUpdateTimeStamp()
                                    .ifPresent(fieldChange -> ce.addEdit(new UndoableFieldChange(fieldChange)));
                            ce.end();

                            panel.getUndoManager().addEdit(ce);

                        } else {
                            panel.getUndoManager().addEdit(undoableFieldChange);
                        }
                        updateSource();
                        panel.markBaseChanged();
                    } catch (IllegalArgumentException ex) {
                        lastFieldAccepted = false;
                        fieldEditor.setInvalidBackgroundColor();
                        if (!SwingUtilities.isEventDispatchThread()) {
                            JOptionPane.showMessageDialog(frame, Localization.lang("Error") + ": " + ex.getMessage(),
                                    Localization.lang("Error setting field"), JOptionPane.ERROR_MESSAGE);
                            LOGGER.debug("Error setting field", ex);
                            requestFocus();
                        }
                    }
                }
                if (fieldEditor.getTextComponent().hasFocus()) {
                    fieldEditor.setBackground(GUIGlobals.ACTIVE_EDITOR_COLOR);
                }
            } else if (source.isEditable() && !source.getText().equals(lastSourceStringAccepted)) {
                validEntry = storeSource();
            }

            // Make sure we scroll to the entry if it moved in the table.
            // Should only be done if this editor is currently showing:
            if (!movingAway && isShowing()) {
                panel.highlightEntry(entry);
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
            panel.selectNextEntry();
        }
    }

    class PrevEntryAction extends AbstractAction {
        public PrevEntryAction() {
            super(Localization.lang("Previous entry"), IconTheme.JabRefIcon.UP.getIcon());

            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Previous entry"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.selectPreviousEntry();
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

            // Store the current edit in case this action is called during the editing of a field:
            storeCurrentEdit();

            // This is a partial clone of net.sf.jabref.gui.BasePanel.setupActions().new AbstractWorker() {...}.run()

            // this updates the table automatically, on close, but not within the tab
            Optional<String> oldValue = entry.getCiteKeyOptional();

            if (oldValue.isPresent()) {
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

            BibtexKeyPatternUtil.makeLabel(panel.getBibDatabaseContext().getMetaData()
                    .getCiteKeyPattern(Globals.prefs.getBibtexKeyPatternPreferences().getKeyPattern()), panel.getDatabase(), entry,
                    Globals.prefs.getBibtexKeyPatternPreferences());

            // Store undo information:
            panel.getUndoManager().addEdit(
                    new UndoableKeyChange(entry, oldValue.orElse(null),
                            entry.getCiteKeyOptional().get())); // Cite key always set here

            // here we update the field
            String bibtexKeyData = entry.getCiteKeyOptional().get();
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
                EntryEditorTab tab = (EntryEditorTab) activeTab;
                FieldEditor fieldEditor = tab.getActive();
                fieldEditor.clearAutoCompleteSuggestion();
                updateField(fieldEditor);
            } else {
                // Source panel.
                updateField(activeTab);
            }

            if (validEntry) {
                panel.runCommand(Actions.SAVE);
            }
        }
    }

    private void warnDuplicateBibtexkey() {
        panel.output(Localization.lang("Duplicate BibTeX key") + ". "
                + Localization.lang("Grouping may not work for this entry."));
    }

    private void warnEmptyBibtexkey() {
        panel.output(Localization.lang("Empty BibTeX key") + ". " + Localization.lang("Grouping may not work for this entry."));
    }


    public AbstractAction getNextEntryAction() {
        return nextEntryAction;
    }


    public AbstractAction getPrevEntryAction() {
        return prevEntryAction;
    }


    public SwitchLeftAction getSwitchLeftAction() {
        return switchLeftAction;
    }


    public SwitchRightAction getSwitchRightAction() {
        return switchRightAction;
    }


    public SaveDatabaseAction getSaveDatabaseAction() {
        return saveDatabaseAction;
    }


    public HelpAction getHelpAction() {
        return helpAction;
    }


    public GenerateKeyAction getGenerateKeyAction() {
        return generateKeyAction;
    }


    public StoreFieldAction getStoreFieldAction() {
        return storeFieldAction;
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


    private boolean updateTimeStampIsSet() {
        return Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP)
                && Globals.prefs.getBoolean(JabRefPreferences.UPDATE_TIMESTAMP);
    }

    /**
     * Updates the timestamp of the given entry and returns the FieldChange
     */
    private Optional<FieldChange> doUpdateTimeStamp() {
        String timeStampField = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD);
        String timeStampFormat = Globals.prefs.get(JabRefPreferences.TIME_STAMP_FORMAT);
        String timestamp = EasyDateFormat.fromTimeStampFormat(timeStampFormat).getCurrentDate();
        return UpdateField.updateField(entry, timeStampField, timestamp);
    }

}
