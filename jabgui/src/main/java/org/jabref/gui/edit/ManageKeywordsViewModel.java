package org.jabref.gui.edit;

import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.undo.ChangeRecorder;
import org.jabref.gui.undo.UndoManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;

import com.tobiasdiez.easybind.EasyBind;

public class ManageKeywordsViewModel {

    private final List<BibEntry> entries;
    private final KeywordList sortedKeywordsOfAllEntriesBeforeUpdateByUser = new KeywordList();
    private final BibEntryPreferences bibEntryPreferences;
    private final UndoManager undoManager;
    private final ObjectProperty<ManageKeywordsDisplayType> displayType = new SimpleObjectProperty<>(ManageKeywordsDisplayType.CONTAINED_IN_ALL_ENTRIES);
    private final ObservableList<String> keywords;

    public ManageKeywordsViewModel(BibEntryPreferences bibEntryPreferences, List<BibEntry> entries, UndoManager undoManager) {
        this.bibEntryPreferences = bibEntryPreferences;
        this.entries = entries;
        this.undoManager = undoManager;
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
        Character keywordSeparator = bibEntryPreferences.getKeywordSeparator();

        if (type == ManageKeywordsDisplayType.CONTAINED_IN_ALL_ENTRIES) {
            for (BibEntry entry : entries) {
                KeywordList separatedKeywords = entry.getKeywords(keywordSeparator);
                sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);
            }
        } else if (type == ManageKeywordsDisplayType.CONTAINED_IN_ANY_ENTRY) {
            // all keywords from first entry have to be added
            BibEntry firstEntry = entries.getFirst();
            KeywordList separatedKeywords = firstEntry.getKeywords(keywordSeparator);
            sortedKeywordsOfAllEntriesBeforeUpdateByUser.addAll(separatedKeywords);

            // for the remaining entries, intersection has to be used
            // this approach ensures that one empty keyword list leads to an empty set of common keywords
            for (BibEntry entry : entries) {
                separatedKeywords = entry.getKeywords(keywordSeparator);
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

        undoManager.record(Localization.lang("Update keywords"), recorder ->
                updateKeywords(recorder, entries, keywordsToAdd, keywordsToRemove));
    }

    private void updateKeywords(ChangeRecorder recorder, List<BibEntry> entries, KeywordList keywordsToAdd,
                                KeywordList keywordsToRemove) {
        Character keywordSeparator = bibEntryPreferences.getKeywordSeparator();

        for (BibEntry entry : entries) {
            KeywordList keywords = entry.getKeywords(keywordSeparator);

            // update keywords
            keywords.removeAll(keywordsToRemove);
            keywords.addAll(keywordsToAdd);

            // put keywords back
            recorder.record(entry.putKeywords(keywords, keywordSeparator));
        }
    }
}
