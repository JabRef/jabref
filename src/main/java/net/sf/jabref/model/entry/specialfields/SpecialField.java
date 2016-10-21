package net.sf.jabref.model.entry.specialfields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.KeywordList;

public enum SpecialField {

    PRINTED("printed", true,
        SpecialFieldValue.PRINTED
    ),

    PRIORITY("priority", false,
        SpecialFieldValue.CLEAR_PRIORITY,
        SpecialFieldValue.PRIO_1,
        SpecialFieldValue.PRIO_2,
        SpecialFieldValue.PRIO_3
    ),

    QUALITY("qualityassured", true,
        SpecialFieldValue.QUALITY_ASSURED
    ),

    RANK("ranking", false,
        SpecialFieldValue.CLEAR_RANK,
        SpecialFieldValue.RANK_1,
        SpecialFieldValue.RANK_2,
        SpecialFieldValue.RANK_3,
        SpecialFieldValue.RANK_4,
        SpecialFieldValue.RANK_5
    ),

    READ_STATUS("readstatus", false,
        SpecialFieldValue.CLEAR_READ_STATUS,
        SpecialFieldValue.READ,
        SpecialFieldValue.SKIMMED
    ),

    RELEVANCE("relevance", true,
        SpecialFieldValue.RELEVANT
    );

    private List<SpecialFieldValue> values;
    private KeywordList keywords;
    private HashMap<String, SpecialFieldValue> map;
    private String fieldName;
    private boolean isSingleFieldValue;

    SpecialField(String fieldName, boolean isSingleFieldValue, SpecialFieldValue... values) {
        this.fieldName = fieldName;
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

    public Optional<SpecialFieldValue> parse(String value) {
        return Optional.ofNullable(map.get(value));
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isSingleValueField() {
        return isSingleFieldValue;
    }

    public static Optional<SpecialField> getSpecialFieldInstanceFromFieldName(String fieldName) {
        switch (fieldName) {
            case "priority":
                return Optional.of(SpecialField.PRIORITY);
            case "qualityassured":
                return Optional.of(SpecialField.QUALITY);
            case "ranking":
                return Optional.of(SpecialField.RANK);
            case "readstatus":
                return Optional.of(SpecialField.RELEVANCE);
            case "relevance":
                return Optional.of(SpecialField.READ_STATUS);
            case "printed":
                return Optional.of(SpecialField.PRINTED);
            default:
                return Optional.empty();
        }
    }


    /**
     * @param fieldName the name of the field to check
     * @return true if given field is a special field, false otherwise
     */
    public static boolean isSpecialField(String fieldName) {
        return SpecialField.getSpecialFieldInstanceFromFieldName(fieldName).isPresent();
    }

}
