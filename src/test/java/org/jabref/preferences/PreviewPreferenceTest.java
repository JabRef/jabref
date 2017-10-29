package org.jabref.preferences;

import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class PreviewPreferenceTest {

    private List<String> previewCycle = null;
    private int previewCyclePosition = 0;
    private int previewPanelHeight = 0;
    private boolean previewPanelEnabled = false;
    private String previewStyle = "";
    private String previewStyleDefault = "";

    @Test
    public void givenNothingWhenCreatingPreviewPreferenceThenNothingThrown() {
        boolean nothingThrown = true;
        try {
            new PreviewPreferences(
                    previewCycle,
                    previewCyclePosition,
                    previewPanelHeight,
                    previewPanelEnabled,
                    previewStyle,
                    previewStyleDefault
            );
        } catch (Exception e) {
            nothingThrown = false;
        }
        assertTrue("Simple creation should not throw exceptions.", nothingThrown);
    }

    @Test
    public void givenNothingWhenBuildingThenNothingThrown() {
        boolean nothingThrown = true;
        try {
            PreviewPreferences previewPreferences =
                    new PreviewPreferences(
                            previewCycle,
                            previewCyclePosition,
                            previewPanelHeight,
                            previewPanelEnabled,
                            previewStyle,
                            previewStyleDefault
                    );
            new PreviewPreferences.Builder(previewPreferences);
        } catch (Exception e) {
            nothingThrown = false;
        }
        assertTrue("Simple creation should not throw exceptions.", nothingThrown);
    }
}
