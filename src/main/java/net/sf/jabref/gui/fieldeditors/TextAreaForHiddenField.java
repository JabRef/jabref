package net.sf.jabref.gui.fieldeditors;

import java.util.Objects;

import net.sf.jabref.gui.GUIGlobals;

public class TextAreaForHiddenField extends TextArea {

    private TextAreaForVisibleField twin;


    public TextAreaForHiddenField(String fieldName) {
        super(fieldName, null);
        label.setText(" " + fieldName.substring(1));
        label.setForeground(GUIGlobals.textAreaLabelForHiddenFieldColor);
    }

    public TextAreaForVisibleField getTwin() {
        return this.twin;
    }

    public void setTwin(TextAreaForVisibleField twin) {
        this.twin = Objects.requireNonNull(twin);
    }

}
