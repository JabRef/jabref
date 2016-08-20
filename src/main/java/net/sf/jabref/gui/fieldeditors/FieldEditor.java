package net.sf.jabref.gui.fieldeditors;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.jabref.gui.autocompleter.AutoCompleteListener;

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
    JComponent getTextComponent();

    JLabel getLabel();

    void setActiveBackgroundColor();

    void setValidBackgroundColor();

    void setInvalidBackgroundColor();

    void setLabelColor(Color color);

    void setBackground(Color color);

    void updateFontColor();

    String getText();

    /**
     * Sets the given text on the current field editor and marks this text
     * editor as modified.
     *
     * @param newText
     */
    void setText(String newText);

    void append(String text);

    Container getParent();

    void requestFocus();

    void setEnabled(boolean enabled);

    void updateFont();

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

    void setAutoCompleteListener(AutoCompleteListener listener);

    void clearAutoCompleteSuggestion();
}
