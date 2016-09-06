package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.SpecialFields;

public class Priority extends SpecialField {

    private static Priority INSTANCE;

    private final Icon icon = IconTheme.JabRefIcon.PRIORITY.getSmallIcon();


    private Priority() {
        List<SpecialFieldValue> values = new ArrayList<>();
        values.add(new SpecialFieldValue(this, null, "clearPriority", Localization.lang("Clear priority"), null,
                Localization.lang("No priority information")));
        Icon tmpicon;
        tmpicon = IconTheme.JabRefIcon.PRIORITY_HIGH.getSmallIcon();
        // DO NOT TRANSLATE "prio1" etc. as this makes the .bib files non portable
        values.add(new SpecialFieldValue(this, "prio1", "setPriority1", Localization.lang("Set priority to high"),
                tmpicon, Localization.lang("Priority high")));
        tmpicon = IconTheme.JabRefIcon.PRIORITY_MEDIUM.getSmallIcon();
        values.add(new SpecialFieldValue(this, "prio2", "setPriority2", Localization.lang("Set priority to medium"),
                tmpicon, Localization.lang("Priority medium")));
        tmpicon = IconTheme.JabRefIcon.PRIORITY_LOW.getSmallIcon();
        values.add(new SpecialFieldValue(this, "prio3", "setPriority3", Localization.lang("Set priority to low"),
                tmpicon, Localization.lang("Priority low")));
        this.setValues(values);
    }

    public static Priority getInstance() {
        if (Priority.INSTANCE == null) {
            Priority.INSTANCE = new Priority();
        }
        return Priority.INSTANCE;
    }

    @Override
    public String getFieldName() {
        return SpecialFields.FIELDNAME_PRIORITY;
    }

    @Override public String getLocalizedFieldName() {
        return Localization.lang("Priority");
    }

    @Override
    public Icon getRepresentingIcon() {
        return this.icon;
    }
}
