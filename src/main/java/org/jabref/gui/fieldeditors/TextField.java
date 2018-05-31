package org.jabref.gui.fieldeditors;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.jabref.Globals;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.util.component.JTextFieldWithPlaceholder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the FieldEditor backed by a JTextField. Used for single-line input, only BibTex key at the
 * moment?!
 */
public class TextField extends JTextFieldWithPlaceholder implements FieldEditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextField.class);

    private final String fieldName;
    private UndoManager undo;

    public TextField(String fieldName, String content, boolean changeColorOnFocus) {
        this(fieldName, content, changeColorOnFocus, "");
    }

    public TextField(String fieldName, String content, boolean changeColorOnFocus, String title) {
        super(content, title);

        setupPasteListener();
        setupUndoRedo();

        updateFont();

        // Add the global focus listener, so a menu item can see if this field
        // was focused when
        // an action was called.
        addFocusListener(Globals.getFocusListener());
        if (changeColorOnFocus) {
            addFocusListener(new FieldEditorFocusListener());
        }
        this.fieldName = fieldName;
        setBackground(GUIGlobals.validFieldBackgroundColor);
        setForeground(GUIGlobals.editorTextColor);
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        if (undo != null) {
            undo.discardAllEdits();
        }
    }

    @Override
    public void append(String text) {
        setText(getText() + text);
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public JComponent getPane() {
        return this;
    }

    @Override
    public JComponent getTextComponent() {
        return this;

    }

    @Override
    public void setActiveBackgroundColor() {
        setBackgroundColor(GUIGlobals.activeBackgroundColor);
    }

    @Override
    public void setValidBackgroundColor() {
        setBackgroundColor(GUIGlobals.validFieldBackgroundColor);
    }

    @Override
    public void setInvalidBackgroundColor() {
        setBackgroundColor(GUIGlobals.invalidFieldBackgroundColor);
    }

    private void setBackgroundColor(Color color) {
        if (SwingUtilities.isEventDispatchThread()) {
            setBackground(color);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> setBackground(color));
            } catch (InvocationTargetException | InterruptedException e) {
                LOGGER.info("Problem setting background color", e);
            }
        }

    }

    private void updateFont() {
        setFont(GUIGlobals.currentFont);
    }

    @Override
    // Only replaces selected text if found
    public void paste(String textToInsert) {
        replaceSelection(textToInsert);
    }

    @Override
    public void undo() {
        // Nothing
    }

    @Override
    public void redo() {
        // Nothing
    }

    private void setupPasteListener() {
        // Bind paste command to KeyBinds.PASTE
        getInputMap().put(Globals.getKeyPrefs().getKey(org.jabref.gui.keyboard.KeyBinding.PASTE), Actions.PASTE);
    }

    private void setupUndoRedo() {
        undo = new UndoManager();
        Document doc = getDocument();

        // Listen for undo and redo events
        doc.addUndoableEditListener(evt -> undo.addEdit(evt.getEdit()));

        // Create an undo action and add it to the text component
        getActionMap().put("Undo", new AbstractAction("Undo") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException ignored) {
                    // Ignored
                }
            }
        });

        // Bind the undo action to ctl-Z
        getInputMap().put(Globals.getKeyPrefs().getKey(org.jabref.gui.keyboard.KeyBinding.UNDO), "Undo");

        // Create a redo action and add it to the text component
        getActionMap().put("Redo", new AbstractAction(Actions.REDO) {

            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException ignored) {
                    // Ignored
                }
            }
        });

        // Bind the redo action to ctl-Y
        getInputMap().put(Globals.getKeyPrefs().getKey(org.jabref.gui.keyboard.KeyBinding.REDO), "Redo");
    }
}
