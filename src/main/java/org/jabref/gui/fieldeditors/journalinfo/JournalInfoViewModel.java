package org.jabref.gui.fieldeditors.journalinfo;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import org.jabref.gui.AbstractViewModel;

public class JournalInfoViewModel extends AbstractViewModel {
    private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();
    private final ObservableList<XYChart.Series<String, Double>> sjrData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> snipData = FXCollections.observableArrayList();

    public JournalInfoViewModel() {
        init();
    }

    private void init() {
        setHeading("Oncotarget");
        setSjrData();
        setSnipData();
    }

    public String getHeading() {
        return heading.get();
    }

    public ReadOnlyStringWrapper headingProperty() {
        return heading;
    }

    private void setHeading(String heading) {
        this.heading.set(heading);
    }

    public ObservableList<XYChart.Series<String, Double>> getSjrData() {
        return sjrData;
    }

    public ObservableList<XYChart.Series<String, Double>> getSnipData() {
        return snipData;
    }

    public void setSjrData() {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("2013", 0.85));
        series.getData().add(new XYChart.Data<>("2014", 0.75));
        series.getData().add(new XYChart.Data<>("2015", 0.695));
        series.getData().add(new XYChart.Data<>("2016", 0.25));
        series.getData().add(new XYChart.Data<>("2017", 0.15));
        series.getData().add(new XYChart.Data<>("2018", 0.95));

        sjrData.add(series);
    }

    public void setSnipData() {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("2013", 0.85));
        series.getData().add(new XYChart.Data<>("2014", 0.75));
        series.getData().add(new XYChart.Data<>("2015", 0.695));
        series.getData().add(new XYChart.Data<>("2016", 0.25));
        series.getData().add(new XYChart.Data<>("2017", 0.15));
        series.getData().add(new XYChart.Data<>("2018", 0.95));

        snipData.add(series);
    }
}
