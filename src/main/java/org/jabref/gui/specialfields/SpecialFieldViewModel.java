package org.jabref.gui.specialfields;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.undo.UndoManager;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.JabRefIcon;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.specialfields.SpecialFieldsUtils;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldViewModel {

    private UndoManager undoManager;

    public SpecialFieldViewModel(SpecialField field, UndoManager undoManager) {
        this.field = Objects.requireNonNull(field);
        this.undoManager = Objects.requireNonNull(undoManager);
    }

    private final SpecialField field;

    public SpecialField getField() {
        return field;
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
                return IconTheme.JabRefIcons.PRINTED.getSmallIcon();
            case PRIORITY:
                return IconTheme.JabRefIcons.PRIORITY.getSmallIcon();
            case QUALITY:
                return IconTheme.JabRefIcons.QUALITY.getSmallIcon();
            case RANKING:
                return IconTheme.JabRefIcons.RANKING.getIcon();
            case READ_STATUS:
                return IconTheme.JabRefIcons.READ_STATUS.getSmallIcon();
            case RELEVANCE:
                return IconTheme.JabRefIcons.RELEVANCE.getSmallIcon();
            default:
                throw new IllegalArgumentException("There is no icon mapping for special field " + field);
        }
    }

    public JabRefIcon getIcon() {
        switch (field) {
            case PRINTED:
                return IconTheme.JabRefIcons.PRINTED;
            case PRIORITY:
                return IconTheme.JabRefIcons.PRIORITY;
            case QUALITY:
                return IconTheme.JabRefIcons.QUALITY;
            case RANKING:
                return IconTheme.JabRefIcons.RANKING;
            case READ_STATUS:
                return IconTheme.JabRefIcons.READ_STATUS;
            case RELEVANCE:
                return IconTheme.JabRefIcons.RELEVANCE;
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

    public JabRefIcon getEmptyIcon() {
        return getIcon();
    }

    public List<SpecialFieldValueViewModel> getValues() {
        return field.getValues().stream()
                .map(SpecialFieldValueViewModel::new)
                .collect(Collectors.toList());
    }

    public void setSpecialFieldValue(BibEntry be, SpecialFieldValue value) {
        List<FieldChange> changes = SpecialFieldsUtils.updateField(getField(), value.getFieldValue().orElse(null), be, getField().isSingleValueField(), Globals.prefs.isKeywordSyncEnabled(), Globals.prefs.getKeywordDelimiter());
        for (FieldChange change : changes) {
            undoManager.addEdit(new UndoableFieldChange(change));
        }
    }

    public void toggle(BibEntry entry) {
        setSpecialFieldValue(entry, getField().getValues().get(0));
    }
}
