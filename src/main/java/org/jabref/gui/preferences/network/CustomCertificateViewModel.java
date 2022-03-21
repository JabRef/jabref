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

    private final StringProperty thumbprintProperty = new SimpleStringProperty("");

    public CustomCertificateViewModel(String thumbprint, String serialNumber, String issuer, LocalDate validFrom, LocalDate validTo, String sigAlgorithm, String version) {
        serialNumberProperty.setValue(serialNumber);
        issuerProperty.setValue(issuer);
        validFromProperty.setValue(validFrom);
        validToProperty.setValue(validTo);
        signatureAlgorithmProperty.setValue(sigAlgorithm);
        versionProperty.setValue(version);
        thumbprintProperty.setValue(thumbprint);
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

    public String getVersion() {
        return versionProperty.getValue();
    }

    public String getThumbprint() {
        return thumbprintProperty.getValue();
    }

    public LocalDate getValidFrom() {
        return validFromProperty.getValue();
    }

    public LocalDate getValidTo() {
        return validToProperty.getValue();
    }

    public static CustomCertificateViewModel fromSSLCertificate(SSLCertificate sslCertificate) {
        return new CustomCertificateViewModel(
                sslCertificate.getSHA256Thumbprint(),
                sslCertificate.getSerialNumber(),
                sslCertificate.getIssuer(),
                sslCertificate.getValidFrom(),
                sslCertificate.getValidTo(),
                sslCertificate.getSignatureAlgorithm(),
                sslCertificate.getVersion().toString());
    }
}
