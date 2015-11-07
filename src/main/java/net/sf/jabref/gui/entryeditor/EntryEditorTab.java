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
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.fieldeditors.TextArea;
import net.sf.jabref.gui.fieldeditors.TextField;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.logic.autocompleter.AutoCompleter;
import net.sf.jabref.gui.fieldeditors.FileListEditor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * A single tab displayed in the EntryEditor holding several FieldEditors.
 */
class EntryEditorTab {

    private final JPanel panel = new JPanel();

    private final JScrollPane scrollPane = new JScrollPane(panel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final String[] fields;

    private final EntryEditor parent;

    private final HashMap<String, FieldEditor> editors = new HashMap<>();

    private FieldEditor activeField;

    // UGLY HACK to have a pointer to the fileListEditor to call autoSetLinks()
    public FileListEditor fileListEditor;

    private BibtexEntry entry;

    private final FocusListener fieldListener = new EntryEditorTabFocusListener(this);

    private final String tabTitle;

    public EntryEditorTab(JabRefFrame frame, BasePanel panel, List<String> fields, EntryEditor parent,
            boolean addKeyField, boolean compressed, String tabTitle) {
        if (fields != null) {
            this.fields = fields.toArray(new String[fields.size()]);
        } else {
            this.fields = new String[] {};
        }

        this.parent = parent;
        this.tabTitle = tabTitle;

        setupPanel(frame, panel, addKeyField, compressed, tabTitle);

        /*
         * The following line makes sure focus cycles inside tab instead of
         * being lost to other parts of the frame:
         */
        scrollPane.setFocusCycleRoot(true);
    }

    private void setupPanel(JabRefFrame frame, BasePanel bPanel, boolean addKeyField,
                            boolean compressed, String title) {

        InputMap inputMap = panel.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = panel.getActionMap();

        inputMap.put(Globals.prefs.getKey("Entry editor, previous entry"), "prev");
        actionMap.put("prev", parent.prevEntryAction);
        inputMap.put(Globals.prefs.getKey("Entry editor, next entry"), "next");
        actionMap.put("next", parent.nextEntryAction);

        inputMap.put(Globals.prefs.getKey("Entry editor, store field"), "store");
        actionMap.put("store", parent.storeFieldAction);
        inputMap.put(Globals.prefs.getKey("Entry editor, next panel"), "right");
        inputMap.put(Globals.prefs.getKey("Entry editor, next panel 2"), "right");
        actionMap.put("left", parent.switchLeftAction);
        inputMap.put(Globals.prefs.getKey("Entry editor, previous panel"), "left");
        inputMap.put(Globals.prefs.getKey("Entry editor, previous panel 2"), "left");
        actionMap.put("right", parent.switchRightAction);
        inputMap.put(Globals.prefs.getKey(KeyBinds.HELP), "help");
        actionMap.put("help", parent.helpAction);
        inputMap.put(Globals.prefs.getKey(KeyBinds.SAVE_DATABASE), "save");
        actionMap.put("save", parent.saveDatabaseAction);
        inputMap.put(Globals.prefs.getKey(KeyBinds.NEXT_TAB), "nexttab");
        actionMap.put("nexttab", parent.frame.nextTab);
        inputMap.put(Globals.prefs.getKey(KeyBinds.PREVIOUS_TAB), "prevtab");
        actionMap.put("prevtab", parent.frame.prevTab);

        panel.setName(title);
        // Use the title for the scrollPane, too.
        // This enables the correct execution of EntryEditor.setVisiblePanel(String name).
        scrollPane.setName(title);

        int fieldsPerRow = compressed ? 2 : 1;

        String colSpec = compressed ? "fill:pref, 1dlu, fill:10dlu:grow, 1dlu, fill:pref, "
                + "8dlu, fill:pref, 1dlu, fill:10dlu:grow, 1dlu, fill:pref"
                : "fill:pref, 1dlu, fill:pref:grow, 1dlu, fill:pref";
        StringBuilder stringBuilder = new StringBuilder();
        int rows = (int) Math.ceil((double) fields.length / fieldsPerRow);
        for (int i = 0; i < rows; i++) {
            stringBuilder.append("fill:pref:grow, ");
        }
        if (addKeyField) {
            stringBuilder.append("4dlu, fill:pref");
        } else if (stringBuilder.length() >= 2) {
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        }
        String rowSpec = stringBuilder.toString();

        DefaultFormBuilder builder = new DefaultFormBuilder
                (new FormLayout(colSpec, rowSpec), panel);

        // BibTex edit fields are defined here
        for (int i = 0; i < fields.length; i++) {
            // Create the text area:
            int editorType = BibtexFields.getEditorType(fields[i]);

            final FieldEditor fieldEditor;
            int defaultHeight;
            int wHeight = (int) (50.0 * BibtexFields.getFieldWeight(fields[i]));
            if (editorType == GUIGlobals.FILE_LIST_EDITOR) {
                fieldEditor = new FileListEditor(frame, bPanel.metaData(), fields[i], null, parent);
                fileListEditor = (FileListEditor) fieldEditor;
                defaultHeight = 0;
            } else {
                fieldEditor = new TextArea(fields[i], null);
                frame.getSearchManager().addSearchListener((TextArea) fieldEditor);
                defaultHeight = fieldEditor.getPane().getPreferredSize().height;
            }

            JComponent extra = parent.getExtra(fields[i], fieldEditor);

            // Add autocompleter listener, if required for this field:
            AutoCompleter autoCompleter = bPanel.getAutoCompleters().get(fields[i]);
            AutoCompleteListener autoCompleteListener = null;
            if (autoCompleter != null) {
                autoCompleteListener = new AutoCompleteListener(autoCompleter);
            }
            setupJTextComponent(fieldEditor.getTextComponent(), autoCompleteListener);
            fieldEditor.setAutoCompleteListener(autoCompleteListener);

            // Store the editor for later reference:
            editors.put(fields[i], fieldEditor);
            if (i == 0) {
                activeField = fieldEditor;
            }

            if (!compressed) {
                fieldEditor.getPane().setPreferredSize(new Dimension(100, Math.max(defaultHeight, wHeight)));
            }
            builder.append(fieldEditor.getLabel());
            if (extra == null) {
                builder.append(fieldEditor.getPane(), 3);
            } else {
                builder.append(fieldEditor.getPane());
                JPanel pan = new JPanel();
                pan.setLayout(new BorderLayout());
                pan.add(extra, BorderLayout.NORTH);
                builder.append(pan);
            }
            if (((i + 1) % fieldsPerRow) == 0) {
                builder.nextLine();
            }
        }

        // Add the edit field for Bibtex-key.
        if (addKeyField) {
            final TextField textField = new TextField(BibtexEntry.KEY_FIELD, parent
                    .getEntry().getField(BibtexEntry.KEY_FIELD), true);
            setupJTextComponent(textField, null);

            editors.put("bibtexkey", textField);
            /*
             * If the key field is the only field, we should have only one
             * editor, and this one should be set as active initially:
             */
            if (editors.size() == 1) {
                activeField = textField;
            }
            builder.nextLine();
            builder.append(textField.getLabel());
            builder.append(textField, 3);
        }
    }


    private BibtexEntry getEntry() {
        return entry;
    }

    private boolean isFieldModified(FieldEditor fieldEditor) {
        String text = fieldEditor.getText().trim();

        if (text.isEmpty()) {
            return getEntry().getField(fieldEditor.getFieldName()) != null;
        } else {
            Object entryValue = getEntry().getField(fieldEditor.getFieldName());
            return (entryValue == null) || !entryValue.toString().equals(text);
        }
    }

    public void markIfModified(FieldEditor fieldEditor) {
        // Only mark as changed if not already is and the field was indeed
        // modified
        if (!updating && !parent.panel.isBaseChanged() && isFieldModified(fieldEditor)) {
            markBaseChanged();
        }
    }

    private void markBaseChanged() {
        parent.panel.markBaseChanged();
    }

    /**
     * Only sets the activeField variable but does not focus it.
     * <p>
     * Call activate afterwards.
     *
     * @param fieldEditor
     */
    public void setActive(FieldEditor fieldEditor) {
        activeField = fieldEditor;
    }

    public void setActive(String fieldName) {
        if (editors.containsKey(fieldName)) {
            activeField = editors.get(fieldName);
        }
    }

    public FieldEditor getActive() {
        return activeField;
    }

    public List<String> getFields() {
        return java.util.Arrays.asList(fields);
    }

    public void activate() {
        if (activeField != null) {
            /**
             * Corrected to fix [ 1594169 ] Entry editor: navigation between panels
             */
            new FocusRequester(activeField.getTextComponent());
        }
    }

    /**
     * Reset all fields from the data in the BibtexEntry.
     */
    public void updateAll() {
        setEntry(getEntry());
    }


    private boolean updating;


    public void setEntry(BibtexEntry entry) {
        try {
            updating = true;
            for (FieldEditor editor : editors.values()) {
                Object content = entry.getField(editor.getFieldName());
                String toSet = content == null ? "" : content.toString();
                if (!toSet.equals(editor.getText())) {
                    editor.setText(toSet);
                }
            }
            this.entry = entry;
        } finally {
            updating = false;
        }
    }

    public boolean updateField(String field, String content) {
        if (!editors.containsKey(field)) {
            return false;
        }
        FieldEditor fieldEditor = editors.get(field);
        // trying to preserve current edit position (fixes SF bug #1285)
        if(fieldEditor.getTextComponent() instanceof JTextComponent) {
            int initialCaretPosition = ((JTextComponent) fieldEditor).getCaretPosition();
            fieldEditor.setText(content);
            int textLength = fieldEditor.getText().length();
            if(initialCaretPosition<textLength) {
                ((JTextComponent) fieldEditor).setCaretPosition(initialCaretPosition);
            } else {
                ((JTextComponent) fieldEditor).setCaretPosition(textLength);
            }
        } else {
            fieldEditor.setText(content);
        }
        return true;
    }

    public void validateAllFields() {
        for (Map.Entry<String, FieldEditor> stringFieldEditorEntry : editors.entrySet()) {
            FieldEditor ed = stringFieldEditorEntry.getValue();
            ed.updateFontColor();
            ed.setEnabled(true);
            if (((Component) ed).hasFocus()) {
                ed.setActiveBackgroundColor();
            } else {
                ed.setValidBackgroundColor();
            }
        }
    }

    public void setEnabled(boolean enabled) {
        for (FieldEditor editor : editors.values()) {
            editor.setEnabled(enabled);
        }
    }

    public Component getPane() {
        return scrollPane;
    }

    public EntryEditor getParent() {
        return parent;
    }

    public String getTabTitle() {
        return tabTitle;
    }

    /**
     * Set up key bindings and focus listener for the FieldEditor.
     *
     * @param component
     */
    private void setupJTextComponent(final JComponent component, final AutoCompleteListener autoCompleteListener) {

        // Here we add focus listeners to the component. The funny code is because we need
        // to guarantee that the AutoCompleteListener - if used - is called before fieldListener
        // on a focus lost event. The AutoCompleteListener is responsible for removing any
        // current suggestion when focus is lost, and this must be done before fieldListener
        // stores the current edit. Swing doesn't guarantee the order of execution of event
        // listeners, so we handle this by only adding the AutoCompleteListener and telling
        // it to call fieldListener afterwards. If no AutoCompleteListener is used, we
        // add the fieldListener normally.
        if (autoCompleteListener != null) {
            component.addKeyListener(autoCompleteListener);
            component.addFocusListener(autoCompleteListener);
            autoCompleteListener.setNextFocusListener(fieldListener);
        } else {
            component.addFocusListener(fieldListener);
        }

        InputMap inputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(Globals.prefs.getKey("Entry editor, previous entry"), "prev");
        actionMap.put("prev", parent.prevEntryAction);
        inputMap.put(Globals.prefs.getKey("Entry editor, next entry"), "next");
        actionMap.put("next", parent.nextEntryAction);

        inputMap.put(Globals.prefs.getKey("Entry editor, store field"), "store");
        actionMap.put("store", parent.storeFieldAction);
        inputMap.put(Globals.prefs.getKey("Entry editor, next panel"), "right");
        inputMap.put(Globals.prefs.getKey("Entry editor, next panel 2"), "right");
        actionMap.put("left", parent.switchLeftAction);
        inputMap.put(Globals.prefs.getKey("Entry editor, previous panel"), "left");
        inputMap.put(Globals.prefs.getKey("Entry editor, previous panel 2"), "left");
        actionMap.put("right", parent.switchRightAction);
        inputMap.put(Globals.prefs.getKey(KeyBinds.HELP), "help");
        actionMap.put("help", parent.helpAction);
        inputMap.put(Globals.prefs.getKey(KeyBinds.SAVE_DATABASE), "save");
        actionMap.put("save", parent.saveDatabaseAction);
        inputMap.put(Globals.prefs.getKey(KeyBinds.NEXT_TAB), "nexttab");
        actionMap.put("nexttab", parent.frame.nextTab);
        inputMap.put(Globals.prefs.getKey(KeyBinds.PREVIOUS_TAB), "prevtab");
        actionMap.put("prevtab", parent.frame.prevTab);


        HashSet<AWTKeyStroke> keys = new HashSet<>(component
                .getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.clear();
        keys.add(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
        keys = new HashSet<>(component
                .getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.clear();
        keys.add(KeyStroke.getKeyStroke("shift pressed TAB"));
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

    }

}
