package org.jabref.preferences;

import java.util.List;

public class PreviewPreferences {

    private final List<String> previewCycle;
    private final int previewCyclePosition;
    private final int previewPanelHeight;
    private final boolean previewPanelEnabled;
    private final String previewStyle;
    private final String previewStyleDefault;


    public PreviewPreferences(List<String> previewCycle, int previewCyclePosition, int previewPanelHeight, boolean previewPanelEnabled, String previewStyle, String previewStyleDefault) {
        this.previewCycle = previewCycle;
        this.previewCyclePosition = previewCyclePosition;
        this.previewPanelHeight = previewPanelHeight;
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

    public int getPreviewPanelHeight() {
        return previewPanelHeight;
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

    public static class Builder {
        private List<String> previewCycle;
        private int previewCyclePosition;
        private int previewPanelHeight;
        private boolean previewPanelEnabled;
        private String previewStyle;
        private final String previewStyleDefault;


        public Builder(PreviewPreferences previewPreferences) {
            this.previewCycle = previewPreferences.getPreviewCycle();
            this.previewCyclePosition = previewPreferences.getPreviewCyclePosition();
            this.previewPanelHeight = previewPreferences.getPreviewPanelHeight();
            this.previewPanelEnabled = previewPreferences.isPreviewPanelEnabled();
            this.previewStyle = previewPreferences.getPreviewStyle();
            this.previewStyleDefault = previewPreferences.getPreviewStyleDefault();
        }

        public Builder withPreviewCycle(List<String> previewCycle) {
            this.previewCycle = previewCycle;
            return withPreviewCyclePosition(previewCyclePosition);
        }

        public Builder withPreviewCyclePosition(int position) {
            previewCyclePosition = position;
            while (previewCyclePosition < 0) {
                previewCyclePosition += previewCycle.size();
            }
            previewCyclePosition %= previewCycle.size();
            return this;
        }

        public Builder withPreviewPanelHeight(int previewPanelHeight) {
            this.previewPanelHeight = previewPanelHeight;
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
            return new PreviewPreferences(previewCycle, previewCyclePosition, previewPanelHeight, previewPanelEnabled, previewStyle, previewStyleDefault);
        }
    }

}
