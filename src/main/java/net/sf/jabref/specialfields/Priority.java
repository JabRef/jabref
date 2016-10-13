package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.SpecialFields;

public class Priority extends SpecialField {

    private static Priority INSTANCE;


    private Priority() {
        List<SpecialFieldValue> values = new ArrayList<>();
        values.add(new SpecialFieldValue(this, null, "clearPriority", Localization.lang("Clear priority"),
                Localization.lang("No priority information")));
        values.add(new SpecialFieldValue(this, "prio1", "setPriority1", Localization.lang("Set priority to high"),
                Localization.lang("Priority high")));
        values.add(new SpecialFieldValue(this, "prio2", "setPriority2", Localization.lang("Set priority to medium"),
                Localization.lang("Priority medium")));
        values.add(new SpecialFieldValue(this, "prio3", "setPriority3", Localization.lang("Set priority to low"),
                 Localization.lang("Priority low")));
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
}
