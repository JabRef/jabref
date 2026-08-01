package org.jabref.logic.openoffice.style;

import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BstStyleLoaderTest {
    @Test
    void discoverInternalStylesNotEmpty() {
        OpenOfficePreferences preferences = mock(OpenOfficePreferences.class);
        when(preferences.getExternalBstStyles()).thenReturn(FXCollections.observableArrayList());

        BstStyleLoader loader = new BstStyleLoader(preferences);
        List<BstStyle> styles = loader.getStyles();

        assertNotNull(styles);
        assertFalse(styles.isEmpty());
    }
}
