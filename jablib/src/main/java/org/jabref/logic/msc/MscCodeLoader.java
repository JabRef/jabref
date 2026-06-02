package org.jabref.logic.msc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MscCodeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MscCodeLoader.class);
    private static final String MSC_CODES_MAP_NAME = "MscCodes";
    private static final CSVFormat MSC_CSV_FORMAT = CSVFormat.DEFAULT.builder()
                                                                     .setDelimiter('\t')
                                                                     .setIgnoreEmptyLines(true)
                                                                     .setQuote('"')
                                                                     .setTrim(true)
                                                                     .setHeader()
                                                                     .setSkipHeaderRecord(true)
                                                                     .build();

    private MscCodeLoader() {
    }

    public static List<MscCodeEntry> readMscCodesFromCsvFile(Path file) throws IOException {
        LOGGER.debug("Reading MSC codes from file {}", file);

        List<MscCodeEntry> entries = new ArrayList<>();
        try (CSVParser csvParser = CSVParser.parse(Files.newBufferedReader(file, StandardCharsets.ISO_8859_1), MSC_CSV_FORMAT)) {
            for (CSVRecord csvRecord : csvParser) {
                String code = csvRecord.size() > 0 ? csvRecord.get(0) : "";
                if (code.isBlank()) {
                    continue;
                }

                String text = csvRecord.size() > 1 ? csvRecord.get(1) : "";
                String description = csvRecord.size() > 2 ? csvRecord.get(2) : "";
                entries.add(new MscCodeEntry(code, text, description));
            }
        }

        LOGGER.debug("Loaded {} MSC codes from {}", entries.size(), file);
        return entries;
    }

    public static List<MscCodeEntry> readMscCodesFromCsvUrl(URL resourceUrl) throws IOException {
        LOGGER.debug("Reading MSC codes from URL {}", resourceUrl);

        List<MscCodeEntry> entries = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(resourceUrl.openStream(), StandardCharsets.ISO_8859_1);
             CSVParser csvParser = CSVParser.parse(reader, MSC_CSV_FORMAT)) {
            for (CSVRecord csvRecord : csvParser) {
                String code = csvRecord.size() > 0 ? csvRecord.get(0) : "";
                if (code.isBlank()) {
                    continue;
                }

                String text = csvRecord.size() > 1 ? csvRecord.get(1) : "";
                String description = csvRecord.size() > 2 ? csvRecord.get(2) : "";
                entries.add(new MscCodeEntry(code, text, description));
            }
        }

        LOGGER.debug("Loaded {} MSC codes from {}", entries.size(), resourceUrl);
        return entries;
    }

    public static void convertCsvToMvStore(Path csvFile, Path mvStoreFile) throws IOException {
        LOGGER.debug("Converting MSC codes from {} to {}", csvFile, mvStoreFile);

        Path parent = mvStoreFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.deleteIfExists(mvStoreFile);

        List<MscCodeEntry> entries = readMscCodesFromCsvFile(csvFile);

        try (MVStore store = new MVStore.Builder()
                .fileName(mvStoreFile.toAbsolutePath().toString())
                .compressHigh()
                .open()) {
            MVMap<String, MscCodeEntry> codesMap = store.openMap(MSC_CODES_MAP_NAME);
            for (MscCodeEntry entry : entries) {
                codesMap.put(entry.code(), entry);
            }
            LOGGER.debug("Stored {} MSC codes in {}", codesMap.size(), mvStoreFile);
        }
    }
}
