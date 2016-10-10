package net.sf.jabref.gui.fieldeditors;

import java.util.Objects;

import javax.swing.JComponent;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.model.entry.FieldName;

public class TextAreaForHiddenField extends TextArea {

    private TextAreaForVisibleField twin;
    private JComponent extra = null;


    public TextAreaForHiddenField(String fieldName) {
        super(fieldName, null);
        label.setText(" " + FieldName.getDisplayName(fieldName.substring(1)) + " ");
        label.setForeground(GUIGlobals.textAreaLabelForHiddenFieldColor);
    }

    public TextAreaForVisibleField getTwin() {
        return this.twin;
    }

    public void setTwin(TextAreaForVisibleField twin) {
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
