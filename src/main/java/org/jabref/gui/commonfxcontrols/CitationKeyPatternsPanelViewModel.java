package org.jabref.gui.commonfxcontrols;

import java.util.Collection;
import java.util.Comparator;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.KeyPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

public class CitationKeyPatternsPanelViewModel {

    public static final String ENTRY_TYPE_DEFAULT_NAME = "default";

    public static Comparator<PatternsItemModel> defaultOnTopComparator = (o1, o2) -> {
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

    private final ListProperty<PatternsItemModel> patternListProperty = new SimpleListProperty<>();
    private final ObjectProperty<PatternsItemModel> defaultItemProperty = new SimpleObjectProperty<>();

    private final CitationKeyPatternPreferences keyPatternPreferences;

    public CitationKeyPatternsPanelViewModel(CitationKeyPatternPreferences keyPatternPreferences) {
        this.keyPatternPreferences = keyPatternPreferences;
    }

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractCitationKeyPatterns initialKeyPattern) {
        String defaultPattern;
        if ((initialKeyPattern.getDefaultValue() == null) || initialKeyPattern.getDefaultValue().equals(KeyPattern.NULL_PATTERN)) {
            defaultPattern = "";
        } else {
            defaultPattern = initialKeyPattern.getDefaultValue().stringRepresentation();
        }

        defaultItemProperty.setValue(new PatternsItemModel(new DefaultEntryType(), defaultPattern));
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
                         patternListProperty.add(new PatternsItemModel(entryType, pattern));
                     });
    }

    public void setItemToDefaultPattern(PatternsItemModel item) {
        item.setPattern(keyPatternPreferences.getDefaultPattern());
    }

    public void resetAll() {
        patternListProperty.forEach(item -> item.setPattern(""));
        defaultItemProperty.getValue().setPattern(keyPatternPreferences.getDefaultPattern());
    }

    public ListProperty<PatternsItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<PatternsItemModel> defaultKeyPatternProperty() {
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
