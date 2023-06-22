package org.jabref.gui.fieldeditors.journalinfo;

import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.util.Pair;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.journals.JournalInformation;
import org.jabref.logic.journals.JournalInformationFetcher;

public class JournalInfoViewModel extends AbstractViewModel {
    private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();
    private final ObservableList<XYChart.Series<String, Double>> sjrData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> snipData = FXCollections.observableArrayList();

    public JournalInfoViewModel(String issn) {
        init(issn);
    }

    private void init(String issn) {
        JournalInformation journalInformation = new JournalInformationFetcher().getJournalInformation(issn);

        setHeading(journalInformation.title());
        sjrData.add(convertToSeries(journalInformation.sjrArray()));
        snipData.add(convertToSeries(journalInformation.snipArray()));
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

    public XYChart.Series<String, Double> convertToSeries(List<Pair<Integer, Double>> data) {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        data.stream()
            .map(pair -> new XYChart.Data<>(pair.getKey().toString(), pair.getValue()))
            .forEach(series.getData()::add);
        return series;
    }
}
