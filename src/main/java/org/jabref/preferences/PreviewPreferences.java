package org.jabref.preferences;

import java.util.List;

import org.jabref.Globals;
import org.jabref.logic.layout.LayoutFormatterPreferences;

public class PreviewPreferences {

    private final List<String> previewCycle;
    private final int previewCyclePosition;
    private final Number previewPanelDividerPosition;
    private final boolean previewPanelEnabled;
    private final String previewStyle;
    private final String previewStyleDefault;

    public PreviewPreferences(List<String> previewCycle, int previeCyclePosition, Number previewPanelDividerPosition, boolean previewPanelEnabled, String previewStyle, String previewStyleDefault) {
        this.previewCycle = previewCycle;
        this.previewCyclePosition = previeCyclePosition;
        this.previewPanelDividerPosition = previewPanelDividerPosition;
        this.previewPanelEnabled = previewPanelEnabled;
        this.previewStyle = previewStyle;
        this.previewStyleDefault = previewStyleDefault;
    }

    public List<String> getPreviewCycle() {
        return previewCycle;
    }

    public int getPreviewCyclePosition() {
        return previewCyclePosition;
    }

    public Number getPreviewPanelDividerPosition() {
        return previewPanelDividerPosition;
    }

    public boolean isPreviewPanelEnabled() {
        return previewPanelEnabled;
    }

    public String getPreviewStyle() {
        return previewStyle;
    }

    public String getPreviewStyleDefault() {
        return previewStyleDefault;
    }

    public Builder getBuilder() {
        return new Builder(this);
    }

    public String getCurrentPreviewStyle() {
        return getPreviewCycle().get(getPreviewCyclePosition());
    }

    public LayoutFormatterPreferences getLayoutFormatterPreferences() {
        return Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
    }

    public static class Builder {

        private List<String> previewCycle;
        private int previeCyclePosition;
        private Number previewPanelDividerPosition;
        private boolean previewPanelEnabled;
        private String previewStyle;
        private final String previewStyleDefault;

        public Builder(PreviewPreferences previewPreferences) {
            this.previewCycle = previewPreferences.getPreviewCycle();
            this.previeCyclePosition = previewPreferences.getPreviewCyclePosition();
            this.previewPanelDividerPosition = previewPreferences.getPreviewPanelDividerPosition();
            this.previewPanelEnabled = previewPreferences.isPreviewPanelEnabled();
            this.previewStyle = previewPreferences.getPreviewStyle();
            this.previewStyleDefault = previewPreferences.getPreviewStyleDefault();
        }

        public Builder withPreviewCycle(List<String> previewCycle) {
            this.previewCycle = previewCycle;
            return withPreviewCyclePosition(previeCyclePosition);
        }

        public Builder withPreviewCyclePosition(int position) {
            if (previewCycle.isEmpty()) {
                previeCyclePosition = 0;
            } else {
                previeCyclePosition = position;
                while (previeCyclePosition < 0) {
                    previeCyclePosition += previewCycle.size();
                }
                previeCyclePosition %= previewCycle.size();
            }
            return this;
        }

        public Builder withPreviewPanelDividerPosition(Number previewPanelDividerPosition) {
            this.previewPanelDividerPosition = previewPanelDividerPosition;
            return this;
        }

        public Builder withPreviewPanelEnabled(boolean previewPanelEnabled) {
            this.previewPanelEnabled = previewPanelEnabled;
            return this;
        }

        public Builder withPreviewStyle(String previewStyle) {
            this.previewStyle = previewStyle;
            return this;
        }

        public PreviewPreferences build() {
            return new PreviewPreferences(previewCycle, previeCyclePosition, previewPanelDividerPosition, previewPanelEnabled, previewStyle, previewStyleDefault);
        }
    }

}
