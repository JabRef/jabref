package net.sf.jabref.gui.fieldeditors;

import java.util.Objects;

/**
 * Indicates that the field of this TextArea can be hidden
 */
public class TextAreaForVisibleField extends TextArea {

    private TextAreaForHiddenField twin;


    public TextAreaForVisibleField(String fieldName) {
        super(fieldName, null);
    }

    public TextAreaForHiddenField getTwin() {
        return this.twin;
    }

    public void setTwin(TextAreaForHiddenField twin) {
        this.twin = Objects.requireNonNull(twin);
    }

}
