package net.sf.jabref.logic.specialfields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.KeywordList;
import net.sf.jabref.model.entry.SpecialFieldValue;
import net.sf.jabref.model.entry.SpecialFields;


public enum SpecialField {

    PRINTED(SpecialFields.FIELDNAME_PRINTED, "Printed", true,
        new SpecialFieldValue("printed", "togglePrinted", Localization.lang("Toggle print status"), Localization.lang("Toggle print status"))
    ),

    PRIORITY(SpecialFields.FIELDNAME_PRIORITY, "Priority", false,
        new SpecialFieldValue(null, "clearPriority", Localization.lang("Clear priority"), Localization.lang("No priority information")),
        new SpecialFieldValue("prio1", "setPriority1", Localization.lang("Set priority to high"), Localization.lang("Priority high")),
        new SpecialFieldValue("prio2", "setPriority2", Localization.lang("Set priority to medium"), Localization.lang("Priority medium")),
        new SpecialFieldValue("prio3", "setPriority3", Localization.lang("Set priority to low"), Localization.lang("Priority low"))
    ),

    QUALITY(SpecialFields.FIELDNAME_QUALITY, "Quality", true,
        new SpecialFieldValue("qualityAssured", "toggleQualityAssured", Localization.lang("Toggle quality assured"), Localization.lang("Toggle quality assured"))
    ),

    RANK(SpecialFields.FIELDNAME_RANKING, "Rank", false,
        new SpecialFieldValue(null, "clearRank", Localization.lang("Clear rank"), Localization.lang("No rank information")),
        new SpecialFieldValue("rank1", "setRank1", "", Localization.lang("One star")),
        new SpecialFieldValue("rank2", "setRank2", "", Localization.lang("Two stars")),
        new SpecialFieldValue("rank3", "setRank3", "", Localization.lang("Three stars")),
        new SpecialFieldValue("rank4", "setRank4", "", Localization.lang("Four stars")),
        new SpecialFieldValue("rank5", "setRank5", "", Localization.lang("Five stars"))
    ),

    READ_STATUS(SpecialFields.FIELDNAME_READ, "Read status", false,
        new SpecialFieldValue(null, "clearReadStatus", Localization.lang("Clear read status"), Localization.lang("No read status information")),
        new SpecialFieldValue("read", "setReadStatusToRead", Localization.lang("Set read status to read"), Localization.lang("Read status read")),
        new SpecialFieldValue("skimmed", "setReadStatusToSkimmed", Localization.lang("Set read status to skimmed"), Localization.lang("Read status skimmed"))
    ),

    RELEVANCE(SpecialFields.FIELDNAME_RELEVANCE, "Relevance", true,
        new SpecialFieldValue("relevant", "toggleRelevance", Localization.lang("Toggle relevance"), Localization.lang("Toggle relevance"))
    );

    // this is just to satisfy our own localization tests, since they will not detect if a variable is passed into Localization.lang()
    static {
        Localization.lang("Printed");
        Localization.lang("Priority");
        Localization.lang("Quality");
        Localization.lang("Rank");
        Localization.lang("Read status");
        Localization.lang("Relevance");
    }

    private List<SpecialFieldValue> values;
    private KeywordList keywords;
    private HashMap<String, SpecialFieldValue> map;
    private String fieldName;
    private String localizationKey;
    private boolean isSingleFieldValue;

    SpecialField(String fieldName, String localizationKey, boolean isSingleFieldValue, SpecialFieldValue... values) {
        this.fieldName = fieldName;
        this.localizationKey = localizationKey;
        this.isSingleFieldValue = isSingleFieldValue;
        this.values = new ArrayList<>();
        this.keywords = new KeywordList();
        this.map = new HashMap<>();
        for (SpecialFieldValue value : values) {
            this.values.add(value);
            value.getKeyword().ifPresent(keywords::add);
            value.getFieldValue().ifPresent(fieldValue -> map.put(fieldValue, value));
        }
    }

    public List<SpecialFieldValue> getValues() {
        return this.values;
    }

    public KeywordList getKeyWords() {
        return this.keywords;
    }

    public Optional<SpecialFieldValue> parse(String s) {
        return Optional.ofNullable(map.get(s));
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getLocalizedFieldName() {
        return Localization.lang(localizationKey);
    }

    public String getMenuString() {
        return getLocalizedFieldName();
    }

    public String getToolTip() {
        return getLocalizedFieldName();
    }

    public boolean isSingleValueField() {
        return isSingleFieldValue;
    }

}
