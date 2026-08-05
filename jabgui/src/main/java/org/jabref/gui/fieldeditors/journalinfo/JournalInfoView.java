package org.jabref.gui.fieldeditors.journalinfo;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalInfoView extends VBox {
    @FXML private Label title;
    @FXML private Label publisher;
    @FXML private Label hIndex;
    @FXML private Label issn;
    @FXML private LineChart<String, Double> worksCountChart;
    @FXML private LineChart<String, Double> citedByCountChart;
    private final JournalInfoViewModel viewModel;

    public JournalInfoView(ImporterPreferences importerPreferences) {
        this.viewModel = new JournalInfoViewModel(importerPreferences);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        title.textProperty().bind(viewModel.titleProperty());
        publisher.textProperty().bind(viewModel.publisherProperty());
        hIndex.textProperty().bind(viewModel.hIndexProperty());
        this.issn.textProperty().bind(viewModel.issnProperty());
        bindChartProperties();
    }

    public Node populateJournalInformation(String issn, String journalName) throws FetcherException {
        viewModel.populateJournalInformation(issn, journalName);
        return this;
    }

    public Node getNode() {
        return this;
    }

    private void bindChartProperties() {
        worksCountChart.setData(viewModel.getWorksCountData());
        citedByCountChart.setData(viewModel.getCitedByCountData());
    }
}
