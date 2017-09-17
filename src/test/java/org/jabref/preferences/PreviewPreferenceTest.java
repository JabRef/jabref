package org.jabref.preferences;

import org.junit.Test;

import java.util.List;

public class PreviewPreferenceTest {

    private List<String> previewCycle = null;
    private int previewCyclePosition = 0;
    private int previewPanelHeight = 0;
    private boolean previewPanelEnabled = false;
    private String previewStyle = "";
    private String previewStyleDefault = "";

    @Test
    public void givenNothingWhenCreatingPreviewPreferenceThenNothingThrown() {
        new PreviewPreferences(
                previewCycle,
                previewCyclePosition,
                previewPanelHeight,
                previewPanelEnabled,
                previewStyle,
                previewStyleDefault
        );
    }

    @Test
    public void givenNothingWhenBuildingThenNothingThrown() {
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
    }
}
