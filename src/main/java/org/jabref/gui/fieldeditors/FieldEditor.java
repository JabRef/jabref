package org.jabref.gui.fieldeditors;

import java.awt.Color;

import javax.swing.JComponent;

/**
 * FieldEditors is a common interface between the TextField and TextArea.
 */
public interface FieldEditor {

    String getFieldName();

    /*
     * Returns the component to be added to a container. Might be a JScrollPane
     * or the component itself.
     */
    JComponent getPane();

    /*
     * Returns the text component itself.
     */
    Object getTextComponent();

    default boolean hasFocus() {
        if (getTextComponent() instanceof JComponent) {
            return ((JComponent) getTextComponent()).hasFocus();
        }
        return false;
    }

    void setActiveBackgroundColor();

    void setValidBackgroundColor();

    void setInvalidBackgroundColor();

    void setBackground(Color color);

    String getText();

    /**
     * Sets the given text on the current field editor and marks this text
     * editor as modified.
     *
     * @param newText
     */
    void setText(String newText);

    void append(String text);

    void requestFocus();

    void setEnabled(boolean enabled);

    /**
     * paste text into component, it should also take some selected text into
     * account
     */
    void paste(String textToInsert);

    /**
     * normally implemented in JTextArea and JTextField
     *
     * @return
     */
    String getSelectedText();

    void undo();

    void redo();
}
