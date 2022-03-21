package org.jabref.logic.net.ssl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLCertificate {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSLCertificate.class);

    private final String sha256Thumbprint;
    private final String serialNumber;
    private final String issuer;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final String signatureAlgorithm;
    private final int version;

    public SSLCertificate(byte[] encoded, String serialNumber, String issuer, LocalDate validFrom, LocalDate validTo, String signatureAlgorithm, int version) {
        this.serialNumber = serialNumber;
        this.issuer = issuer;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.signatureAlgorithm = signatureAlgorithm;
        this.version = version;
        this.sha256Thumbprint = Hashing.sha256().hashBytes(encoded).toString();
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

    /**
     * @return the SHA-256 of the <a href="https://en.wikipedia.org/wiki/X.690#DER_encoding">DER encoding</a>
     *
     * */
    public String getSHA256Thumbprint() {
        return sha256Thumbprint;
    }

    public static Optional<SSLCertificate> fromX509(X509Certificate x509Certificate) {
        Objects.requireNonNull(x509Certificate);
        try {
            return Optional.of(new SSLCertificate(x509Certificate.getEncoded(), x509Certificate.getSerialNumber().toString(),
                    x509Certificate.getIssuerX500Principal().getName(),
                    LocalDate.ofInstant(x509Certificate.getNotBefore().toInstant(), ZoneId.systemDefault()),
                    LocalDate.ofInstant(x509Certificate.getNotAfter().toInstant(), ZoneId.systemDefault()),
                    x509Certificate.getSigAlgName(),
                    x509Certificate.getVersion()));
        } catch (CertificateEncodingException e) {
            LOGGER.warn("Error while encoding certificate", e);
        }
        return Optional.empty();
    }

    public static Optional<SSLCertificate> fromPath(Path certPath) {
        Objects.requireNonNull(certPath);
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            return fromX509((X509Certificate) certificateFactory.generateCertificate(new FileInputStream(certPath.toFile())));
        } catch (CertificateException e) {
            LOGGER.warn("Certificate doesn't follow X.509 format", e);
        } catch (FileNotFoundException e) {
            LOGGER.warn("Bad Certificate path: {}", certPath, e);
        }
        return Optional.empty();
    }
}
