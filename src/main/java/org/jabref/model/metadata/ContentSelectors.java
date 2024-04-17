package org.jabref.model.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

public class ContentSelectors {

    private final SortedSet<ContentSelector> contentSelectors;

    public ContentSelectors() {
        contentSelectors = new TreeSet<>(new Comparator<ContentSelector>() {
            @Override
            public int compare(ContentSelector o1, ContentSelector o2) {
                // First, check the field name
                int result = o1.getField().getName().compareTo(o2.getField().getName());
                if (result != 0) {
                    return result;
                }

                // If the field names are equal, compare the properties
                // We did not find any other way to compare enum sets, so we convert them to lists and compare them using the toString method
                List<FieldProperty> properties1 = o1.getField().getProperties().stream().sorted().toList();
                List<FieldProperty> properties2 = o2.getField().getProperties().stream().sorted().toList();
                return properties1.toString().compareTo(properties2.toString());
            }
        });
    }

    public void addContentSelector(ContentSelector contentSelector) {
        Objects.requireNonNull(contentSelector);

        this.contentSelectors.add(contentSelector);
    }

    public static ContentSelector parse(Field key, String values) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(values);

        List<String> valueList = Arrays.asList(values.split(";"));

        return new ContentSelector(key, valueList);
    }

    public List<String> getSelectorValuesForField(Field field) {
        for (ContentSelector selector : contentSelectors) {
            if (selector.getField().equals(field)) {
                return selector.getValues();
            }
        }

        return Collections.emptyList();
    }

    public SortedSet<ContentSelector> getContentSelectors() {
        return contentSelectors;
    }

    public void removeSelector(Field field) {
        ContentSelector toRemove = null;

        for (ContentSelector selector : contentSelectors) {
            if (selector.getField().equals(field)) {
                toRemove = selector;
                break;
            }
        }

        if (toRemove != null) {
            contentSelectors.remove(toRemove);
        }
    }

    public List<Field> getFieldsWithSelectors() {
        List<Field> result = new ArrayList<>(contentSelectors.size());

        for (ContentSelector selector : contentSelectors) {
            result.add(selector.getField());
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContentSelectors that = (ContentSelectors) o;
        return Objects.equals(contentSelectors, that.contentSelectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentSelectors);
    }

    @Override
    public String toString() {
        return "ContentSelectors{" +
                "contentSelectors=" + contentSelectors +
                ", fieldsWithSelectors=" + getFieldsWithSelectors() +
                '}';
    }
}
