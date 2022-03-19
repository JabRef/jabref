package org.jabref.logic.net.ssl;

import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

public class SSLCertificate {
    private final String serialNumber;
    private final String issuer;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final String signatureAlgorithm;
    private final int version;

    public SSLCertificate(String serialNumber, String issuer, LocalDate validFrom, LocalDate validTo, String signatureAlgorithm, int version) {
        this.serialNumber = serialNumber;
        this.issuer = issuer;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.signatureAlgorithm = signatureAlgorithm;
        this.version = version;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getIssuer() {
        return issuer;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public int getVersion() {
        return version;
    }

    public static SSLCertificate from(X509Certificate x509Certificate) {
        Objects.requireNonNull(x509Certificate);

        return new SSLCertificate(x509Certificate.getSerialNumber().toString(),
                                  x509Certificate.getIssuerX500Principal().getName(),
                                  LocalDate.ofInstant(x509Certificate.getNotBefore().toInstant(), ZoneId.systemDefault()),
                                  LocalDate.ofInstant(x509Certificate.getNotAfter().toInstant(), ZoneId.systemDefault()),
                                  x509Certificate.getSigAlgName(),
                                  x509Certificate.getVersion());
    }
}
