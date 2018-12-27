package org.jabref.gui.plaintextimport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.OSXCompatibleToolbar;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.component.OverlayPanel;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.FreeCiteImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.EntryTypes;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * import from plain text => simple mark/copy/paste into bibtex entry
 * <p>
 * TODO
 * - change colors and fonts
 * - delete selected text
 * - make textarea editable
 * - create several bibtex entries in dialog
 * - if the dialog works with an existing entry (right click menu item), the cancel option doesn't work well
 */
public class TextInputDialog extends JabRefDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextInputDialog.class);

    private final JButton okButton = new JButton(Localization.lang("Accept"));
    private final JButton cancelButton = new JButton(Localization.lang("Cancel"));
    private final JButton insertButton = new JButton(Localization.lang("Insert"));
    private final JButton parseWithFreeCiteButton = new JButton(Localization.lang("Parse with FreeCite"));
    private final JPanel panel1 = new JPanel();
    private final JPanel buttons = new JPanel();
    private final JPanel rawPanel = new JPanel();
    private final JPanel sourcePanel = new JPanel();
    private JList<String> fieldList;
    private final JRadioButton override = new JRadioButton(Localization.lang("Override"));
    private final JRadioButton append = new JRadioButton(Localization.lang("Append"));
    private final JToolBar toolBar = new OSXCompatibleToolbar();

    private final List<String> allFields = new ArrayList<>();
    private final List<String> requiredFields = new ArrayList<>();
    private final List<String> optionalFields = new ArrayList<>();

    private final BibEntry entry;

    private final JPopupMenu inputMenu = new JPopupMenu();
    private StyledDocument document; // content from inputPane
    private final JTextPane textPane = new JTextPane();
    private final JTextArea sourcePreview = new JTextArea();

    private final TagToMarkedTextStore markedTextStore;

    private final JabRefFrame frame;

    private boolean okPressed;


    public TextInputDialog(JabRefFrame frame, BibEntry bibEntry) {
        super(true, TextInputDialog.class);

        this.frame = Objects.requireNonNull(frame);

        entry = Objects.requireNonNull(bibEntry);
        markedTextStore = new TagToMarkedTextStore();

        jbInit();
        pack();
        updateSourceView();
    }

    private void jbInit() {
        getContentPane().setLayout(new BorderLayout());
        StringBuilder typeStr = new StringBuilder("Plain text import");
        if (entry.getType() != null) {
            typeStr.append(' ').append(Localization.lang("for")).append(' ').append(entry.getType());
        }

        this.setTitle(typeStr.toString());
        getContentPane().add(panel1, BorderLayout.CENTER);

        initRawPanel();
        initButtonPanel();
        initSourcePanel();

        JTabbedPane tabbed = new JTabbedPane();

        tabbed.add(rawPanel, Localization.lang("Raw source"));
        tabbed.add(sourcePanel, Localization.lang("%0 source", frame.getCurrentBasePanel().getBibDatabaseContext().getMode().getFormattedName()));

        // Panel Layout
        panel1.setLayout(new BorderLayout());
        panel1.add(tabbed, BorderLayout.CENTER);
        panel1.add(buttons, BorderLayout.SOUTH);

        // Key bindings:
        ActionMap am = buttons.getActionMap();
        InputMap im = buttons.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE), "close");
        am.put("close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    // Panel with text import functionality
    private void initRawPanel() {
        rawPanel.setLayout(new BorderLayout());

        // Textarea
        textPane.setEditable(false);

        document = textPane.getStyledDocument();
        addStylesToDocument();

        try {
            document.insertString(0, "", document.getStyle("regular"));
        } catch (BadLocationException ex) {
            LOGGER.warn("Problem setting style", ex);

        }

        OverlayPanel testPanel = new OverlayPanel(textPane, Localization.lang("paste text here"));

        testPanel.setPreferredSize(new Dimension(450, 255));
        testPanel.setMaximumSize(new Dimension(450, Integer.MAX_VALUE));

        // Setup fields (required to be done before setting up popup menu)
        fieldList = new JList<>(getAllFields());
        fieldList.setCellRenderer(new SimpleCellRenderer(fieldList.getFont()));
        ListSelectionModel listSelectionModel = fieldList.getSelectionModel();
        listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listSelectionModel.addListSelectionListener(new FieldListSelectionHandler());
        fieldList.addMouseListener(new FieldListMouseListener());

        // After the call to getAllFields
        initPopupMenuAndToolbar();

        //Add listener to components that can bring up popup menus.
        MouseListener popupListener = new PopupListener(inputMenu);
        textPane.addMouseListener(popupListener);
        testPanel.addMouseListener(popupListener);

        JPanel leftPanel = new JPanel(new BorderLayout());

        leftPanel.add(toolBar, BorderLayout.NORTH);
        leftPanel.add(testPanel, BorderLayout.CENTER);

        JPanel inputPanel = setUpFieldListPanel();

        // parse with FreeCite button
        parseWithFreeCiteButton.addActionListener(event -> {
            if (parseWithFreeCiteAndAddEntries()) {
                okPressed = false; // we do not want to have the super method to handle our entries, we do it on our own
                dispose();
            }
        });

        rawPanel.add(leftPanel, BorderLayout.CENTER);
        rawPanel.add(inputPanel, BorderLayout.EAST);

        JLabel desc = new JLabel("<html><h3>" + Localization.lang("Plain text import") + "</h3><p>"
                + Localization.lang("This is a simple copy and paste dialog. First load or paste some text into "
                + "the text input area.<br>After that, you can mark text and assign it to a BibTeX field.")
                + "</p></html>");
        desc.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        rawPanel.add(desc, BorderLayout.SOUTH);
    }

    private JPanel setUpFieldListPanel() {
        JPanel inputPanel = new JPanel();

        // Panel Layout
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints con = new GridBagConstraints();
        con.weightx = 0;
        con.insets = new Insets(5, 5, 0, 5);
        con.fill = GridBagConstraints.HORIZONTAL;

        inputPanel.setLayout(gbl);

        // Border
        TitledBorder titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 2),
                Localization.lang("Work options"));
        inputPanel.setBorder(titledBorder1);
        inputPanel.setMinimumSize(new Dimension(10, 10));

        JScrollPane fieldScroller = new JScrollPane(fieldList);
        fieldScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // insert buttons
        insertButton.addActionListener(event -> insertTextForTag(override.isSelected()));

        // Radio buttons
        append.setToolTipText(Localization.lang("Append the selected text to BibTeX field"));
        append.setMnemonic(KeyEvent.VK_A);
        append.setSelected(true);

        override.setToolTipText(Localization.lang("Override the BibTeX field by the selected text"));
        override.setMnemonic(KeyEvent.VK_O);
        override.setSelected(false);

        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(append);
        group.add(override);

        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(append);
        radioPanel.add(override);

        // insert sub components
        JLabel label1 = new JLabel(Localization.lang("Available BibTeX fields"));
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(label1, con);
        inputPanel.add(label1);

        con.gridwidth = GridBagConstraints.REMAINDER;
        con.gridheight = 8;
        con.weighty = 1;
        con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(fieldScroller, con);
        inputPanel.add(fieldScroller);

        con.fill = GridBagConstraints.HORIZONTAL;
        con.weighty = 0;
        con.gridwidth = 2;
        gbl.setConstraints(radioPanel, con);
        inputPanel.add(radioPanel);

        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(insertButton, con);
        inputPanel.add(insertButton);
        return inputPanel;
    }

    private void initPopupMenuAndToolbar() {
        // copy/paste Menu
        PasteAction pasteAction = new PasteAction();
        ClearAction clearAction = new ClearAction();
        JMenuItem pasteMI = new JMenuItem(pasteAction);

        inputMenu.add(clearAction);
        inputMenu.addSeparator();
        inputMenu.add(pasteMI);
        inputMenu.addSeparator();

        // Right-click append/override
        JMenu appendMenu = new JMenu(Localization.lang("Append"));
        appendMenu.setToolTipText(Localization.lang("Append the selected text to BibTeX field"));
        JMenu overrideMenu = new JMenu(Localization.lang("Override"));
        overrideMenu.setToolTipText(Localization.lang("Override the BibTeX field by the selected text"));
        for (String field : allFields) {
            appendMenu.add(new JMenuItem(new MenuTextForTagAction(field, false)));
            overrideMenu.add(new JMenuItem(new MenuTextForTagAction(field, true)));
        }

        inputMenu.add(appendMenu);
        inputMenu.add(overrideMenu);

        // Toolbar

        toolBar.add(clearAction);
        toolBar.setBorderPainted(false);
        toolBar.addSeparator();
        toolBar.add(pasteAction);
        toolBar.add(new LoadAction());
    }

    private void initButtonPanel() {
        okButton.addActionListener(event -> {
            okPressed = true;
            dispose();
        });
        cancelButton.addActionListener(event -> dispose());

        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(okButton);
        bb.addButton(parseWithFreeCiteButton);
        bb.addButton(cancelButton);
        bb.addGlue();
    }

    // Panel with BibTeX source code
    private void initSourcePanel() {
        sourcePreview.setEditable(false);
        sourcePreview.setFont(new Font("Monospaced", Font.PLAIN, Globals.prefs.getInt(JabRefPreferences.FONT_SIZE)));
        JScrollPane paneScrollPane = new JScrollPane(sourcePreview);
        paneScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        paneScrollPane.setPreferredSize(new Dimension(500, 255));
        paneScrollPane.setMinimumSize(new Dimension(10, 10));

        sourcePanel.setLayout(new BorderLayout());
        sourcePanel.add(paneScrollPane, BorderLayout.CENTER);
    }

    private void addStylesToDocument() {
        //Initialize some styles.
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regularStyle = document.addStyle("regular", defaultStyle);
        StyleConstants.setFontFamily(defaultStyle, "SansSerif");
        StyleConstants.setFontSize(defaultStyle, Globals.prefs.getInt(JabRefPreferences.FONT_SIZE));

        Style s = document.addStyle("used", regularStyle);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, Color.blue);

        s = document.addStyle("marked", regularStyle);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, Color.red);
    }

    private void insertTextForTag(boolean overrideField) {
        String fieldName = fieldList.getSelectedValue();
        if (fieldName != null) {
            String txt = textPane.getSelectedText();

            if (txt != null) {
                int selectionStart = textPane.getSelectionStart();
                int selectionEnd = textPane.getSelectionEnd();

                // unselect text
                textPane.setSelectionEnd(selectionStart);

                // mark the selected text as "used"
                document.setCharacterAttributes(selectionStart, selectionEnd - selectionStart,
                        document.getStyle("marked"), true);

                // override an existing entry
                if (overrideField) {
                    entry.setField(fieldName, txt);
                    // erase old text selection
                    markedTextStore.setStyleForTag(fieldName, "regular", document); // delete all previous styles
                    markedTextStore.insertPosition(fieldName, selectionStart, selectionEnd); // insert new selection style
                } else {
                    // memorize the selection for text highlighting
                    markedTextStore.appendPosition(fieldName, selectionStart, selectionEnd);

                    // get old text from BibTeX tag
                    Optional<String> old = entry.getField(fieldName);

                    // merge old and selected text
                    if (old.isPresent()) {
                        // insert a new name with an additional "and"
                        if (InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.PERSON_NAMES)) {
                            entry.setField(fieldName, old.get() + " and " + txt);
                        } else if (FieldName.KEYWORDS.equals(fieldName)) {
                            // Add keyword
                            entry.addKeyword(txt, Globals.prefs.getKeywordDelimiter());
                        } else {
                            entry.setField(fieldName, old.get() + txt);
                        }
                    } else {
                        // "null"+"txt" Strings forbidden
                        entry.setField(fieldName, txt);
                    }
                }
                // make the new data in BibTeX source code visible
                updateSourceView();
            }
        }
    }

    public boolean okPressed() {
        return okPressed;
    }

    /**
     * tries to parse the pasted reference with freecite
     *
     * @return true if successful, false otherwise
     */
    private boolean parseWithFreeCiteAndAddEntries() {
        FreeCiteImporter fimp = new FreeCiteImporter(Globals.prefs.getImportFormatPreferences());
        String text = textPane.getText();

        // we have to remove line breaks (but keep empty lines)
        // otherwise, the result is broken
        text = text.replace(OS.NEWLINE.concat(OS.NEWLINE), "##NEWLINE##");
        // possible URL line breaks are removed completely.
        text = text.replace("/".concat(OS.NEWLINE), "/");
        text = text.replace(OS.NEWLINE, " ");
        text = text.replace("##NEWLINE##", OS.NEWLINE);

        ParserResult importerResult = fimp.importEntries(text);
        if (importerResult.hasWarnings()) {
            frame.showMessage(importerResult.getErrorMessage());
        }
        List<BibEntry> importedEntries = importerResult.getDatabase().getEntries();
        if (importedEntries.isEmpty()) {
            return false;
        } else {
            UpdateField.setAutomaticFields(importedEntries, false, false, Globals.prefs.getUpdateFieldPreferences());

            importedEntries.forEach(entry -> frame.getCurrentBasePanel().insertEntry(entry));
            return true;
        }
    }

    // update the bibtex source view and available List
    private void updateSourceView() {
        StringWriter sw = new StringWriter(200);
        try {
            new BibEntryWriter(new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()),
                    false).write(entry, sw, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
            sourcePreview.setText(sw.getBuffer().toString());
        } catch (IOException ex) {
            LOGGER.error("Error in entry" + ": " + ex.getMessage(), ex);
        }

        fieldList.clearSelection();
    }

    private String[] getAllFields() {
        Optional<EntryType> type = EntryTypes.getType(entry.getType(),
                frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
        if (type.isPresent()) {
            allFields.addAll(type.get().getAllFields());
            requiredFields.addAll(type.get().getRequiredFieldsFlat());
            optionalFields.addAll(type.get().getPrimaryOptionalFields());
        }
        for (String field : InternalBibtexFields.getAllPublicFieldNames()) {
            if (!allFields.contains(field)) {
                allFields.add(field);
            }
        }
        return allFields.toArray(new String[allFields.size()]);
    }

    private class PasteAction extends BasicAction {

        public PasteAction() {
            super(Localization.lang("Paste"), Localization.lang("Paste from clipboard"),
                    IconTheme.JabRefIcons.PASTE.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String data = Globals.clipboardManager.getContents();
            int selStart = textPane.getSelectionStart();
            int selEnd = textPane.getSelectionEnd();
            if ((selEnd - selStart) > 0) {
                textPane.replaceSelection("");
            }
            int cPos = textPane.getCaretPosition();
            try {
                document.insertString(cPos, data, document.getStyle("regular"));
            } catch (BadLocationException ex) {
                LOGGER.warn("Could not paste text", ex);
            }
        }
    }

    private class LoadAction extends BasicAction {

        public LoadAction() {
            super(Localization.lang("Open"), Localization.lang("Open file"), IconTheme.JabRefIcons.OPEN.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                        .addExtensionFilter(Localization.lang("Plain text"), StandardFileType.TXT)
                        .withDefaultExtension(Localization.lang("Plain text"), StandardFileType.TXT)
                        .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
                DialogService ds = frame.getDialogService();

                Optional<Path> path = DefaultTaskExecutor
                        .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));

                if (path.isPresent()) {
                    Path file = path.get();
                    document.remove(0, document.getLength());
                    EditorKit eKit = textPane.getEditorKit();
                    if (eKit != null) {
                        try (InputStream fis = Files.newInputStream(file)) {
                            eKit.read(fis, document, 0);
                            document.setLogicalStyle(0, document.getStyle("regular"));
                        }
                    }
                }
            } catch (BadLocationException | IOException ex) {
                LOGGER.warn("Problem reading or inserting file", ex);
            }
        }
    }

    private class ClearAction extends BasicAction {

        public ClearAction() {
            super(Localization.lang("Clear"), Localization.lang("Clear inputarea"), IconTheme.JabRefIcons.NEW.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            textPane.setText("");
        }
    }

    class FieldListSelectionHandler implements ListSelectionListener {

        private int lastIndex = -1;

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();

            int index = lsm.getAnchorSelectionIndex();
            if (index != lastIndex) {
                boolean isAdjusting = e.getValueIsAdjusting();

                if (!isAdjusting) {
                    if (lastIndex > -1) {
                        String tag1 = fieldList.getModel().getElementAt(lastIndex);
                        markedTextStore.setStyleForTag(tag1, "used", document);
                    }

                    String tag2 = fieldList.getModel().getElementAt(index);
                    markedTextStore.setStyleForTag(tag2, "marked", document);

                    lastIndex = index;
                }
            }
        }
    }

    // simple JList Renderer
    // based on : Advanced JList Programming at developers.sun.com
    private class SimpleCellRenderer extends DefaultListCellRenderer {

        private final Font baseFont;
        private final Font usedFont;
        private final Icon okIcon = IconTheme.JabRefIcons.PLAIN_TEXT_IMPORT_DONE.getSmallIcon();
        private final Icon needIcon = IconTheme.JabRefIcons.PLAIN_TEXT_IMPORT_TODO.getSmallIcon();
        private final Color requiredColor = new Color(230, 235, 255);
        private final Color optionalColor = new Color(230, 255, 230);


        public SimpleCellRenderer(Font normFont) {
            baseFont = normFont;
            usedFont = baseFont.deriveFont(Font.ITALIC);
        }

        /* This is the only method defined by ListCellRenderer.  We just
         * reconfigure the Jlabel each time we're called.
         */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, // value to display
                                                      int index, // cell index
                                                      boolean iss, // is the cell selected
                                                      boolean chf) // the list and the cell have the focus
        {
            /* The DefaultListCellRenderer class will take care of
             * the JLabels text property, it's foreground and background
             * colors, and so on.
             */
            super.getListCellRendererComponent(list, value, index, iss, chf);

            /* We additionally set the JLabels icon property here.
             */
            String s = value.toString();
            if (entry.hasField(s)) {
                this.setForeground(Color.gray);
                this.setFont(usedFont);
                this.setIcon(okIcon);
                this.setToolTipText(Localization.lang("Filled"));
            } else {
                this.setIcon(needIcon);
                this.setToolTipText(Localization.lang("Field is missing"));
            }
            if (requiredFields.contains(s)) {
                this.setBackground(requiredColor);
            } else if (optionalFields.contains(s)) {
                this.setBackground(optionalColor);
            }
            return this;
        }
    }

    private class FieldListMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                insertTextForTag(override.isSelected());
            }
        }
    }

    private class MenuTextForTagAction extends AbstractAction {

        private final String field;
        private final Boolean overrideField;


        public MenuTextForTagAction(String field, Boolean overrideField) {
            super(field);
            this.field = field;
            this.overrideField = overrideField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // To enable correct marking of used values
            fieldList.setSelectedValue(field, false);
            insertTextForTag(overrideField);
        }
    }
}

class PopupListener extends MouseAdapter {

    private final JPopupMenu popMenu;


    public PopupListener(JPopupMenu menu) {
        popMenu = menu;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}

abstract class BasicAction extends AbstractAction {

    public BasicAction(String text, String description, Icon icon) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, description);
    }

    public BasicAction(String text) {
        super(text);
    }

    @Override
    public abstract void actionPerformed(ActionEvent e);
}
