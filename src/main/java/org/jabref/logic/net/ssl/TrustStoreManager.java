package org.jabref.logic.net.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustStoreManager.class);
    private static final String STORE_PASSWORD = "changeit";

    private final Path storePath;

    private KeyStore store;

    public TrustStoreManager(Path storePath) {
        this.storePath = storePath;
        createTruststoreFileIfNotExist(storePath);
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
        } catch (KeyStoreException | CertificateException | IOException e) {
            LOGGER.warn("Error while adding a new certificate to the truststore: {}", alias, e);
        }
    }

    public void deleteCertificate(String alias) {
        Objects.requireNonNull(alias);
        try {
            store.deleteEntry(alias);
        } catch (KeyStoreException e) {
            LOGGER.warn("Error while deleting certificate entry with alias: {}", alias, e);
        }
    }

    public boolean certificateExists(String alias) {
        Objects.requireNonNull(alias);
        try {
            return store.isCertificateEntry(alias);
        } catch (KeyStoreException e) {
            LOGGER.warn("Error while checking certificate existence: {}", alias, e);
        }
        return false;
    }

    public List<String> aliases() {
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

    public void flush() {
        try {
            store.store(Files.newOutputStream(storePath), STORE_PASSWORD.toCharArray());
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            LOGGER.warn("Error while flushing trust store", e);
        }
    }

    /**
     * Custom certificates are certificates with alias that ends with {@code [custom]}
     */
    private Boolean isCustomCertificate(String alias) {
        return alias.endsWith("[custom]");
    }

    /**
     * Deletes all custom certificates, Custom certificates are certificates with alias that ends with {@code [custom]}
     */
    public void clearCustomCertificates() {
        aliases().stream().filter(this::isCustomCertificate).forEach(this::deleteCertificate);
        flush();
    }

    public List<SSLCertificate> getCustomCertificates() {
        return aliases().stream()
                        .filter(this::isCustomCertificate)
                        .map(this::getCertificate)
                        .map(SSLCertificate::fromX509)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList());
    }

    public X509Certificate getCertificate(String alias) {
        try {
            return (X509Certificate) store.getCertificate(alias);
        } catch (KeyStoreException e) {
            LOGGER.warn("Error while getting certificate of alias: {}", alias, e);
        }
        return null;
    }

    /**
     * This method checks to see if the truststore is present in {@code storePath},
     * and if it isn't, it copies the default JDK truststore to the specified location.
     *
     * @param storePath path of the truststore
     */
    public static void createTruststoreFileIfNotExist(Path storePath) {
        try {
            LOGGER.debug("Trust store path: {}", storePath.toAbsolutePath());
            if (Files.notExists(storePath)) {
                Path storeResourcePath = Path.of(TrustStoreManager.class.getResource("/ssl/truststore.jks").toURI());
                Files.createDirectories(storePath.getParent());
                Files.copy(storeResourcePath, storePath);
            }

            try {
                configureTrustStore(storePath);
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | CertificateException e) {
                LOGGER.error("Error configuring trust store {}", storePath, e);
            }
        } catch (IOException e) {
            LOGGER.warn("Bad truststore path", e);
        } catch (URISyntaxException e) {
            LOGGER.warn("Bad resource path", e);
        }
    }

    // based on https://stackoverflow.com/a/62586564/3450689
    private static void configureTrustStore(Path myStorePath) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException,
        CertificateException, IOException {
        X509TrustManager jreTrustManager = getJreTrustManager();
        X509TrustManager myTrustManager = getJabRefTrustManager(myStorePath);

        X509TrustManager mergedTrustManager = createMergedTrustManager(jreTrustManager, myTrustManager);
        setSystemTrustManager(mergedTrustManager);
    }

    private static X509TrustManager getJreTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        return findDefaultTrustManager(null);
    }

    private static X509TrustManager getJabRefTrustManager(Path myStorePath) throws KeyStoreException, IOException,
        NoSuchAlgorithmException, CertificateException {
        // Adapt to load your keystore
        try (InputStream myKeys = Files.newInputStream(myStorePath)) {
            KeyStore myTrustStore = KeyStore.getInstance("jks");
            myTrustStore.load(myKeys, STORE_PASSWORD.toCharArray());

            return findDefaultTrustManager(myTrustStore);
        }
    }

    private static X509TrustManager findDefaultTrustManager(KeyStore keyStore)
        throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore); // If keyStore is null, tmf will be initialized with the default trust store

        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager manager) {
                return manager;
            }
        }
        return null;
    }

    private static X509TrustManager createMergedTrustManager(X509TrustManager jreTrustManager,
                                                             X509TrustManager customTrustManager) {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                // If you're planning to use client-cert auth,
                // merge results from "defaultTm" and "myTm".
                return jreTrustManager.getAcceptedIssuers();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                try {
                    customTrustManager.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    // This will throw another CertificateException if this fails too.
                    jreTrustManager.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // If you're planning to use client-cert auth,
                // do the same as checking the server.
                jreTrustManager.checkClientTrusted(chain, authType);
            }
        };
    }

    private static void setSystemTrustManager(X509TrustManager mergedTrustManager)
        throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {mergedTrustManager}, null);

        // You don't have to set this as the default context,
        // it depends on the library you're using.
        SSLContext.setDefault(sslContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }
}
