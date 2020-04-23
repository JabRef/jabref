package org.jabref.gui;

import java.util.Optional;

import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateTimestampListenerTest {

    private BibDatabase database;
    private BibEntry bibEntry;

    private JabRefPreferences preferencesMock;
    private TimestampPreferences timestampPreferencesMock;

    @BeforeEach
    void setUp() {
        database = new BibDatabase();
        bibEntry = new BibEntry();

        database.insertEntry(bibEntry);

        preferencesMock = mock(JabRefPreferences.class);
        timestampPreferencesMock = mock(TimestampPreferences.class);

        when(preferencesMock.getTimestampPreferences()).thenReturn(timestampPreferencesMock);
    }

    @Test
    void updateTimestampEnabled() {
        final Field timestampField = StandardField.TIMESTAMP;
        final String baseDate = "2000-1-1";
        final String newDate = "2000-1-2";

        final boolean includeTimestamp = true;

        when(timestampPreferencesMock.getTimestampField()).thenReturn(timestampField);
        when(timestampPreferencesMock.now()).thenReturn(newDate);
        when(timestampPreferencesMock.includeTimestamps()).thenReturn(includeTimestamp);

        bibEntry.setField(timestampField, baseDate);

        assertEquals(Optional.of(baseDate), bibEntry.getField(timestampField), "Initial timestamp not set correctly");

        database.registerListener(new UpdateTimestampListener(preferencesMock));

        bibEntry.setField(new UnknownField("test"), "some value");

        assertEquals(Optional.of(newDate), bibEntry.getField(timestampField), "Timestamp not set correctly after entry changed");
    }

    @Test
    void updateTimestampDisabled() {
        final Field timestampField = StandardField.TIMESTAMP;
        final String baseDate = "2000-1-1";
        final String newDate = "2000-1-2";

        final boolean includeTimestamp = false;

        when(timestampPreferencesMock.getTimestampField()).thenReturn(timestampField);
        when(timestampPreferencesMock.now()).thenReturn(newDate);
        when(timestampPreferencesMock.includeTimestamps()).thenReturn(includeTimestamp);

        bibEntry.setField(timestampField, baseDate);

        assertEquals(Optional.of(baseDate), bibEntry.getField(timestampField), "Initial timestamp not set correctly");

        database.registerListener(new UpdateTimestampListener(preferencesMock));

        bibEntry.setField(new UnknownField("test"), "some value");

        assertEquals(Optional.of(baseDate), bibEntry.getField(timestampField), "New timestamp set after entry changed even though updates were disabled");
    }
}
