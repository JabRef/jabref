package org.jabref.gui.fieldeditors.journalinfo;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.airhacks.afterburner.views.ViewLoader;

public class JournalInfoView extends VBox {
    @FXML private Label journalLabel;
    @FXML private LineChart<String, Double> sjrChart;
    @FXML private LineChart<String, Double> snipChart;
    private final JournalInfoViewModel viewModel;

    public JournalInfoView() {
        this.viewModel = new JournalInfoViewModel();

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.getStylesheets().add(Objects.requireNonNull(JournalInfoView.class.getResource("JournalInfo.css")).toExternalForm());

        journalLabel.textProperty().bind(viewModel.headingProperty());
        bindChartProperties();
    }

    public Parent getNode() {
        return this;
    }

    private void bindChartProperties() {
        sjrChart.setData(viewModel.getSjrData());
        snipChart.setData(viewModel.getSnipData());
    }
}
