package org.jabref.gui.preferences.network;

import java.time.LocalDate;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.net.ssl.SSLCertificate;

public class CustomCertificateViewModel extends AbstractViewModel {
    private final StringProperty serialNumberProperty = new SimpleStringProperty("");
    private final StringProperty issuerProperty = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> validFromProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> validToProperty = new SimpleObjectProperty<>();
    private final StringProperty signatureAlgorithmProperty = new SimpleStringProperty("");
    private final StringProperty versionProperty = new SimpleStringProperty("");

    public CustomCertificateViewModel(SSLCertificate sslCertificate) {
        serialNumberProperty.setValue(sslCertificate.getSerialNumber());
        issuerProperty.setValue(sslCertificate.getIssuer());
        validFromProperty.setValue(sslCertificate.getValidFrom());
        validToProperty.setValue(sslCertificate.getValidTo());
        signatureAlgorithmProperty.setValue(sslCertificate.getSignatureAlgorithm());
        versionProperty.setValue(formatVersion(sslCertificate.getVersion()));
    }

    private String formatVersion(int version) {
        return String.format("v%d", version);
    }

    public ReadOnlyStringProperty serialNumberProperty() {
        return serialNumberProperty;
    }

    public ReadOnlyStringProperty issuerProperty() {
        return issuerProperty;
    }

    public ReadOnlyObjectProperty<LocalDate> validFromProperty() {
        return validFromProperty;
    }

    public ReadOnlyObjectProperty<LocalDate> validToProperty() {
        return validToProperty;
    }

    public ReadOnlyStringProperty signatureAlgorithmProperty() {
        return signatureAlgorithmProperty;
    }

    public ReadOnlyStringProperty versionProperty() {
        return versionProperty;
    }
}
