package org.jabref.gui.fieldeditors.journalinfo;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.logic.importer.FetcherException;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalInfoView extends VBox {
    @FXML private Label title;
    @FXML private Label categories;
    @FXML private Label publisher;
    @FXML private Label hIndex;
    @FXML private Label issn;
    @FXML private LineChart<String, Double> sjrChart;
    @FXML private LineChart<String, Double> citableDocsPrevious3YearsChart;
    @FXML private LineChart<String, Double> citesOutgoingChart;
    @FXML private LineChart<String, Double> citesOutgoingPerDocChart;
    @FXML private LineChart<String, Double> citesIncomingByRecentlyPublishedChart;
    @FXML private LineChart<String, Double> docsThisYearChart;
    private final JournalInfoViewModel viewModel;

    public JournalInfoView() {
        this.viewModel = new JournalInfoViewModel();

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.getStylesheets().add(Objects.requireNonNull(JournalInfoView.class.getResource("JournalInfo.css")).toExternalForm());

        title.textProperty().bind(viewModel.titleProperty());
        categories.textProperty().bind(viewModel.categoriesProperty());
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
        sjrChart.setData(viewModel.getSjrData());
        citableDocsPrevious3YearsChart.setData(viewModel.getCitableDocsPrevious3YearsData());
        citesOutgoingChart.setData(viewModel.getCitesOutgoingData());
        citesOutgoingPerDocChart.setData(viewModel.getCitesOutgoingPerDocData());
        citesIncomingByRecentlyPublishedChart.setData(viewModel.getCitesIncomingByRecentlyPublishedData());
        docsThisYearChart.setData(viewModel.getDocsThisYearData());
    }
}
