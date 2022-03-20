package org.jabref.logic.net.ssl;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SSLPreferences {
    private final BooleanProperty useCustomCertificates;
    private final ObservableList<String> customCertificateAlias;
    private final ObservableList<String> customCertificateVersion;

    public SSLPreferences(Boolean useCustomCertificates, List<String> customCertificateAlias, List<String> customCertificateVersion) {
        this.useCustomCertificates = new SimpleBooleanProperty(useCustomCertificates);
        this.customCertificateAlias = FXCollections.observableList(customCertificateAlias);
        this.customCertificateVersion = FXCollections.observableList(customCertificateVersion);
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

    public ObservableList<String> getCustomCertificateAlias() {
        return customCertificateAlias;
    }

    public ObservableList<String> getCustomCertificateVersion() {
        return customCertificateVersion;
    }

    public void setCustomCertificateAlias(List<String> list) {
        customCertificateAlias.clear();
        customCertificateAlias.addAll(list);
    }

    public void setCustomCertificateVersion(List<String> list) {
        customCertificateVersion.clear();
        customCertificateVersion.addAll(list);
    }
}
