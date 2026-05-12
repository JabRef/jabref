package org.jabref.logic.ai.summarization.migration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.ai.summarization.repositories.MVStoreSummariesRepository;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Tests that {@link SummariesMigrationV1} correctly migrates every v1 summaries MVStore
/// found under {@code src/test/resources/…/summarization/migration/} into the v2 repository.
///
/// Entries (database path + citation key) are discovered dynamically from the MVStore,
/// so adding {@code summariesN.mv} alongside its matching {@code summariesN.json} is enough
/// to add a new test case without touching this class.
///
/// The fixed library ID acts as the new identifier created during v1 → v2 migration.
class SummariesMigrationV1Test {

    private static final String TEST_RESOURCES = "src/test/resources/org/jabref/logic/ai/summarization/migrations";
    private static final String LIBRARY_ID = "00000000-0000-0000-0000-000000000001";
    private static final String SUMMARIES_MAP_PREFIX = "summaries-";

    @TempDir
    Path tempDir;

    static List<String> mvStoreFiles() throws IOException {
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(Path.of(TEST_RESOURCES))) {
            for (Path p : stream.toList()) {
                if (p.getFileName().toString().endsWith(".mv")) {
                    names.add(p.getFileName().toString());
                }
            }
        }
        return names;
    }

    @ParameterizedTest
    @MethodSource("mvStoreFiles")
    void allEntriesAreMigratedCorrectly(String fileName) throws Exception {
        Path oldFilePath = copyResource(fileName);

        List<DiscoveredEntry> entries = discoverEntries(oldFilePath);
        assertFalse(entries.isEmpty(), "Test resource " + fileName + " contains no summaries");

        boolean onWindows = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows");
        assumeTrue(onWindows || !hasWindowsAbsolutePaths(entries),
                "Skipping " + fileName + ": contains Windows-style paths, not runnable on this OS");

        String jsonName = fileName.replace(".mv", ".json");
        URL jsonUrl = Path.of(TEST_RESOURCES, jsonName).toUri().toURL();

        JsonSummariesFile jsonFile = JsonSummariesFile.parse(jsonUrl);

        for (Path dbPath : entries.stream().map(DiscoveredEntry::dbPath).distinct().toList()) {
            MVStoreSummariesRepository repo = new MVStoreSummariesRepository(
                    _ -> {
                    },
                    tempDir.resolve("v2-" + dbPath.getFileName() + "-" + fileName)
            );

            SummariesMigrationV1.migrate(oldFilePath, LIBRARY_ID, dbPath, repo, _ -> {
            });

            for (DiscoveredEntry entry : entries.stream().filter(e -> e.dbPath().equals(dbPath)).toList()) {
                AiSummaryIdentifier id = new AiSummaryIdentifier(LIBRARY_ID, entry.citationKey());
                Optional<AiSummary> migrated = repo.get(id);

                assertFalse(migrated.isEmpty(),
                        "Summary not found for " + dbPath + " / " + entry.citationKey());

                JsonSummariesFile.JsonSummary expected = jsonFile.summaryFor(dbPath, entry.citationKey());
                Objects.requireNonNull(expected, "JSON entry not found for " + entry.citationKey());

                assertEquals(expected.aiProvider(), migrated.get().metadata().aiProvider(),
                        "Provider mismatch for " + entry.citationKey());
                assertEquals(expected.model(), migrated.get().metadata().model(),
                        "Model mismatch for " + entry.citationKey());
                assertEquals(expected.content(), migrated.get().content(),
                        "Content mismatch for " + entry.citationKey());

                Instant expectedTimestamp = entry.oldSummary().timestamp()
                                                 .atZone(ZoneId.systemDefault()).toInstant();
                assertEquals(expectedTimestamp, migrated.get().metadata().timestamp(),
                        "Timestamp mismatch for " + entry.citationKey());
            }
        }
    }

    private Path copyResource(String fileName) throws IOException {
        Path source = Path.of(TEST_RESOURCES, fileName);
        Path dest = tempDir.resolve(fileName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    private static boolean hasWindowsAbsolutePaths(List<DiscoveredEntry> entries) {
        return entries.stream().map(DiscoveredEntry::dbPath).anyMatch(p -> {
            String raw = p.toString();
            return raw.length() >= 3
                    && Character.isLetter(raw.charAt(0))
                    && raw.charAt(1) == ':'
                    && (raw.charAt(2) == '\\' || raw.charAt(2) == '/');
        });
    }

    private List<DiscoveredEntry> discoverEntries(Path oldFilePath) {
        List<DiscoveredEntry> result = new ArrayList<>();
        try (MVStore store = new MVStore.Builder()
                .fileName(oldFilePath.toString())
                .readOnly()
                .open()) {
            for (String mapName : store.getMapNames()) {
                if (mapName.startsWith(SUMMARIES_MAP_PREFIX)) {
                    Path dbPath = Path.of(mapName.substring(SUMMARIES_MAP_PREFIX.length()));

                    MVMap<String, byte[]> map = store.openMap(
                            mapName,
                            new MVMap.Builder<String, byte[]>()
                                    .valueType(new SummariesMigrationV1.RawBytesDataType())
                    );

                    for (var entry : map.entrySet()) {
                        String citationKey = entry.getKey();

                        Optional<SummariesMigrationV1.OldSummary> oldSummary =
                                SummariesMigrationV1.deserializeOldSummary(entry.getValue());

                        oldSummary.ifPresent(summary ->
                                result.add(new DiscoveredEntry(dbPath, citationKey, summary)));
                    }
                }
            }
        }
        return result;
    }

    private record DiscoveredEntry(
            Path dbPath,
            String citationKey,
            SummariesMigrationV1.OldSummary oldSummary
    ) {
    }

    private static class JsonSummariesFile {

        private final java.util.Map<String, java.util.Map<String, JsonSummary>> data;

        private JsonSummariesFile(java.util.Map<String, java.util.Map<String, JsonSummary>> data) {
            this.data = data;
        }

        static JsonSummariesFile parse(URL url) throws Exception {
            String json;
            try (var in = url.openStream()) {
                json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            var mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            java.util.Map<String, java.util.Map<String, JsonSummary>> result = new java.util.LinkedHashMap<>();
            root.properties().forEach(dbEntry -> {
                java.util.Map<String, JsonSummary> byKey = new java.util.LinkedHashMap<>();
                dbEntry.getValue().properties().forEach(keyEntry -> {
                    JsonNode summaryNode = keyEntry.getValue();
                    String aiProviderStr = summaryNode.get("aiProvider").asText();
                    String model = summaryNode.get("model").asText();
                    String content = summaryNode.get("content").asText();

                    AiProvider aiProvider = AiProvider.valueOf(aiProviderStr);
                    byKey.put(keyEntry.getKey(), new JsonSummary(aiProvider, model, content));
                });
                result.put(dbEntry.getKey(), byKey);
            });
            return new JsonSummariesFile(result);
        }

        JsonSummary summaryFor(Path dbPath, String citationKey) {
            var byKey = data.get(dbPath.toString());
            if (byKey == null) {
                return null;
            }
            return byKey.get(citationKey);
        }

        record JsonSummary(AiProvider aiProvider, String model, String content) {
        }
    }
}
