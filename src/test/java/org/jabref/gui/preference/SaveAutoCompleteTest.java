package org.jabref.gui.preference;


import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.JabRefPreferences;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;


public class SaveAutoCompleteTest {

    @Test
    public void storeAutoCompletePreferencesTest() throws NoSuchFieldException, IllegalAccessException {
        JabRefPreferences preferencesService = JabRefPreferences.getInstance();
        preferencesService.storeAutoCompletePreferences(new AutoCompletePreferences(
                true,
                AutoCompleteFirstNameMode.BOTH,
                AutoCompletePreferences.NameFormat.BOTH,
                FieldFactory.parseFieldList(""),
                preferencesService.getJournalAbbreviationPreferences()));

        Field field = preferencesService.getClass().getDeclaredField("AUTO_COMPLETE");
        field.setAccessible(true);
        assertEquals(preferencesService.getBoolean((String) field.get(preferencesService)), true);
    }
}
