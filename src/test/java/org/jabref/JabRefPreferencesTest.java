package org.jabref;

import java.nio.charset.StandardCharsets;

import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JabRefPreferencesTest {

    private JabRefPreferences prefs;

    @BeforeEach
    void setUp() {
        prefs = mock(JabRefPreferences.class);
        GeneralPreferences generalPreferences = mock(GeneralPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        when(prefs.getGeneralPreferences()).thenReturn(generalPreferences);
    }

    // FIXME: This test is not testing anything. prefs.getDefaultEncoding() will always return StandardCharsets.UTF_8
    //  because of setUp(): when(prefs.getDefaultEnconding()).thenReturn(StandardCharsets.UTF_8);
    @Test
    void getDefaultEncodingReturnsPreviouslyStoredEncoding() {
        // prefs.setDefaultEncoding(StandardCharsets.UTF_8);
        // assertEquals(StandardCharsets.UTF_8, prefs.getDefaultEncoding());
    }
}
