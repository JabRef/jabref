package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.SpecialFields;

public class ReadStatus extends SpecialField {

    private static ReadStatus INSTANCE;

    private ReadStatus() {
        List<SpecialFieldValue> values = new ArrayList<>();
        values.add(new SpecialFieldValue(this, null, "clearReadStatus", Localization.lang("Clear read status"),
                Localization.lang("No read status information")));
        // DO NOT TRANSLATE "read" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "read", "setReadStatusToRead",
                Localization.lang("Set read status to read"),
                Localization.lang("Read status read")));
        values.add(new SpecialFieldValue(this, "skimmed", "setReadStatusToSkimmed",
                Localization.lang("Set read status to skimmed"),
                Localization.lang("Read status skimmed")));
        this.setValues(values);
    }

    public static ReadStatus getInstance() {
        if (ReadStatus.INSTANCE == null) {
            ReadStatus.INSTANCE = new ReadStatus();
        }
        return ReadStatus.INSTANCE;
    }

    @Override
    public String getFieldName() {
        return SpecialFields.FIELDNAME_READ;
    }

    @Override
    public String getLocalizedFieldName() {
        return Localization.lang("Read status");
    }
}
