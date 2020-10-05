package org.jabref.preferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class OpenOfficeSyncWhenCitingTest {

    private static boolean previousValue;
    private final JabRefPreferences preferences = JabRefPreferences.getInstance();

    @BeforeAll
    public static void savePreferenceSyncWhenCiting() {
        previousValue = JabRefPreferences.getInstance().getBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING);
    }

    @AfterAll
    public static void restorePreferenceSyncWhenCiting() {
        JabRefPreferences.getInstance().putBoolean(JabRefPreferences.OO_SYNC_WHEN_CITING, previousValue);
    }

    @Test
    public void testDefaultSyncWhenCiting() {
        OpenOfficePreferences prefs = preferences.getOpenOfficePreferences();
        assertTrue(prefs.getSyncWhenCiting());
    }

    @Test
    public void testChangedSyncWhenCiting() {
        OpenOfficePreferences prefs = preferences.getOpenOfficePreferences();
        prefs.setSyncWhenCiting(false);
        assertFalse(prefs.getSyncWhenCiting());
    }

}
