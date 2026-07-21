package org.jabref.gui.preferences.export;

import org.jabref.gui.commonfxcontrols.SaveOrderConfigPanel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.logic.l10n.Localization;

public class ExportTab extends AbstractPreferenceTabView<ExportTabViewModel> {

    public ExportTab() {
        this.viewModel = new ExportTabViewModel(preferences.getExportPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Export");
    }

    private void buildView() {
        SaveOrderConfigPanel exportOrderPanel = new SaveOrderConfigPanel();
        exportOrderPanel.saveInOriginalProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        exportOrderPanel.saveInTableOrderProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportOrderPanel.saveInSpecifiedOrderProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());
        exportOrderPanel.sortableFieldsProperty().bind(viewModel.sortableFieldsProperty());
        exportOrderPanel.sortCriteriaProperty().bindBidirectional(viewModel.sortCriteriaProperty());
        exportOrderPanel.setCriteriaLimit(3);

        getChildren().add(form()
                .section(Localization.lang("Export sort order"), order -> order
                        .custom(exportOrderPanel))
                .build());
    }
}
