package net.sf.jabref.gui.preftabs;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.CleanupPresetPanel;
import net.sf.jabref.logic.cleanup.CleanupPreset;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class CleanupPreferencesTab extends JPanel implements PrefsTab {

    private final CleanupPresetPanel presetPanel;
    private final JabRefPreferences preferences;

    public CleanupPreferencesTab(JabRefPreferences preferences) {
        this.presetPanel = new CleanupPresetPanel(CleanupPreset.loadQuickPresetFromPreferences(preferences));
        this.preferences = Objects.requireNonNull(preferences);

        build();
    }

    private void build() {
        FormLayout layout = new FormLayout("left:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("Quick cleanup"));
        JLabel description = new JLabel(Localization
                .lang("Choose the cleanup procedures which should be performed upon clicking the quick cleanup button."));
        builder.append(description);
        builder.append(presetPanel.getPanel());

        JPanel panel = builder.getPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        presetPanel.updateDisplay(CleanupPreset.loadQuickPresetFromPreferences(preferences));
    }

    @Override
    public void storeSettings() {
        presetPanel.getCleanupPreset().storeInPreferencesAsQuick(preferences);
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Cleanup");
    }

}
