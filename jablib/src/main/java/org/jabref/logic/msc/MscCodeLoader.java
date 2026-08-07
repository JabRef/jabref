package org.jabref.logic.msc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.StreamSupport;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.strings.StringUtil;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class MscCodeLoader {
    public static final String MSC_FILE_NAME = "msc_codes.mv";
    public static final String MSC_CSV_URL = "https://msc2020.org/MSC_2020.csv";

    private static final Logger LOGGER = LoggerFactory.getLogger(MscCodeLoader.class);
    private static final String MSC_CODES_MAP_NAME = "MscCodes";
    private static final CSVFormat MSC_CSV_FORMAT = CSVFormat.DEFAULT.builder()
                                                                     .setDelimiter('\t')
                                                                     .setIgnoreEmptyLines(true)
                                                                     .setQuote('"')
                                                                     .setTrim(true)
                                                                     .setHeader()
                                                                     .setSkipHeaderRecord(true)
                                                                     .get();

    private MscCodeLoader() {
    }

    public static List<MscCodeEntry> readMscCodesFromCsvUrl(URL resourceUrl) throws IOException {
        LOGGER.debug("Reading MSC codes from URL {}", resourceUrl);
        try (InputStreamReader reader = new InputStreamReader(new URLDownload(resourceUrl).asInputStream(), StandardCharsets.ISO_8859_1)) {
            List<MscCodeEntry> entries = readMscCodes(reader);
            LOGGER.debug("Loaded {} MSC codes from {}", entries.size(), resourceUrl);
            return entries;
        } catch (FetcherException e) {
            throw new IOException(e);
        }
    }

    private static List<MscCodeEntry> readMscCodes(Reader reader) throws IOException {
        try (CSVParser csvParser = MSC_CSV_FORMAT.parse(reader)) {
            return StreamSupport.stream(csvParser.spliterator(), false)
                                .map(csvRecord -> {
                                    String code = csvRecord.size() > 0 ? csvRecord.get(0) : "";
                                    String text = csvRecord.size() > 1 ? csvRecord.get(1) : "";
                                    String description = csvRecord.size() > 2 ? csvRecord.get(2) : "";
                                    return new MscCodeEntry(code, text, description);
                                })
                                .filter(entry -> !StringUtil.isBlank(entry.code()))
                                .toList();
        }
    }

    public static void convertCsvToMvStore(Path csvFile, Path mvStoreFile) throws IOException {
        LOGGER.debug("Converting MSC codes from {} to {}", csvFile, mvStoreFile);
        ensureParentDirectoryExists(mvStoreFile);
        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.ISO_8859_1)) {
            storeInMvStore(readMscCodes(reader), mvStoreFile);
        }
    }

    public static void downloadAndConvert(URL csvUrl, Path mvStoreFile) throws IOException {
        LOGGER.debug("Downloading MSC codes from {} and converting to {}", csvUrl, mvStoreFile);
        ensureParentDirectoryExists(mvStoreFile);
        storeInMvStore(readMscCodesFromCsvUrl(csvUrl), mvStoreFile);
    }

    private static void ensureParentDirectoryExists(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void storeInMvStore(List<MscCodeEntry> entries, Path mvStoreFile) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(mvStoreFile.getParent(), "msc", ".mv");
            try (MVStore store = new MVStore.Builder()
                    .fileName(tempFile.toAbsolutePath().toString())
                    .compressHigh()
                    .open()) {
                MVMap<String, MscCodeEntry> codesMap = store.openMap(MSC_CODES_MAP_NAME);
                codesMap.clear();
                for (MscCodeEntry entry : entries) {
                    codesMap.put(entry.code(), entry);
                }
                store.commit();
            }
            Files.move(tempFile, mvStoreFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.debug("Stored {} MSC codes in {}", entries.size(), mvStoreFile);
        } catch (IOException e) {
            LOGGER.error("Error writing MSC codes to MVStore: {}", mvStoreFile, e);
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ex) {
                    LOGGER.error("Could not delete temporary file: {}", tempFile, ex);
                }
            }
        }
    }

    public static boolean isMvStoreAvailableWithData(Path mvStoreFile) {
        if (!Files.exists(mvStoreFile)) {
            return false;
        }
        try (MVStore store = new MVStore.Builder().readOnly().fileName(mvStoreFile.toAbsolutePath().toString()).open()) {
            MVMap<String, MscCodeEntry> codesMap = store.openMap(MSC_CODES_MAP_NAME);
            return !codesMap.isEmpty();
        } catch (Exception e) {
            LOGGER.debug("MSC codes MVStore not available or broken: {}", mvStoreFile, e);
            return false;
        }
    }
}
