package org.jabref.gui.preferences.network;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustStoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrustStoreManager.class);
    private static final String STORE_PASSWORD = "changeit";

    private final Path storePath;

    private KeyStore store;

    public TrustStoreManager(Path storePath) {
        this.storePath = storePath;
        try {
            LOGGER.info("Trust store path: {}", storePath.toAbsolutePath());
            Path storeResourcePath = Path.of(TrustStoreManager.class.getResource("/ssl/truststore.jks").toURI());
            Files.createDirectories(storePath.getParent());
            if (Files.notExists(storePath)) {
                Files.copy(storeResourcePath, storePath);
            }
        } catch (IOException e) {
            LOGGER.warn("Bad truststore path", e);
        } catch (URISyntaxException e) {
            LOGGER.warn("Bad resource path", e);
        }

        try {
            store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(new FileInputStream(storePath.toFile()), STORE_PASSWORD.toCharArray());
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.warn("Error while loading trust store from: {}", storePath.toAbsolutePath(), e);
        }
    }

    public void addCertificate(String alias, Path certPath) {
        Objects.requireNonNull(alias);
        Objects.requireNonNull(certPath);

        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            store.setCertificateEntry(alias, certificateFactory.generateCertificate(new FileInputStream(certPath.toFile())));
            flush();
        } catch (KeyStoreException | CertificateException | IOException e) {
            LOGGER.warn("Error while adding a new certificate to the truststore: {}", alias, e);
        }
    }

    public void removeCertificate(String alias) {
        Objects.requireNonNull(alias);
        try {
            store.deleteEntry(alias);
            flush();
        } catch (KeyStoreException e) {
            LOGGER.warn("Error while deleting certificate entry with alias: {}", alias, e);
        }
    }

    public boolean isCertificateExist(String alias) {
        Objects.requireNonNull(alias);
        try {
            return store.isCertificateEntry(alias);
        } catch (KeyStoreException e) {
            LOGGER.warn("Error while checking certificate existence: {}", alias, e);
        }
        return false;
    }

    public List<String> getAliases() {
        try {
            return Collections.list(store.aliases());
        } catch (KeyStoreException e) {
            LOGGER.warn("Error while reading aliases", e);
        }
        return Collections.emptyList();
    }

    public int certsCount() {
        try {
            return store.size();
        } catch (KeyStoreException e) {
            LOGGER.warn("Can't count certificates", e);
        }
        return 0;
    }

    private void flush() {
        try {
            store.store(new FileOutputStream(storePath.toFile()), STORE_PASSWORD.toCharArray());
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            LOGGER.warn("Error while flushing trust store", e);
        }
    }
}
