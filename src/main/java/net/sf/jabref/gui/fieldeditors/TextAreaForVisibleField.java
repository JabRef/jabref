package net.sf.jabref.gui.fieldeditors;

import java.util.Objects;

import javax.swing.JComponent;

/**
 * Indicates that the field of this TextArea can be hidden
 */
public class TextAreaForVisibleField extends TextArea {

    private TextAreaForHiddenField twin;
    private JComponent extra = null;


    public TextAreaForVisibleField(String fieldName) {
        super(fieldName, null);
    }

    public TextAreaForHiddenField getTwin() {
        return this.twin;
    }

    public void setTwin(TextAreaForHiddenField twin) {
        this.twin = Objects.requireNonNull(twin);
    }

    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (extra != null) {
            extra.setVisible(isVisible);
        }
    }

    public void setExtra(JComponent extra) {
        this.extra = extra;
    }
}
