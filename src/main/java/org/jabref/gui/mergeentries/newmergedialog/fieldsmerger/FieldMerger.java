package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

/**
 * This class is responsible for taking two values for some field and merging them to into one value
 * */
@FunctionalInterface
public interface FieldMerger {
    String merge(String fieldValueA, String fieldValueB);
}
