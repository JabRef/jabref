package net.sf.jabref.model.entry.specialfields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.KeywordList;

public enum SpecialField {

    PRINTED(SpecialFields.FIELDNAME_PRINTED, "Printed", true,
        SpecialFieldValue.PRINTED
    ),

    PRIORITY(SpecialFields.FIELDNAME_PRIORITY, "Priority", false,
        SpecialFieldValue.CLEAR_PRIORITY,
        SpecialFieldValue.PRIO_1,
        SpecialFieldValue.PRIO_2,
        SpecialFieldValue.PRIO_3
    ),

    QUALITY(SpecialFields.FIELDNAME_QUALITY, "Quality", true,
        SpecialFieldValue.QUALITY_ASSURED
    ),

    RANK(SpecialFields.FIELDNAME_RANKING, "Rank", false,
        SpecialFieldValue.CLEAR_RANK,
        SpecialFieldValue.RANK_1,
        SpecialFieldValue.RANK_2,
        SpecialFieldValue.RANK_3,
        SpecialFieldValue.RANK_4,
        SpecialFieldValue.RANK_5
    ),

    READ_STATUS(SpecialFields.FIELDNAME_READ, "Read status", false,
        SpecialFieldValue.CLEAR_READ_STATUS,
        SpecialFieldValue.READ,
        SpecialFieldValue.SKIMMED
    ),

    RELEVANCE(SpecialFields.FIELDNAME_RELEVANCE, "Relevance", true,
        SpecialFieldValue.RELEVANT
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
