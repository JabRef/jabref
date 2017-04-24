package org.jabref.gui.specialfields;

import java.util.Objects;

import javax.swing.Icon;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
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
        switch (field) {
            case PRINTED:
                return IconTheme.JabRefIcon.PRINTED.getSmallIcon();
            case PRIORITY:
                return IconTheme.JabRefIcon.PRIORITY.getSmallIcon();
            case QUALITY:
                return IconTheme.JabRefIcon.QUALITY.getSmallIcon();
            case RANKING:
                return IconTheme.JabRefIcon.RANKING.getIcon();
            case READ_STATUS:
                return IconTheme.JabRefIcon.READ_STATUS.getSmallIcon();
            case RELEVANCE:
                return IconTheme.JabRefIcon.RELEVANCE.getSmallIcon();
            default:
                throw new IllegalArgumentException("There is no icon mapping for special field " + field);
        }
    }

    public String getLocalization() {
        switch (field) {
            case PRINTED:
                return Localization.lang("Printed");
            case PRIORITY:
                return Localization.lang("Priority");
            case QUALITY:
                return Localization.lang("Quality");
            case RANKING:
                return Localization.lang("Rank");
            case READ_STATUS:
                return Localization.lang("Read status");
            case RELEVANCE:
                return Localization.lang("Relevance");
            default:
                throw new IllegalArgumentException("There is no icon mapping for special field " + field);
        }
    }

}
