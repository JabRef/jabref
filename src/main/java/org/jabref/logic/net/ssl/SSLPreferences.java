package org.jabref.logic.net.ssl;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SSLPreferences {
    private final BooleanProperty useCustomCertificates;
    private final ObservableList<String> customCertificateThumbprint;
    private final ObservableList<String> customCertificateVersion;
    private final ObservableList<String> customCertificateValidFrom;
    private final ObservableList<String> customCertificateValidTo;

    public SSLPreferences(Boolean useCustomCertificates, List<String> customCertificateThumbprint, List<String> customCertificateVersion,
                          List<String> customCertificateValidFrom, List<String> customCertificateValidTo) {
        this.useCustomCertificates = new SimpleBooleanProperty(useCustomCertificates);
        this.customCertificateThumbprint = FXCollections.observableList(customCertificateThumbprint);
        this.customCertificateVersion = FXCollections.observableList(customCertificateVersion);
        this.customCertificateValidFrom = FXCollections.observableList(customCertificateValidFrom);
        this.customCertificateValidTo = FXCollections.observableList(customCertificateValidTo);
    }

    public boolean shouldUseCustomCertificates() {
        return useCustomCertificates.getValue();
    }

    public BooleanProperty useCustomCertificatesProperty() {
        return useCustomCertificates;
    }

    public void setUseCustomCertificates(boolean useCustomCertificates) {
        this.useCustomCertificates.set(useCustomCertificates);
    }

    public ObservableList<String> getCustomCertificateThumbprint() {
        return customCertificateThumbprint;
    }

    public ObservableList<String> getCustomCertificateVersion() {
        return customCertificateVersion;
    }

    public ObservableList<String> getCustomCertificateValidFrom() {
        return customCertificateValidFrom;
    }

    public ObservableList<String> getCustomCertificateValidTo() {
        return customCertificateValidTo;
    }

    public void setCustomCertificateThumbprint(List<String> list) {
        customCertificateThumbprint.clear();
        customCertificateThumbprint.addAll(list);
    }

    public void setCustomCertificateVersion(List<String> list) {
        customCertificateVersion.clear();
        customCertificateVersion.addAll(list);
    }

    public void setCustomCertificateValidFrom(List<String> list) {
        customCertificateValidFrom.clear();
        customCertificateValidFrom.addAll(list);
    }

    public void setCustomCertificateValidTo(List<String> list) {
        customCertificateValidTo.clear();
        customCertificateValidTo.addAll(list);
    }
}
