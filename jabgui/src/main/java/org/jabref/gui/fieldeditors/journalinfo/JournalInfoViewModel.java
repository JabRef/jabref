package org.jabref.gui.fieldeditors.journalinfo;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.util.Pair;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.JournalInformationFetcher;
import org.jabref.logic.journals.JournalInformation;

public class JournalInfoViewModel extends AbstractViewModel {
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper publisher = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper hIndex = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper issn = new ReadOnlyStringWrapper();
    private final ObservableList<XYChart.Series<String, Double>> worksCountData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> citedByCountData = FXCollections.observableArrayList();

    public void populateJournalInformation(String issn, String journalName) throws FetcherException {
        Optional<JournalInformation> journalInformationOptional = new JournalInformationFetcher().getJournalInformation(issn, journalName);

        journalInformationOptional.ifPresent(journalInformation -> {
            setTitle(journalInformation.title());
            setPublisher(journalInformation.publisher());
            sethIndex(journalInformation.hIndex());
            setIssn(journalInformation.issn());
            worksCountData.add(convertToSeries(journalInformation.worksCount()));
            citedByCountData.add(convertToSeries(journalInformation.citedByCount()));
        });
    }

    public String getTitle() {
        return title.get();
    }

    public ReadOnlyStringWrapper titleProperty() {
        return title;
    }

    private void setTitle(String title) {
        this.title.set(title);
    }

    public String getPublisher() {
        return publisher.get();
    }

    public ReadOnlyStringWrapper publisherProperty() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher.set(publisher);
    }

    public String gethIndex() {
        return hIndex.get();
    }

    public ReadOnlyStringWrapper hIndexProperty() {
        return hIndex;
    }

    public void sethIndex(String hIndex) {
        this.hIndex.set(hIndex);
    }

    public String getIssn() {
        return issn.get();
    }

    public ReadOnlyStringWrapper issnProperty() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn.set(issn);
    }

    public ObservableList<XYChart.Series<String, Double>> getWorksCountData() {
        return worksCountData;
    }

    public ObservableList<XYChart.Series<String, Double>> getCitedByCountData() {
        return citedByCountData;
    }

    public XYChart.Series<String, Double> convertToSeries(List<Pair<Integer, Double>> data) {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        data.stream()
            .map(pair -> new XYChart.Data<>(pair.getKey().toString(), pair.getValue()))
            .forEach(series.getData()::add);
        return series;
    }
}
