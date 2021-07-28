package org.jabref.gui;

import java.util.Optional;

import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateTimestampListenerTest {

    private BibDatabase database;
    private BibEntry bibEntry;

    private PreferencesService preferencesMock;
    private TimestampPreferences timestampPreferencesMock;

    private final String baseDate = "2000-1-1";
    private final String newDate = "2000-1-2";

    @BeforeEach
    void setUp() {
        database = new BibDatabase();
        bibEntry = new BibEntry();

        database.insertEntry(bibEntry);

        preferencesMock = mock(PreferencesService.class);
        timestampPreferencesMock = mock(TimestampPreferences.class);

        when(preferencesMock.getTimestampPreferences()).thenReturn(timestampPreferencesMock);
    }

    @Test
    void updateTimestampEnabled() {
        final boolean includeTimestamp = true;

        when(timestampPreferencesMock.now()).thenReturn(newDate);
        when(timestampPreferencesMock.shouldAddModificationDate()).thenReturn(includeTimestamp);

        bibEntry.setField(StandardField.MODIFICATIONDATE, baseDate);

        assertEquals(Optional.of(baseDate), bibEntry.getField(StandardField.MODIFICATIONDATE), "Initial timestamp not set correctly");

        database.registerListener(new UpdateTimestampListener(preferencesMock));

        bibEntry.setField(new UnknownField("test"), "some value");

        assertEquals(Optional.of(newDate), bibEntry.getField(StandardField.MODIFICATIONDATE), "Timestamp not set correctly after entry changed");
    }

    @Test
    void updateTimestampDisabled() {
        final boolean includeTimestamp = false;

        when(timestampPreferencesMock.now()).thenReturn(newDate);
        when(timestampPreferencesMock.shouldAddModificationDate()).thenReturn(includeTimestamp);

        bibEntry.setField(StandardField.MODIFICATIONDATE, baseDate);

        assertEquals(Optional.of(baseDate), bibEntry.getField(StandardField.MODIFICATIONDATE), "Initial timestamp not set correctly");

        database.registerListener(new UpdateTimestampListener(preferencesMock));

        bibEntry.setField(new UnknownField("test"), "some value");

        assertEquals(Optional.of(baseDate), bibEntry.getField(StandardField.MODIFICATIONDATE), "New timestamp set after entry changed even though updates were disabled");
    }
}
