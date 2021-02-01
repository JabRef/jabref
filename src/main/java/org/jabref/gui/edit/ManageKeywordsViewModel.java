package org.jabref.gui.edit;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class ManageKeywordsViewModel {

    private final List<BibEntry> entries;
    private final KeywordList sortedKeywordsOfAllEntriesBeforeUpdateByUser = new KeywordList();
    private final PreferencesService preferences;
    private final ObjectProperty<ManageKeywordsDisplayType> displayType = new SimpleObjectProperty<>(ManageKeywordsDisplayType.CONTAINED_IN_ALL_ENTRIES);
    private final ObservableList<String> keywords;

    public ManageKeywordsViewModel(PreferencesService preferences, List<BibEntry> entries) {
        this.preferences = preferences;
        this.entries = entries;
        this.keywords = FXCollections.observableArrayList();

        EasyBind.subscribe(displayType, this::fillKeywordsList);
    }

    public ManageKeywordsDisplayType getDisplayType() {
        return displayType.get();
    }

    public ObjectProperty<ManageKeywordsDisplayType> displayTypeProperty() {
        return displayType;
    }

    private void fillKeywordsList(ManageKeywordsDisplayType type) {
        keywords.clear();
        sortedKeywordsOfAllEntriesBeforeUpdateByUser.clear();

        if (type == ManageKeywordsDisplayType.CONTAINED_IN_ALL_ENTRIES) {
            for (BibEntry entry : entries) {
                KeywordList separatedKeywords = entry.getKeywords(preferences.getKeywordDelimiter());
                sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);
            }
        } else if (type == ManageKeywordsDisplayType.CONTAINED_IN_ANY_ENTRY) {

            // all keywords from first entry have to be added
            BibEntry firstEntry = entries.get(0);
            KeywordList separatedKeywords = firstEntry.getKeywords(preferences.getKeywordDelimiter());
            sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);

            // for the remaining entries, intersection has to be used
            // this approach ensures that one empty keyword list leads to an empty set of common keywords
            for (BibEntry entry : entries) {
                separatedKeywords = entry.getKeywords(preferences.getKeywordDelimiter());
                sortedKeywordsOfAllEntriesBeforeUpdateByUser.retainAll(separatedKeywords);
            }
        } else {
            throw new IllegalStateException("DisplayType " + type + " not handled");
        }
        for (Keyword keyword : sortedKeywordsOfAllEntriesBeforeUpdateByUser) {
            keywords.add(keyword.get());
        }
    }

    public ObservableList<String> getKeywords() {
        return keywords;
    }

    public void removeKeyword(String keyword) {
        keywords.remove(keyword);
    }

    public void saveChanges() {
        KeywordList keywordsToAdd = new KeywordList();
        KeywordList userSelectedKeywords = new KeywordList();
        // build keywordsToAdd and userSelectedKeywords in parallel
        for (String keyword : keywords) {
            userSelectedKeywords.add(keyword);
            if (!sortedKeywordsOfAllEntriesBeforeUpdateByUser.contains(keyword)) {
                keywordsToAdd.add(keyword);
            }
        }

        KeywordList keywordsToRemove = new KeywordList();
        for (Keyword kword : sortedKeywordsOfAllEntriesBeforeUpdateByUser) {
            if (!userSelectedKeywords.contains(kword)) {
                keywordsToRemove.add(kword);
            }
        }

        if (keywordsToAdd.isEmpty() && keywordsToRemove.isEmpty()) {
            // nothing to be done if nothing is new and nothing is obsolete
            return;
        }

        NamedCompound ce = updateKeywords(entries, keywordsToAdd, keywordsToRemove);
        // TODO: bp.getUndoManager().addEdit(ce);
    }

    private NamedCompound updateKeywords(List<BibEntry> entries, KeywordList keywordsToAdd,
                                         KeywordList keywordsToRemove) {
        NamedCompound ce = new NamedCompound(Localization.lang("Update keywords"));
        for (BibEntry entry : entries) {
            KeywordList keywords = entry.getKeywords(preferences.getKeywordDelimiter());

            // update keywords
            keywords.removeAll(keywordsToRemove);
            keywords.addAll(keywordsToAdd);

            // put keywords back
            Optional<FieldChange> change = entry.putKeywords(keywords, preferences.getKeywordDelimiter());
            change.ifPresent(fieldChange -> ce.addEdit(new UndoableFieldChange(fieldChange)));
        }
        ce.end();
        return ce;
    }
}
