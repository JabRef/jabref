package org.jabref.gui.fieldeditors;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.gui.GUIGlobals;
import org.jabref.gui.customjfx.CustomJFXPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the FieldEditor backed by a {@link EditorTextArea}.
 * Used for multi-line input, currently all BibTexFields except Bibtex key!
 */
public class TextArea implements FieldEditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextArea.class);

    private final JFXPanel swingPanel;

    private final EditorTextArea textArea;
    private String fieldName;

    public TextArea(String fieldName, String content) {
        this(fieldName, content, "");
    }

    public TextArea(String fieldName, String content, String title) {
        textArea = new EditorTextArea(content);
        textArea.setPromptText(title);

        swingPanel = CustomJFXPanel.wrap(new Scene(textArea));
        swingPanel.setBackground(GUIGlobals.activeBackgroundColor);


        /*
        // Add the global focus listener, so a menu item can see if this field
        // was focused when an action was called.
        addFocusListener(Globals.getFocusListener());
        addFocusListener(new FieldEditorFocusListener());
        */

        this.fieldName = fieldName;

        /*
        FieldTextMenu popMenu = new FieldTextMenu(this);
        this.addMouseListener(popMenu);
        label.addMouseListener(popMenu);
        */
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String newName) {
        fieldName = newName;
    }

    @Override
    public void setBackground(Color color) {
        //not needed
    }

    @Override
    public JComponent getPane() {
        return swingPanel;
    }

    @Override
    public JComponent getTextComponent() {
        return null;
    }

    @Override
    public boolean hasFocus() {
        return false;
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

    @Override
    public String getText() {
        return textArea.getText();
    }

    @Override
    public void setText(String newText) {
        textArea.setText(newText);
    }

    @Override
    public void append(String text) {
        // not needed
    }

    @Override
    public void setEnabled(boolean enabled) {
        // not needed
    }

    @Override
    public void paste(String textToInsert) {
        /*
        replaceSelection(textToInsert);
        */
    }

    @Override
    public String getSelectedText() {
        return null;
    }

    @Override
    public void undo() {
        // Nothing
    }

    @Override
    public void redo() {
        // Nothing
    }

    @Override
    public void requestFocus() {
        // not needed
    }
}
