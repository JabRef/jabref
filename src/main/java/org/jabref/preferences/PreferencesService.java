package org.jabref.preferences;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.model.metadata.FileDirectoryPreferences;

public interface PreferencesService {
    JournalAbbreviationPreferences getJournalAbbreviationPreferences();

    void storeKeyBindingRepository(KeyBindingRepository keyBindingRepository);

    KeyBindingRepository getKeyBindingRepository();

    void storeJournalAbbreviationPreferences(JournalAbbreviationPreferences abbreviationsPreferences);

    FileDirectoryPreferences getFileDirectoryPreferences();

    Path getWorkingDir();

    void setWorkingDir(Path dir);

    Map<String, List<String>> getEntryEditorTabList();

    Boolean getEnforceLegalKeys();

    Map<String, String> getCustomTabsNamesAndFields();

}
