package org.jabref.preferences;

import java.util.List;

import org.jabref.gui.Globals;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;

public class PreviewPreferences {

    private final List<PreviewLayout> previewCycle;
    private final int previewCyclePosition;
    private final Number previewPanelDividerPosition;
    private final String previewStyle;
    private final String previewStyleDefault;
    private final boolean showPreviewAsExtraTab;

    public PreviewPreferences(List<PreviewLayout> previewCycle, int previewCyclePosition, Number previewPanelDividerPosition, String previewStyle, String previewStyleDefault, boolean showPreviewAsExtraTab) {
        this.previewCycle = previewCycle;
        this.previewCyclePosition = previewCyclePosition;
        this.previewPanelDividerPosition = previewPanelDividerPosition;
        this.previewStyle = previewStyle;
        this.previewStyleDefault = previewStyleDefault;
        this.showPreviewAsExtraTab = showPreviewAsExtraTab;
    }

    public List<PreviewLayout> getPreviewCycle() {
        return previewCycle;
    }

    public int getPreviewCyclePosition() {
        return previewCyclePosition;
    }

    public Number getPreviewPanelDividerPosition() {
        return previewPanelDividerPosition;
    }

    public String getPreviewStyle() {
        return previewStyle;
    }

    public String getDefaultPreviewStyle() {
        return previewStyleDefault;
    }

    public Builder getBuilder() {
        return new Builder(this);
    }

    public PreviewLayout getCurrentPreviewStyle() {
        if (previewCycle.size() > 0) {
            return previewCycle.get(previewCyclePosition);
        }
        return getTextBasedPreviewLayout();
    }

    public LayoutFormatterPreferences getLayoutFormatterPreferences() {
        return Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository);
    }

    public PreviewLayout getTextBasedPreviewLayout() {
        return new TextBasedPreviewLayout(previewStyle, getLayoutFormatterPreferences());
    }

    public boolean showPreviewAsExtraTab() {
        return showPreviewAsExtraTab;
    }

    public static class Builder {

        private boolean showPreviewAsExtraTab;
        private List<PreviewLayout> previewCycle;
        private int previewCyclePosition;
        private Number previewPanelDividerPosition;
        private String previewStyle;
        private final String previewStyleDefault;

        public Builder(PreviewPreferences previewPreferences) {
            this.previewCycle = previewPreferences.getPreviewCycle();
            this.previewCyclePosition = previewPreferences.getPreviewCyclePosition();
            this.previewPanelDividerPosition = previewPreferences.getPreviewPanelDividerPosition();
            this.previewStyle = previewPreferences.getPreviewStyle();
            this.previewStyleDefault = previewPreferences.getDefaultPreviewStyle();
            this.showPreviewAsExtraTab = previewPreferences.showPreviewAsExtraTab();
        }

        public Builder withShowAsExtraTab(boolean showAsExtraTab) {
            this.showPreviewAsExtraTab = showAsExtraTab;
            return this;
        }

        public Builder withPreviewCycle(List<PreviewLayout> previewCycle) {
            this.previewCycle = previewCycle;
            return withPreviewCyclePosition(previewCyclePosition);
        }

        public Builder withPreviewCyclePosition(int position) {
            if (previewCycle.isEmpty()) {
                previewCyclePosition = 0;
            } else {
                previewCyclePosition = position;
                while (previewCyclePosition < 0) {
                    previewCyclePosition += previewCycle.size();
                }
                previewCyclePosition %= previewCycle.size();
            }
            return this;
        }

        public Builder withPreviewPanelDividerPosition(Number previewPanelDividerPosition) {
            this.previewPanelDividerPosition = previewPanelDividerPosition;
            return this;
        }

        public Builder withPreviewStyle(String previewStyle) {
            this.previewStyle = previewStyle;
            return this;
        }

        public PreviewPreferences build() {
            return new PreviewPreferences(previewCycle, previewCyclePosition, previewPanelDividerPosition, previewStyle, previewStyleDefault, showPreviewAsExtraTab);
        }
    }
}
