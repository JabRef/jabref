package org.jabref.logic.net.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustStoreManagerTest {

    private static final String STORE_PASSWORD = "changeit";

    @Test
    void createTruststoreFileIfNotExistMergesMissingBundledCertificatesIntoExistingFile(@TempDir Path tempDir)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        Path storePath = tempDir.resolve("truststore.jks");

        KeyStore existingStore = KeyStore.getInstance(KeyStore.getDefaultType());
        existingStore.load(null, STORE_PASSWORD.toCharArray());
        try (OutputStream outputStream = Files.newOutputStream(storePath)) {
            existingStore.store(outputStream, STORE_PASSWORD.toCharArray());
        }

        TrustStoreManager.createTruststoreFileIfNotExist(storePath);

        KeyStore mergedStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream inputStream = Files.newInputStream(storePath)) {
            mergedStore.load(inputStream, STORE_PASSWORD.toCharArray());
        }

        assertTrue(mergedStore.containsAlias("haricaeccrootca2021"));
    }
}
