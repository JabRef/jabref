/*
 Copyright (C) 2004 R. Nagel

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

// created by : r.nagel 14.09.2004
//
// function : import from plain text => simple mark/copy/paste into bibtex entry
//
// todo     : - change colors and fonts
//            - delete selected text
//            - make textarea editable
//            - create several bibtex entries in dialog
//            - if the dialog works with an existing entry (right click menu item)
//              the cancel option doesn't work well
//
// modified :
//            28.07.2005
//            - fix: insert button doesnt work
//            - append a author with "and"
//            04.11.2004
//            - experimental: text-input-area with underlying infotext
//            02.11.2004
//            - integrity check, which reports errors and warnings for the fields
//            22.10.2004
//            - little help box
//

package net.sf.jabref.wizard.text.gui;

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
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.EditorKit;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.gui.*;
import net.sf.jabref.bibtex.BibtexEntryWriter;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;
import net.sf.jabref.importer.fileformat.FreeCiteImporter;
import net.sf.jabref.wizard.integrity.gui.IntegrityMessagePanel;
import net.sf.jabref.wizard.text.TagToMarkedTextStore;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class TextInputDialog extends JDialog implements ActionListener {
    private final JButton okButton = new JButton();
    private final JButton cancelButton = new JButton();
    private final JButton insertButton = new JButton();
    private final JButton parseWithFreeCiteButton = new JButton();
    private final JPanel panel1 = new JPanel();
    private final JPanel buttons = new JPanel();
    private final JPanel rawPanel = new JPanel();
    private final JPanel sourcePanel = new JPanel();
    private final IntegrityMessagePanel warnPanel;
    private JList<String> fieldList;
    private JRadioButton overRadio;

    private final BibtexEntry entry;

    private final JPopupMenu inputMenu = new JPopupMenu();
    private StyledDocument doc; // content from inputPane
    private JTextPane textPane;
    private JTextArea preview;

    private final boolean inputChanged; // input changed, fired by insert buttons

    private final TagToMarkedTextStore marked;

    private final JabRefFrame _frame;

    private boolean okPressed;

    public TextInputDialog(JabRefFrame frame, BasePanel panel, String title, boolean modal, BibtexEntry bibEntry) {
        super(frame, title, modal);

        warnPanel = new IntegrityMessagePanel(panel);
        inputChanged = true; // for a first validCheck

        _frame = frame;

        entry = bibEntry;
        marked = new TagToMarkedTextStore();

        try {
            jbInit(frame);
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        updateSourceView();
    }

    private void jbInit(JabRefFrame parent) {
        this.setModal(true);
        //this.setResizable( false ) ;
        getContentPane().setLayout(new BorderLayout());
        String typeStr = Localization.lang("for");
        if (entry != null)
        {
            if (entry.getType() != null)
            {
                typeStr = typeStr + " " + entry.getType().getName();
            }
        }

        this.setTitle(Localization.lang("Plain_text_import") + " " + typeStr);
        getContentPane().add(panel1, BorderLayout.CENTER);

        initRawPanel();
        initButtonPanel();
        initSourcePanel();

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.addChangeListener(
                new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e)
                    {
                        if (inputChanged)
                        {
                            warnPanel.updateView(entry);
                        }
                    }
                });

        tabbed.add(rawPanel, Localization.lang("Raw_source"));
        tabbed.add(sourcePanel, Localization.lang("BibTeX_source"));
        tabbed.add(warnPanel, Localization.lang("Messages_and_Hints"));

        // Panel Layout
        panel1.setLayout(new BorderLayout());
        panel1.add(tabbed, BorderLayout.CENTER);
        panel1.add(buttons, BorderLayout.SOUTH);

        // Key bindings:
        ActionMap am = buttons.getActionMap();
        InputMap im = buttons.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(parent.prefs().getKey("Close dialog"), "close");
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
        textPane = new JTextPane();

        textPane.setEditable(false);

        doc = textPane.getStyledDocument();
        addStylesToDocument(doc);

        try {
            doc.insertString(0, "", doc.getStyle("regular"));
        } catch (Exception ignored) {
        }

        OverlayPanel testPanel = new OverlayPanel(textPane,
                Localization.lang("Text_Input_Area"));

        testPanel.setPreferredSize(new Dimension(450, 255));
        testPanel.setMaximumSize(new Dimension(450, Integer.MAX_VALUE));

        // copy/paste Menu
        PasteAction pasteAction = new PasteAction();
        JMenuItem pasteMI = new JMenuItem(pasteAction);
        inputMenu.add(new MenuHeaderAction());
        inputMenu.addSeparator();
        inputMenu.add(pasteMI);

        //Add listener to components that can bring up popup menus.
        MouseListener popupListener = new PopupListener(inputMenu);
        textPane.addMouseListener(popupListener);
        testPanel.addMouseListener(popupListener);

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.add(new ClearAction());
        toolBar.setBorderPainted(false);
        toolBar.addSeparator();
        toolBar.add(pasteAction);
        toolBar.add(new LoadAction());

        JPanel leftPanel = new JPanel(new BorderLayout());

        leftPanel.add(toolBar, BorderLayout.NORTH);
        leftPanel.add(testPanel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();

        // Panel Layout
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints con = new GridBagConstraints();
        con.weightx = 0;
        con.insets = new Insets(5, 5, 0, 5);
        con.fill = GridBagConstraints.HORIZONTAL;

        inputPanel.setLayout(gbl);

        // Border
        TitledBorder titledBorder1 = new TitledBorder(
                BorderFactory.createLineBorder(
                        new Color(153, 153, 153), 2),
                Localization.lang("Input"));
        inputPanel.setBorder(titledBorder1);
        //inputPanel.setPreferredSize( new Dimension( 200, 255 ) ) ;
        inputPanel.setMinimumSize(new Dimension(10, 10));

        fieldList = new JList<>(getAllFields());
        fieldList.setCellRenderer(new SimpleCellRenderer(fieldList.getFont()));
        ListSelectionModel listSelectionModel = fieldList.getSelectionModel();
        listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listSelectionModel.addListSelectionListener(new FieldListSelectionHandler());
        fieldList.addMouseListener(new FieldListMouseListener());

        JScrollPane fieldScroller = new JScrollPane(fieldList);
        fieldScroller.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //fieldScroller.setPreferredSize( new Dimension( 180, 190 ) ) ;
        //fieldScroller.setMinimumSize( new Dimension( 180, 190 ) ) ;

        // insert buttons
        insertButton.setText(Localization.lang("Insert"));
        insertButton.addActionListener(this);

        // parse with FreeCite button
        parseWithFreeCiteButton.setText(Localization.lang("Parse with FreeCite"));
        parseWithFreeCiteButton.addActionListener(this);

        // Radio buttons
        JRadioButton appRadio = new JRadioButton(Localization.lang("Append"));
        appRadio.setToolTipText(Localization.lang("append_the_selected_text_to_bibtex_key"));
        appRadio.setMnemonic(KeyEvent.VK_A);
        appRadio.setSelected(true);

        overRadio = new JRadioButton(Localization.lang("Override"));
        overRadio.setToolTipText(Localization.lang("override_the_bibtex_key_by_the_selected_text"));
        overRadio.setMnemonic(KeyEvent.VK_O);
        overRadio.setSelected(false);

        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(appRadio);
        group.add(overRadio);

        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(appRadio);
        radioPanel.add(overRadio);

        // insert sub components
        JLabel label1 = new JLabel(Localization.lang("Available fields"));
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

        rawPanel.add(leftPanel, BorderLayout.CENTER);
        rawPanel.add(inputPanel, BorderLayout.EAST);

        JLabel desc = new JLabel("<html><h3>" + Localization.lang("Plain text import") + "</h3><p>"
                + Localization.lang("This is a simple copy and paste dialog. First load or paste some text into "
                + "the text input area.<br>After that, you can mark text and assign it to a BibTeX field.")
                + "</p></html>");
        desc.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        /*infoText.setEditable(false);
        infoText.setBackground(GUIGlobals.infoField);
        infoText.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        infoText.setPreferredSize( new Dimension(220, 50));
        infoText.setMinimumSize( new Dimension(180, 50));*/

        rawPanel.add(desc, BorderLayout.SOUTH);
    }

    private void initButtonPanel() {
        okButton.setText(Localization.lang("Accept"));
        okButton.addActionListener(this);
        cancelButton.setText(Localization.lang("Cancel"));
        cancelButton.addActionListener(this);

        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(okButton);
        bb.addButton(parseWithFreeCiteButton);
        bb.addButton(cancelButton);
        bb.addGlue();
    }

    // Panel with bibtex source code
    private void initSourcePanel() {
        //    preview =  new PreviewPanel(entry) ;
        preview = new JTextArea();
        preview.setEditable(false);

        JScrollPane paneScrollPane = new JScrollPane(preview);
        paneScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        paneScrollPane.setPreferredSize(new Dimension(500, 255));
        paneScrollPane.setMinimumSize(new Dimension(10, 10));

        sourcePanel.setLayout(new BorderLayout());
        sourcePanel.add(paneScrollPane, BorderLayout.CENTER);
    }

    private void addStylesToDocument(StyledDocument doc) {
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");
        StyleConstants.setFontSize(def, 12);

        Style s = doc.addStyle("oldused", regular);
        StyleConstants.setItalic(s, true);
        StyleConstants.setForeground(s, Color.blue);

        s = doc.addStyle("used", regular);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, Color.blue);

        s = doc.addStyle("marked", regular);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, Color.red);

        s = doc.addStyle("small", regular);
        StyleConstants.setFontSize(s, 10);

        s = doc.addStyle("large", regular);
        StyleConstants.setFontSize(s, 16);
    }

    private void insertTextForTag() {
        String type = fieldList.getSelectedValue();
        if (type != null) {
            String txt = textPane.getSelectedText();

            if (txt != null) {
                int selStart = textPane.getSelectionStart();
                int selEnd = textPane.getSelectionEnd();

                // unselect text
                textPane.setSelectionEnd(selStart);

                // mark the selected text as "used"
                doc.setCharacterAttributes(selStart, selEnd - selStart,
                        doc.getStyle("marked"), true);

                // override an existing entry
                if (overRadio.isSelected()) {
                    entry.setField(type, txt);
                    // erase old text selection
                    marked.setStyleForTag(type, "regular", doc); // delete all previous styles
                    marked.insertPosition(type, selStart, selEnd); // insert new selection style
                }
                else {
                    // memorize the selection for text highlighting
                    marked.appendPosition(type, selStart, selEnd);

                    // get old text from bibtex tag
                    String old = entry.getField(type);

                    // merge old and selected text
                    if (old != null) {
                        // insert a new author with an additional "and"
                        if (type.hashCode() == "author".hashCode()) {
                            entry.setField(type, old + " and " + txt);
                        } else {
                            entry.setField(type, old + txt);
                        }
                    }
                    // "null"+"txt" Strings forbidden
                    else {
                        entry.setField(type, txt);
                    }
                }
                // make the new data in bibtex source code visible
                updateSourceView();
            }
        }
    }

    public boolean okPressed() {
        return okPressed;
    }

    //  ActionListener
    //  handling of buttons-click actions
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == this.okButton) {
            okPressed = true;
            dispose();
        }
        else if (source == this.cancelButton) {
            dispose();
        }
        else if (source == this.insertButton) {
            insertTextForTag();
        }
        else if (source == this.parseWithFreeCiteButton) {
            if (parseWithFreeCiteAndAddEntries()) {
                okPressed = false; // we do not want to have the super method to handle our entries, we do it on our own
                dispose();
            }
        }
    }

    /**
     * tries to parse the pasted reference with freecite
     * @return true if successful, false otherwise
     */
    private boolean parseWithFreeCiteAndAddEntries() {
        FreeCiteImporter fimp = new FreeCiteImporter();
        String text = textPane.getText();

        // we have to remove line breaks (but keep empty lines)
        // otherwise, the result is broken
        text = text.replace(Globals.NEWLINE.concat(Globals.NEWLINE), "##NEWLINE##");
        // possible URL line breaks are removed completely.
        text = text.replace("/".concat(Globals.NEWLINE), "/");
        text = text.replace(Globals.NEWLINE, " ");
        text = text.replace("##NEWLINE##", Globals.NEWLINE);

        List<BibtexEntry> importedEntries = fimp.importEntries(text, JabRef.jrf);
        if (importedEntries != null) {
            Util.setAutomaticFields(importedEntries, false, false, true);
            for (BibtexEntry e : importedEntries) {
                JabRef.jrf.basePanel().insertEntry(e);
            }
            return true;
        } else {
            return false;
        }
    }

    // update the bibtex source view and available List
    private void updateSourceView() {
        StringWriter sw = new StringWriter(200);
        try {
            new BibtexEntryWriter(new LatexFieldFormatter(), false).write(entry, sw);
            String srcString = sw.getBuffer().toString();
            preview.setText(srcString);
        } catch (IOException ignored) {
        }

        fieldList.clearSelection();
    }

    private String[] getAllFields() {
        ArrayList<String> f = new ArrayList<>();
        List<String> req = entry.getRequiredFields();
        List<String> opt = entry.getOptionalFields();
        String[] allFields = BibtexFields.getAllFieldNames();
        f.addAll(req);
        f.addAll(opt);
        for (String allField : allFields) {
            if (!f.contains(allField)) {
                f.add(allField);
            }
        }
        return f.toArray(new String[f.size()]);
    }

    class PasteAction extends BasicAction {
        public PasteAction() {
            super("Paste", "Paste from clipboard", IconTheme.JabRefIcon.PASTE.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String data = ClipBoardManager.clipBoard.getClipboardContents();
            if (data != null) {
                int selStart = textPane.getSelectionStart();
                int selEnd = textPane.getSelectionEnd();
                if (selEnd - selStart > 0) {
                    textPane.replaceSelection("");
                }
                int cPos = textPane.getCaretPosition();
                try {
                    doc.insertString(cPos, data, doc.getStyle("regular"));
                } catch (Exception ignored) {}
            }
        }
    }

    class LoadAction extends BasicAction {
        public LoadAction() {
            super("Open", "Open_file", IconTheme.JabRefIcon.OPEN.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String chosen = FileDialogs.getNewFile(_frame, null, null,
                        ".txt",
                        JFileChooser.OPEN_DIALOG, false);
                if (chosen != null) {
                    File newFile = new File(chosen);
                    doc.remove(0, doc.getLength());
                    EditorKit eKit = textPane.getEditorKit();
                    if (eKit != null) {
                        try(FileInputStream fis = new FileInputStream(newFile)) {
                            eKit.read(fis, doc, 0);
                            doc.setLogicalStyle(0, doc.getStyle("regular"));
                        }
                    }   
                }
            } catch (Exception ignored) {}
        }
    }

    class ClearAction extends BasicAction {
        public ClearAction() {
            super("Clear", "Clear_inputarea", IconTheme.JabRefIcon.NEW.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            textPane.setText("");
        }
    }

    class MenuHeaderAction extends BasicAction {
        public MenuHeaderAction() {
            super("Edit");
            this.setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {}
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
                        marked.setStyleForTag(tag1, "used", doc);
                    }

                    String tag2 = fieldList.getModel().getElementAt(index);
                    marked.setStyleForTag(tag2, "marked", doc);

                    lastIndex = index;
                }
            }
        }
    }

    // simple JList Renderer
    // based on : Advanced JList Programming at developers.sun.com
    class SimpleCellRenderer extends DefaultListCellRenderer {
        private final Font baseFont;
        private final Font usedFont;
        private final Icon okIcon = IconTheme.JabRefIcon.PLAIN_TEXT_IMPORT_DONE.getSmallIcon();
        private final Icon needIcon = IconTheme.JabRefIcon.PLAIN_TEXT_IMPORT_TODO.getSmallIcon();

        public SimpleCellRenderer(Font normFont) {
            baseFont = normFont;
            usedFont = baseFont.deriveFont(Font.ITALIC);
        }

        /* This is the only method defined by ListCellRenderer.  We just
         * reconfigure the Jlabel each time we're called.
         */
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value, // value to display
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
            //        setIcon((s.length > 10) ? longIcon : shortIcon);
            if (entry.getField(s) != null) {
                this.setForeground(Color.gray);
                this.setFont(usedFont);
                this.setIcon(okIcon);
                this.setToolTipText("filled");
            }
            else {
                this.setIcon(needIcon);
                this.setToolTipText("field is missing");
            }
            return this;
        }
    }

    private class FieldListMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                insertTextForTag();
            }
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
        super(Localization.lang(text), icon);
        putValue(Action.SHORT_DESCRIPTION, Localization.lang(description));
    }

    public BasicAction(String text, String description, URL icon, KeyStroke key) {
        super(Localization.lang(text), new ImageIcon(icon));
        putValue(Action.ACCELERATOR_KEY, key);
        putValue(Action.SHORT_DESCRIPTION, Localization.lang(description));
    }

    public BasicAction(String text) {
        super(Localization.lang(text));
    }

    public BasicAction(String text, KeyStroke key) {
        super(Localization.lang(text));
        putValue(Action.ACCELERATOR_KEY, key);
    }

    @Override
    public abstract void actionPerformed(ActionEvent e);
}
