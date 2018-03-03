package org.jabref;

import java.nio.charset.StandardCharsets;

import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JabRefPreferencesTest {

    private JabRefPreferences prefs;

    @BeforeEach
    public void setUp() {
        prefs = mock(JabRefPreferences.class);
        when(prefs.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
    }

    @Test
    public void getDefaultEncodingReturnsPreviouslyStoredEncoding() {
        prefs.setDefaultEncoding(StandardCharsets.UTF_8);
        assertEquals(StandardCharsets.UTF_8, prefs.getDefaultEncoding());
    }

}
