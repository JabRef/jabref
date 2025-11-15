package org.jabref.gui.mergeentries.threewaymerge.fieldsmerger;

/// This class is responsible for taking two values for some field and merging them into one value
@FunctionalInterface
public interface FieldMerger {
    String merge(String fieldValueA, String fieldValueB);
}
