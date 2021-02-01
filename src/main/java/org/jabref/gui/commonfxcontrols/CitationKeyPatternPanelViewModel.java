package org.jabref.gui.commonfxcontrols;

import java.util.Collection;
import java.util.Comparator;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.PreferencesService;

public class CitationKeyPatternPanelViewModel {

    public static final String ENTRY_TYPE_DEFAULT_NAME = "default";

    public static Comparator<CitationKeyPatternPanelItemModel> defaultOnTopComparator = (o1, o2) -> {
        String itemOneName = o1.getEntryType().getName();
        String itemTwoName = o2.getEntryType().getName();

        if (itemOneName.equals(itemTwoName)) {
            return 0;
        } else if (itemOneName.equals(ENTRY_TYPE_DEFAULT_NAME)) {
            return -1;
        } else if (itemTwoName.equals(ENTRY_TYPE_DEFAULT_NAME)) {
            return 1;
        }

        return 0;
    };

    private final ListProperty<CitationKeyPatternPanelItemModel> patternListProperty = new SimpleListProperty<>();
    private final ObjectProperty<CitationKeyPatternPanelItemModel> defaultItemProperty = new SimpleObjectProperty<>();

    private final PreferencesService preferences;

    public CitationKeyPatternPanelViewModel(PreferencesService preferences) {
        this.preferences = preferences;
    }

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractCitationKeyPattern initialKeyPattern) {
        String defaultPattern;
        if ((initialKeyPattern.getDefaultValue() == null) || initialKeyPattern.getDefaultValue().isEmpty()) {
            defaultPattern = "";
        } else {
            defaultPattern = initialKeyPattern.getDefaultValue().get(0);
        }

        defaultItemProperty.setValue(new CitationKeyPatternPanelItemModel(new DefaultEntryType(), defaultPattern));
        patternListProperty.setValue(FXCollections.observableArrayList());
        patternListProperty.add(defaultItemProperty.getValue());

        entryTypeList.stream()
                     .map(BibEntryType::getType)
                     .forEach(entryType -> {
                         String pattern;
                         if (initialKeyPattern.isDefaultValue(entryType)) {
                             pattern = "";
                         } else {
                             pattern = initialKeyPattern.getPatterns().get(entryType).get(0);
                         }
                         patternListProperty.add(new CitationKeyPatternPanelItemModel(entryType, pattern));
                     });
    }

    public void setItemToDefaultPattern(CitationKeyPatternPanelItemModel item) {
        item.setPattern(preferences.getDefaultsDefaultCitationKeyPattern());
    }

    public void resetAll() {
        patternListProperty.forEach(item -> item.setPattern(""));
        defaultItemProperty.getValue().setPattern(preferences.getDefaultsDefaultCitationKeyPattern());
    }

    public ListProperty<CitationKeyPatternPanelItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<CitationKeyPatternPanelItemModel> defaultKeyPatternProperty() {
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
