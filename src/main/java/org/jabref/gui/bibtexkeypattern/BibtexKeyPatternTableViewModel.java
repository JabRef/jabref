package org.jabref.gui.bibtexkeypattern;

import java.util.Collection;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternTableViewModel {

    private final ListProperty<BibtexKeyPatternTableItemModel> patternListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final AbstractBibtexKeyPattern keyPattern;
    private final Collection<BibEntryType> bibEntryTypeList;
    private final JabRefPreferences preferences;

    public BibtexKeyPatternTableViewModel(JabRefPreferences preferences, Collection<BibEntryType> entryTypeList, AbstractBibtexKeyPattern keyPattern) {
        this.preferences = preferences;
        this.bibEntryTypeList = entryTypeList;
        this.keyPattern = keyPattern;
    }

    public void setValues() {
        patternListProperty.clear();

        String defaultPattern = "";
        if ((keyPattern.getDefaultValue() == null) || keyPattern.getDefaultValue().isEmpty()) {
            defaultPattern = "";
        } else {
            defaultPattern = keyPattern.getDefaultValue().get(0);
        }

        patternListProperty.add(new BibtexKeyPatternTableItemModel(new EntryType() {
            @Override
            public String getName() { return "default"; }

            @Override
            public String getDisplayName() { return Localization.lang("Default Pattern"); }
        }, defaultPattern));

        bibEntryTypeList.stream()
                        .map(BibEntryType::getType)
                        .forEach(entryType -> {
                            String pattern;
                            if (keyPattern.isDefaultValue(entryType)) {
                                pattern = "";
                            } else {
                                pattern = keyPattern.getPatterns().get(entryType).get(0);
                            }
                            patternListProperty.add(new BibtexKeyPatternTableItemModel(entryType, pattern));
                        });
    }

    public AbstractBibtexKeyPattern getKeyPattern() {
        AbstractBibtexKeyPattern newKeyPattern;
        if (keyPattern instanceof GlobalBibtexKeyPattern) {
             newKeyPattern = GlobalBibtexKeyPattern.fromPattern(preferences.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
        } else {
            newKeyPattern = new DatabaseBibtexKeyPattern(preferences.getKeyPattern());
        }

        patternListProperty.forEach(item -> {
            String patternString = item.getPattern();
            if (item.getEntryType().getName().equals("default")) {
                if (!patternString.trim().isEmpty()) {
                    // we do not trim the value at the assignment to enable users to have spaces at the beginning and
                    // at the end of the pattern
                    newKeyPattern.setDefaultValue(item.getPattern());
                }
            } else {
                if (!patternString.trim().isEmpty()) {
                    newKeyPattern.addBibtexKeyPattern(item.getEntryType(), patternString);
                }
            }
        });

        return newKeyPattern;
    }

    public void setDefaultPattern(String pattern) {
        patternListProperty.stream().filter(item -> item.getEntryType().getName().equals("default")).findFirst()
                           .ifPresent(item -> item.setPattern(pattern));
    }

    public String getDefaultPattern() {
        Optional<BibtexKeyPatternTableItemModel> defaultItem;
        defaultItem = patternListProperty.stream().filter(item -> item.getEntryType().getName().equals("default"))
                                         .findFirst();

        if (defaultItem.isEmpty()) {
            String defaultPattern = "";
            if (!(keyPattern.getDefaultValue() == null || keyPattern.getDefaultValue().isEmpty())) {
                defaultPattern = keyPattern.getDefaultValue().get(0);
            }
            return defaultPattern;
        }

        return defaultItem.get().getPattern();
    }

    public void setItemToDefaultPattern(BibtexKeyPatternTableItemModel item) {
        item.setPattern((String) preferences.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
    }

    public ListProperty<BibtexKeyPatternTableItemModel> patternListProperty() { return patternListProperty; }
}
