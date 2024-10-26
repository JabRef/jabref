package org.jabref.gui.commonfxcontrols;

import java.util.Collection;
import java.util.Comparator;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.filenameformatpatterns.AbstractFilenameFormatPatterns;
import org.jabref.logic.filenameformatpatterns.FilenameFormatPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

public class FilenamePatternPanelViewModel {

    public static final String ENTRY_TYPE_DEFAULT_NAME = "default";

    public static Comparator<FilenamePatternItemModel> defaultOnTopComparator = (o1, o2) -> {
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

    private final ListProperty<FilenamePatternItemModel> patternListProperty = new SimpleListProperty<>();
    private final ObjectProperty<FilenamePatternItemModel> defaultItemProperty = new SimpleObjectProperty<>();

    private final FilePreferences filePreferences;

    public FilenamePatternPanelViewModel(FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
    }

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractFilenameFormatPatterns initialKeyPattern) {
        String defaultPattern;
        if ((initialKeyPattern.getDefaultValue() == null) || initialKeyPattern.getDefaultValue().equals(FilenameFormatPattern.NULL_FileName_PATTERN)) {
            defaultPattern = "";
        } else {
            defaultPattern = initialKeyPattern.getDefaultValue().stringRepresentation();
        }

        defaultItemProperty.setValue(new FilenamePatternItemModel(new DefaultEntryType(), defaultPattern));
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
                         patternListProperty.add(new FilenamePatternItemModel(entryType, pattern));
                     });
    }

    public void setItemToDefaultPattern(FilenamePatternItemModel item) {
        item.setPattern(filePreferences.getDefaultPattern());
    }

    public void resetAll() {
        patternListProperty.forEach(item -> item.setPattern(""));
        defaultItemProperty.getValue().setPattern(filePreferences.getDefaultPattern());
    }

    public ListProperty<FilenamePatternItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<FilenamePatternItemModel> defaultKeyPatternProperty() {
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
