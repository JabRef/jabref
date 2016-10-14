package net.sf.jabref.model.entry.specialfields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.KeywordList;

public enum SpecialField {

    PRINTED(SpecialFields.FIELDNAME_PRINTED, "Printed", true,
        new SpecialFieldValue("printed", "togglePrinted")
    ),

    PRIORITY(SpecialFields.FIELDNAME_PRIORITY, "Priority", false,
        new SpecialFieldValue(null, "clearPriority"),
        new SpecialFieldValue("prio1", "setPriority1"),
        new SpecialFieldValue("prio2", "setPriority2"),
        new SpecialFieldValue("prio3", "setPriority3")
    ),

    QUALITY(SpecialFields.FIELDNAME_QUALITY, "Quality", true,
        new SpecialFieldValue("qualityAssured", "toggleQualityAssured")
    ),

    RANK(SpecialFields.FIELDNAME_RANKING, "Rank", false,
        new SpecialFieldValue(null, "clearRank"),
        new SpecialFieldValue("rank1", "setRank1"),
        new SpecialFieldValue("rank2", "setRank2"),
        new SpecialFieldValue("rank3", "setRank3"),
        new SpecialFieldValue("rank4", "setRank4"),
        new SpecialFieldValue("rank5", "setRank5")
    ),

    READ_STATUS(SpecialFields.FIELDNAME_READ, "Read status", false,
        new SpecialFieldValue(null, "clearReadStatus"),
        new SpecialFieldValue("read", "setReadStatusToRead"),
        new SpecialFieldValue("skimmed", "setReadStatusToSkimmed")
    ),

    RELEVANCE(SpecialFields.FIELDNAME_RELEVANCE, "Relevance", true,
        new SpecialFieldValue("relevant", "toggleRelevance")
    );

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

    public String getLocalizationKey() {
        return localizationKey;
    }

    public boolean isSingleValueField() {
        return isSingleFieldValue;
    }

}
