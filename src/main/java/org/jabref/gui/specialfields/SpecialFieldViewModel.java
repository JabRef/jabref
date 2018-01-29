package org.jabref.gui.specialfields;

import java.util.Objects;

import javax.swing.Icon;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.JabRefIcon;
import org.jabref.gui.actions.ActionsFX;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldViewModel {

    private final SpecialField field;

    public SpecialFieldViewModel(SpecialField field) {
        Objects.requireNonNull(field);
        this.field = field;
    }

    public SpecialFieldAction getSpecialFieldAction(SpecialFieldValue value, JabRefFrame frame) {
        return new SpecialFieldAction(frame, field, value.getFieldValue().orElse(null),
                // if field contains only one value, it has to be nulled
                // otherwise, another setting does not empty the field
                field.getValues().size() == 1,
                getLocalization());
    }

    public Icon getRepresentingIcon() {
        return getAction().getIcon().map(IconTheme.JabRefIcons::getSmallIcon).orElse(null);
    }

    public JabRefIcon getIcon() {
        return getAction().getIcon().orElse(null);
    }

    public String getLocalization() {
        return getAction().getText();
    }

    public ActionsFX getAction() {
        switch (field) {
            case PRINTED:
                return ActionsFX.PRINTED;
            case PRIORITY:
                return ActionsFX.PRIORITY;
            case QUALITY:
                return ActionsFX.QUALITY;
            case RANKING:
                return ActionsFX.RANKING;
            case READ_STATUS:
                return ActionsFX.READ_STATUS;
            case RELEVANCE:
                return ActionsFX.RELEVANCE;
            default:
                throw new IllegalArgumentException("There is no icon mapping for special field " + field);
        }
    }
}
