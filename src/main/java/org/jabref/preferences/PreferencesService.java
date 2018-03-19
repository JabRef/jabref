package org.jabref.preferences;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.metadata.FileDirectoryPreferences;

public interface PreferencesService {
    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository);

    KeyBindingRepository getKeyBindingRepository();

    void storeJournalAbbreviationPreferences(JournalAbbreviationPreferences abbreviationsPreferences);

    FileDirectoryPreferences getFileDirectoryPreferences();

    String get(String key);

    void put(String key, String value);
}
