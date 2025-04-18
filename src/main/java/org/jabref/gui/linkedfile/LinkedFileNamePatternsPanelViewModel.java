package org.jabref.gui.linkedfile;

import java.util.Collection;
import java.util.Comparator;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.CitationKeyPatternsPanelItemModel;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.KeyPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.linkedfile.AbstractLinkedFileNamePatterns;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

public class LinkedFileNamePatternsPanelViewModel {
    public static final String ENTRY_TYPE_DEFAULT_NAME = "default";

    public static Comparator<CitationKeyPatternsPanelItemModel> defaultOnTopComparator = (o1, o2) -> {
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

    private final FilePreferences filePreferences;

    private final ListProperty<CitationKeyPatternsPanelItemModel> patternListProperty = new SimpleListProperty<>();
    private final ObjectProperty<CitationKeyPatternsPanelItemModel> defaultItemProperty = new SimpleObjectProperty<>();

    public LinkedFileNamePatternsPanelViewModel(FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
    }

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractLinkedFileNamePatterns initialNamePattern) {
        String defaultPattern;
        if ((initialNamePattern.getDefaultValue() == null) || initialNamePattern.getDefaultValue().equals(KeyPattern.NULL_PATTERN)) {
            defaultPattern = "";
        } else {
            defaultPattern = initialNamePattern.getDefaultValue().stringRepresentation();
        }

        defaultItemProperty.setValue(new CitationKeyPatternsPanelItemModel(new DefaultEntryType(), defaultPattern));
        patternListProperty.setValue(FXCollections.observableArrayList());
        patternListProperty.add(defaultItemProperty.getValue());

        entryTypeList.stream()
                     .map(BibEntryType::getType)
                     .forEach(entryType -> {
                         String pattern;
                         if (initialNamePattern.isDefaultValue(entryType)) {
                             pattern = "";
                         } else {
                             pattern = initialNamePattern.getPatterns().get(entryType).stringRepresentation();
                         }
                         patternListProperty.add(new CitationKeyPatternsPanelItemModel(entryType, pattern));
                     });
    }

    public void setItemToDefaultPattern(CitationKeyPatternsPanelItemModel item) {
        item.setPattern(filePreferences.getDefaultPattern());
    }

    public void resetAll() {
        patternListProperty.forEach(item -> item.setPattern(""));
        defaultItemProperty.getValue().setPattern(ENTRY_TYPE_DEFAULT_NAME);
    }

    public ListProperty<CitationKeyPatternsPanelItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<CitationKeyPatternsPanelItemModel> defaultNamePatternProperty() {
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
