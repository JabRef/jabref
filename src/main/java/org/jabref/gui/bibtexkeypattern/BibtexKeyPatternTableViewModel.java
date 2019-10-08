package org.jabref.gui.bibtexkeypattern;

import java.util.Collection;
import java.util.Comparator;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternTableViewModel {

    public static final String ENTRY_TYPE_DEFAULT_NAME = "default";

    public static Comparator<BibtexKeyPatternTableItemModel> defaultOnTopComparator = (o1, o2) -> {
        String itemOneName = o1.getEntryType().getName();
        String itemTwoName = o2.getEntryType().getName();

        if (itemOneName.equals(itemTwoName)) {
            return 0;
        } else if  (itemOneName.equals(ENTRY_TYPE_DEFAULT_NAME)) {
            return -1;
        } else if (itemTwoName.equals(ENTRY_TYPE_DEFAULT_NAME)) {
            return 1;
        }

        return 0;
    };

    private final ListProperty<BibtexKeyPatternTableItemModel> patternListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibtexKeyPatternTableItemModel> defaultItemProperty = new SimpleObjectProperty<>();
    private final AbstractBibtexKeyPattern initialKeyPattern;
    private final Collection<BibEntryType> bibEntryTypeList;
    private final JabRefPreferences preferences;

    public BibtexKeyPatternTableViewModel(JabRefPreferences preferences, Collection<BibEntryType> entryTypeList, AbstractBibtexKeyPattern initialKeyPattern) {
        this.preferences = preferences;
        this.bibEntryTypeList = entryTypeList;
        this.initialKeyPattern = initialKeyPattern;
    }

    public void setValues() {
        String defaultPattern;
        if ((initialKeyPattern.getDefaultValue() == null) || initialKeyPattern.getDefaultValue().isEmpty()) {
            defaultPattern = "";
        } else {
            defaultPattern = initialKeyPattern.getDefaultValue().get(0);
        }

        defaultItemProperty.setValue(new BibtexKeyPatternTableItemModel(new DefaultEntryType(), defaultPattern));
        patternListProperty.setValue(FXCollections.observableArrayList());
        patternListProperty.add(defaultItemProperty.getValue());

        bibEntryTypeList.stream()
                        .map(BibEntryType::getType)
                        .forEach(entryType -> {
                            String pattern;
                            if (initialKeyPattern.isDefaultValue(entryType)) {
                                pattern = "";
                            } else {
                                pattern = initialKeyPattern.getPatterns().get(entryType).get(0);
                            }
                            patternListProperty.add(new BibtexKeyPatternTableItemModel(entryType, pattern));
                        });
    }

    public void setItemToDefaultPattern(BibtexKeyPatternTableItemModel item) {
        item.setPattern((String) preferences.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
    }

    public void resetAll() {
        patternListProperty.forEach(item -> item.setPattern(""));
        defaultItemProperty.getValue().setPattern((String) preferences.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
    }

    public ListProperty<BibtexKeyPatternTableItemModel> patternListProperty() { return patternListProperty; }

    public ObjectProperty<BibtexKeyPatternTableItemModel> defaultKeyPatternProperty() { return defaultItemProperty; }

    public static class DefaultEntryType implements EntryType {
        @Override
        public String getName() { return ENTRY_TYPE_DEFAULT_NAME; }

        @Override
        public String getDisplayName() { return Localization.lang("Default pattern"); }
    }
}
