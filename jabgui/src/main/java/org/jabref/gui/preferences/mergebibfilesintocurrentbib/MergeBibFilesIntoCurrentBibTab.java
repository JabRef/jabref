package org.jabref.gui.preferences.mergebibfilesintocurrentbib;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class MergeBibFilesIntoCurrentBibTab extends AbstractPreferenceTabView<MergeBibFilesIntoCurrentBibTabViewModel> implements PreferencesTab {

    @FXML private CheckBox mergeSameKeyEntries;
    @FXML private CheckBox mergeDuplicateEntries;

    public MergeBibFilesIntoCurrentBibTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new MergeBibFilesIntoCurrentBibTabViewModel(preferences);

        mergeSameKeyEntries.selectedProperty().bindBidirectional(viewModel.mergeSameKeyEntriesProperty());
        mergeDuplicateEntries.selectedProperty().bindBidirectional(viewModel.mergeDuplicateEntriesProperty());
    }

    @Override
    public String getTabName() {
        return Localization.lang("Merge BibTeX files into current library");
    }
}
