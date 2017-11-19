package org.jabref.gui;

import java.util.Optional;

import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateTimestampListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private BibDatabase database;

    @Before
    public void setUp(){
        database = new BibDatabase();
    }

    @Test
    public void updateTimestampEnabled(){
        final String TIMESTAMP_FIELD = "timestamp";
        final String BASE_DATE = "2000-1-1";
        final String NEW_DATE = "2000-1-2";
        final boolean INCLUDE_TIMESTAMP = true;

        JabRefPreferences preferencesMock = mock(JabRefPreferences.class);
        TimestampPreferences timestampPreferencesMock = mock(TimestampPreferences.class);

        when(preferencesMock.getTimestampPreferences()).thenReturn(timestampPreferencesMock);

        when(timestampPreferencesMock.getTimestampField()).thenReturn(TIMESTAMP_FIELD);
        when(timestampPreferencesMock.now()).thenReturn(NEW_DATE);
        when(timestampPreferencesMock.includeTimestamps()).thenReturn(INCLUDE_TIMESTAMP);

        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(TIMESTAMP_FIELD, BASE_DATE);

        database.insertEntries(bibEntry);

        assertEquals("Initial timestamp not set correctly",
                Optional.of(BASE_DATE), bibEntry.getField(TIMESTAMP_FIELD));

        database.registerListener(new UpdateTimestampListener(preferencesMock));

        bibEntry.setField("test", "some value");

        assertEquals("Timestamp not set correctly after entry changed",
                Optional.of(NEW_DATE), bibEntry.getField(TIMESTAMP_FIELD));
    }

    @Test
    public void updateTimestampDisabled(){
        final String TIMESTAMP_FIELD = "timestamp";
        final String BASE_DATE = "2000-1-1";
        final String NEW_DATE = "2000-1-2";
        final boolean INCLUDE_TIMESTAMP = false;

        JabRefPreferences preferencesMock = mock(JabRefPreferences.class);
        TimestampPreferences timestampPreferencesMock = mock(TimestampPreferences.class);

        when(preferencesMock.getTimestampPreferences()).thenReturn(timestampPreferencesMock);

        when(timestampPreferencesMock.getTimestampField()).thenReturn(TIMESTAMP_FIELD);
        when(timestampPreferencesMock.now()).thenReturn(NEW_DATE);
        when(timestampPreferencesMock.includeTimestamps()).thenReturn(INCLUDE_TIMESTAMP);

        BibEntry bibEntry = new BibEntry();

        bibEntry.setField(TIMESTAMP_FIELD, BASE_DATE);

        database.insertEntries(bibEntry);
        assertEquals("Initial timestamp not set correctly",
                Optional.of(BASE_DATE), bibEntry.getField(TIMESTAMP_FIELD));

        database.registerListener(new UpdateTimestampListener(preferencesMock));

        bibEntry.setField("test", "some value");

        assertEquals("New timestamp set after entry changed even though updates were disabled",
                Optional.of(BASE_DATE), bibEntry.getField(TIMESTAMP_FIELD));
    }
}
