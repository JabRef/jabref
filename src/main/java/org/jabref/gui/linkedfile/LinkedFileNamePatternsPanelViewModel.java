package org.jabref.gui.linkedfile;

import java.util.Collection;
import java.util.Comparator;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.linkedfile.AbstractLinkedFileNamePatterns;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

public class LinkedFileNamePatternsPanelViewModel {
    public static final String ENTRY_TYPE_DEFAULT_NAME = "default";

    public static Comparator<LinkedFileNamePatternsItemModel> defaultOnTopComparator = (o1, o2) -> {
        String itemOneName = o1.getEntryType().getName();
        String itemTwoName = o2.getEntryType().getName();

        if (itemOneName.equals(itemTwoName)) {
            return 0;
        } else if (ENTRY_TYPE_DEFAULT_NAME.equals(itemOneName)) {
            return -1;
        } else if (ENTRY_TYPE_DEFAULT_NAME.equals(itemTwoName)) {
            return 1;
        }

        return 0;
    };

    private final ListProperty<LinkedFileNamePatternsItemModel> patternListProperty = new SimpleListProperty<>();
    private final ObjectProperty<LinkedFileNamePatternsItemModel> defaultItemProperty = new SimpleObjectProperty<>();

    public LinkedFileNamePatternsPanelViewModel() { }

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractLinkedFileNamePatterns initialKeyPattern) {
        String defaultPattern;
        if ((initialKeyPattern.getDefaultValue() == null) || initialKeyPattern.getDefaultValue().equals(CitationKeyPattern.NULL_CITATION_KEY_PATTERN)) {
            defaultPattern = "";
        } else {
            defaultPattern = initialKeyPattern.getDefaultValue().stringRepresentation();
        }

         // defaultItemProperty.setValue(new LinkedFileNamePatternsItemModel(new LinkedFileNamePatternPanelViewModel().DefaultEntryType(), defaultPattern));
         defaultItemProperty.setValue(new LinkedFileNamePatternsItemModel(new DefaultEntryType(), defaultPattern));
        patternListProperty.setValue(FXCollections.observableArrayList());
        patternListProperty.add(defaultItemProperty.getValue());

        entryTypeList.stream()
                     .map(BibEntryType::getType)
                     .forEach(entryType -> {
                         String pattern;
                         if (initialKeyPattern.isDefaultValue(entryType)) {
                             pattern = "";
                         } else {
                             pattern = initialKeyPattern.getPatterns().get(entryType).stringRepresentation();
                         }
                         patternListProperty.add(new LinkedFileNamePatternsItemModel(entryType, pattern));
                     });
    }

    public void setItemToDefaultPattern(LinkedFileNamePatternsItemModel item) {
        item.setPattern("Default Pattern");
    }

    public void resetAll() {
        patternListProperty.forEach(item -> item.setPattern(""));
        defaultItemProperty.getValue().setPattern(ENTRY_TYPE_DEFAULT_NAME);
    }

    public ListProperty<LinkedFileNamePatternsItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<LinkedFileNamePatternsItemModel> defaultKeyPatternProperty() {
        return defaultItemProperty;
    }

    public static class DefaultEntryType implements EntryType {
        @Override
        public String getName() {
            return ENTRY_TYPE_DEFAULT_NAME;
        }

        @Override
        public String getDisplayName() {
            return Localization.lang("Default pattern");
        }
    }
}
