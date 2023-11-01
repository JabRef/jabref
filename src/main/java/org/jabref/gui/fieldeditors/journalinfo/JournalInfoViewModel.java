package org.jabref.gui.fieldeditors.journalinfo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final ReadOnlyStringWrapper country = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper categories = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper publisher = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper scimagoId = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper hIndex = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper issn = new ReadOnlyStringWrapper();
    private final ObservableList<XYChart.Series<String, Double>> sjrData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> snipData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> citableDocsPrevious3YearsData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> citesOutgoingData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> citesOutgoingPerDocData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> citesIncomingByRecentlyPublishedData = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Series<String, Double>> docsThisYearData = FXCollections.observableArrayList();

    public void populateJournalInformation(String issn, String journalName) throws FetcherException {
        Optional<JournalInformation> journalInformationOptional = new JournalInformationFetcher().getJournalInformation(issn, journalName);

        journalInformationOptional.ifPresent(journalInformation -> {
            setTitle(journalInformation.title());
            setCountry(journalInformation.country());
            setCategories(getFormattedCategories(journalInformation));
            setPublisher(getFormattedPublisher(journalInformation));
            setScimagoId(journalInformation.scimagoId());
            sethIndex(journalInformation.hIndex());
            setIssn(journalInformation.issn());
            sjrData.add(convertToSeries(journalInformation.sjrArray()));
            snipData.add(convertToSeries(journalInformation.snipArray()));
            citableDocsPrevious3YearsData.add(convertToSeries(journalInformation.citableDocsPrevious3Years()));
            citesOutgoingData.add(convertToSeries(journalInformation.citesOutgoing()));
            citesOutgoingPerDocData.add(convertToSeries(journalInformation.citesOutgoingPerDoc()));
            citesIncomingByRecentlyPublishedData.add(convertToSeries(journalInformation.citesIncomingByRecentlyPublished()));
            docsThisYearData.add(convertToSeries(journalInformation.docsThisYear()));
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

    public String getCountry() {
        return country.get();
    }

    public ReadOnlyStringWrapper countryProperty() {
        return country;
    }

    public void setCountry(String country) {
        this.country.set(country);
    }

    public String getCategories() {
        return categories.get();
    }

    public ReadOnlyStringWrapper categoriesProperty() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories.set(categories);
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

    public String getScimagoId() {
        return scimagoId.get();
    }

    public ReadOnlyStringWrapper scimagoIdProperty() {
        return scimagoId;
    }

    public void setScimagoId(String scimagoId) {
        this.scimagoId.set(scimagoId);
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

    public ObservableList<XYChart.Series<String, Double>> getSjrData() {
        return sjrData;
    }

    public ObservableList<XYChart.Series<String, Double>> getSnipData() {
        return snipData;
    }

    public ObservableList<XYChart.Series<String, Double>> getCitableDocsPrevious3YearsData() {
        return citableDocsPrevious3YearsData;
    }

    public ObservableList<XYChart.Series<String, Double>> getCitesOutgoingData() {
        return citesOutgoingData;
    }

    public ObservableList<XYChart.Series<String, Double>> getCitesOutgoingPerDocData() {
        return citesOutgoingPerDocData;
    }

    public ObservableList<XYChart.Series<String, Double>> getCitesIncomingByRecentlyPublishedData() {
        return citesIncomingByRecentlyPublishedData;
    }

    public ObservableList<XYChart.Series<String, Double>> getDocsThisYearData() {
        return docsThisYearData;
    }

    public XYChart.Series<String, Double> convertToSeries(List<Pair<Integer, Double>> data) {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        data.stream()
            .map(pair -> new XYChart.Data<>(pair.getKey().toString(), pair.getValue()))
            .forEach(series.getData()::add);
        return series;
    }

    private static String getFormattedCategories(JournalInformation journalInformation) {
        return Arrays.stream(journalInformation.categories().split(","))
                     .map(String::trim)
                     .collect(Collectors.joining("\n"));
    }

    private static String getFormattedPublisher(JournalInformation journalInformation) {
        StringBuilder publisher = new StringBuilder();
        publisher.append(journalInformation.publisher());
        String country = journalInformation.country();
        if (!country.isBlank()) {
            publisher.append(" (").append(country).append(")");
        }
        return publisher.toString();
    }
}
