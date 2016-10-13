package net.sf.jabref.specialfields;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.KeywordList;


public abstract class SpecialField {

    private List<SpecialFieldValue> values;
    private KeywordList keywords;
    private HashMap<String, SpecialFieldValue> map;

    @Deprecated // create via a new constructor SpecialField(List<SpecialFieldValue> values) instead
    protected void setValues(List<SpecialFieldValue> values) {
        this.values = values;
        this.keywords = new KeywordList();
        this.map = new HashMap<>();
        for (SpecialFieldValue value : values) {
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

    public abstract String getFieldName();

    public abstract String getLocalizedFieldName();

    public String getMenuString() {
        return getLocalizedFieldName();
    }

    public String getToolTip() {
        return getLocalizedFieldName();
    }

    public boolean isSingleValueField() {
        return false;
    }

}
