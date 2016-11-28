package net.sf.jabref.model.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ContentSelectors {

    private HashMap<String, List<String>> contentSelectors;

    public ContentSelectors() {
        contentSelectors = new HashMap<>();
    }

    public void addContentSelector(String fieldName, List<String> selectors) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(selectors);

        this.contentSelectors.put(fieldName, selectors);
    }

    public List<String> getSelectorsForField(String fieldName) {
        List<String> result = contentSelectors.get(fieldName);

        if(result == null){
            result = Collections.emptyList();
        }

        return result;
    }

    public void removeSelector(String fieldName) {
        contentSelectors.remove(fieldName);
    }

    public static ContentSelectors parse(List<String> selectors) {
        //fixme: do the actual parsing

        return null;
    }

    public List<String> getAsStringList() {
        // fixme: do the actual serialization

        return null;
    }
}
