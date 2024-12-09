package org.jabref.logic.pseudonymization;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jooq.lambda.Unchecked;

public class PseudonymizationResultCsvWriter {

    public static void writeValuesMappingAsCsv(Path path, Pseudonymization.Result result) throws IOException {
        try (
                OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)
        ) {
            csvPrinter.printRecord("pseudonymized", "original value");
            // The valueMapping is a map of pseudonymized value to original value
            // The pseudonymized value is the key, the original value is the value
            result.valueMapping().entrySet().stream()
                  // We have date-1, date-2, ..., date-10, date-11. That should be sorted accordingly.
                  // In case a "dumb" string ordering is used, we would get date-1, date-10, date-11, date-2, ...
                  .sorted(Comparator.comparing((Map.Entry<String, String> entry) -> getKeyPrefix(entry.getKey())
                  ).thenComparingInt(entry -> extractNumber(entry.getKey())))
                  .forEach(Unchecked.consumer(entry -> {
                      csvPrinter.printRecord(entry.getKey(), entry.getValue());
                  }));
        }
    }

    private static String getKeyPrefix(String key) {
        int dashIndex = key.lastIndexOf('-');
        return dashIndex != -1 ? key.substring(0, dashIndex) : key;
    }

    private static int extractNumber(String key) {
        try {
            return Integer.parseInt(key.substring(key.lastIndexOf('-') + 1));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
