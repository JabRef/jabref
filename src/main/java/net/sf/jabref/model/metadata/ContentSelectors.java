package net.sf.jabref.model.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ContentSelectors {

    private final List<ContentSelector> contentSelectors;


    public ContentSelectors() {
        contentSelectors = new ArrayList<>();
    }

    public void addContentSelector(ContentSelector contentSelector) {
        Objects.requireNonNull(contentSelector);

        this.contentSelectors.add(contentSelector);
    }

    public List<String> getSelectorValuesForField(String fieldName) {
        for(ContentSelector selector: contentSelectors) {
            if(selector.getFieldName().equals(fieldName)){
                return selector.getValues();
            }
        }

        return Collections.emptyList();
    }

    public void removeSelector(String fieldName) {
        contentSelectors.remove(fieldName);
    }

    public static ContentSelector parse(String key, String values) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(values);

        List<String> valueList = Arrays.asList(values.split(";"));

        return new ContentSelector(key, valueList);
    }

    public List<String> getAsStringList() {
        // fixme: do the actual serialization

        return null;
    }

    public Map<String, List<String>> getSelectorData() {
        Map<String, List<String>> result = new HashMap<>();

        for(ContentSelector selector: contentSelectors) {
            result.put(selector.getFieldName(), selector.getValues());
        }

        return result;
    }
}
