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

    private final BibtexKeyPatternTableItemModel defaultItem = new BibtexKeyPatternTableItemModel(new EntryType() {
        @Override
        public String getName() { return "default"; }

        @Override
        public String getDisplayName() { return Localization.lang("Default Pattern"); }
    }, "");

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

        defaultItem.setPattern(defaultPattern);
        patternListProperty.add(defaultItem);

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
            if (!item.getEntryType().getName().equals("default")) {
                if (!patternString.trim().isEmpty()) {
                    newKeyPattern.addBibtexKeyPattern(item.getEntryType(), patternString);
                }
            }
        });

        if (!defaultItem.getPattern().trim().isEmpty()) {
            // we do not trim the value at the assignment to enable users to have spaces at the beginning and
            // at the end of the pattern
            newKeyPattern.setDefaultValue(defaultItem.getPattern());
        }

        return newKeyPattern;
    }

    public void setDefaultPattern(String pattern) { defaultItem.setPattern(pattern); }

    public String getDefaultPattern() { return defaultItem.getPattern(); }

    public void setItemToDefaultPattern(BibtexKeyPatternTableItemModel item) {
        item.setPattern((String) preferences.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
    }

    public void resetAll() {
        patternListProperty.forEach(item -> item.setPattern(""));
        defaultItem.setPattern((String) preferences.defaults.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
    }

    public ListProperty<BibtexKeyPatternTableItemModel> patternListProperty() { return patternListProperty; }
}
