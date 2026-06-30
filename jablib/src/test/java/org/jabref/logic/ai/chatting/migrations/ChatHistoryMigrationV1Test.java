package org.jabref.logic.ai.chatting.migrations;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.ChatType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Tests that {@link ChatHistoryMigrationV1} correctly migrates every v1 chat-history MVStore
/// found under {@code src/test/resources/…/chatting/migrations/} into the v2 repository.
///
/// Entries (database path + citation key) are discovered dynamically from the MVStore map names,
/// so adding {@code historiesN.mv} alongside its matching {@code historiesN.json} is enough
/// to add a new test case without touching this class.
///
/// The fixed library ID acts as the new identifier created during v1 → v2 migration.
class ChatHistoryMigrationV1Test {

    private static final String TEST_RESOURCES = "src/test/resources/org/jabref/logic/ai/chatting/migrations";
    private static final String LIBRARY_ID = "00000000-0000-0000-0000-000000000002";
    private static final String ENTRY_INFIX = "-entry-";
    private static final String GROUP_INFIX = "-group-";

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
    void allEntriesAreMigrated(String fileName) throws Exception {
        Path oldFilePath = copyResource(fileName);

        List<DiscoveredEntry> entries = discoverEntries(oldFilePath);
        assertFalse(entries.isEmpty(), "Test resource " + fileName + " contains no chat history maps");

        boolean onWindows = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows");
        assumeTrue(onWindows || !hasWindowsAbsolutePaths(entries),
                "Skipping " + fileName + ": contains Windows-style paths, not runnable on this OS");

        BibDatabaseContext ctx = buildContext(entries);
        MVStoreChatHistoryRepository repo = new MVStoreChatHistoryRepository(
                tempDir.resolve("v2-" + fileName), _ -> {
        }
        );

        ChatHistoryMigrationV1.migrate(oldFilePath, LIBRARY_ID, ctx, repo, _ -> {
        });

        for (DiscoveredEntry entry : entries) {
            ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, entry.chatType(), entry.chatName());
            assertFalse(repo.isEmpty(id),
                    "Expected messages for " + entry.chatType() + " / " + entry.chatName());
        }
    }

    @ParameterizedTest
    @MethodSource("mvStoreFiles")
    void migratedMessageRolesAndContentsMatchJsonCompanion(String fileName) throws Exception {
        Path oldFilePath = copyResource(fileName);

        List<DiscoveredEntry> entries = discoverEntries(oldFilePath);

        boolean onWindows = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("windows");
        assumeTrue(onWindows || !hasWindowsAbsolutePaths(entries),
                "Skipping " + fileName + ": contains Windows-style paths, not runnable on this OS");

        BibDatabaseContext ctx = buildContext(entries);
        MVStoreChatHistoryRepository repo = new MVStoreChatHistoryRepository(
                tempDir.resolve("v2-msg-" + fileName), _ -> {
        }
        );

        ChatHistoryMigrationV1.migrate(oldFilePath, LIBRARY_ID, ctx, repo, _ -> {
        });

        String jsonName = fileName.replace(".mv", ".json");
        URL jsonUrl = getClass().getResource(jsonName);
        if (jsonUrl == null) {
            return;
        }

        JsonChatHistoryFile jsonFile = JsonChatHistoryFile.parse(jsonUrl);

        for (DiscoveredEntry entry : entries) {
            if (entry.chatType() != ChatType.WITH_ENTRY) {
                continue;
            }
            List<JsonChatHistoryFile.JsonMessage> expected =
                    jsonFile.messagesFor(entry.dbPath(), entry.chatName());
            if (expected == null) {
                continue;
            }

            ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, entry.chatName());
            List<ChatMessage> actual = repo.getAllMessages(id);

            assertEquals(expected.size(), actual.size(),
                    "Message count mismatch for " + entry.chatName());

            for (int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i).role(), actual.get(i).role(),
                        "Role mismatch at index " + i + " for " + entry.chatName());
                assertEquals(expected.get(i).content().strip(), actual.get(i).content().strip(),
                        "Content mismatch at index " + i + " for " + entry.chatName());
            }
        }
    }

    private Path copyResource(String fileName) throws IOException {
        URL resource = Objects.requireNonNull(getClass().getResource(fileName));
        Path dest = tempDir.resolve(fileName);
        try (var in = resource.openStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
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
                if (mapName.contains(ENTRY_INFIX)) {
                    int idx = mapName.lastIndexOf(ENTRY_INFIX);
                    Path dbPath = Path.of(mapName.substring(0, idx));
                    String citationKey = mapName.substring(idx + ENTRY_INFIX.length());
                    result.add(new DiscoveredEntry(dbPath, ChatType.WITH_ENTRY, citationKey));
                } else if (mapName.contains(GROUP_INFIX)) {
                    int idx = mapName.lastIndexOf(GROUP_INFIX);
                    Path dbPath = Path.of(mapName.substring(0, idx));
                    String groupName = mapName.substring(idx + GROUP_INFIX.length());
                    result.add(new DiscoveredEntry(dbPath, ChatType.WITH_GROUP, groupName));
                }
            }
        }
        return result;
    }

    private BibDatabaseContext buildContext(List<DiscoveredEntry> entries) {
        List<BibEntry> bibEntries = entries.stream()
                                           .filter(e -> e.chatType() == ChatType.WITH_ENTRY)
                                           .map(e -> new BibEntry().withCitationKey(e.chatName()))
                                           .toList();
        BibDatabase database = new BibDatabase(bibEntries);
        MetaData metaData = new MetaData();
        metaData.setAiLibraryId(LIBRARY_ID);
        BibDatabaseContext ctx = new BibDatabaseContext(database, metaData);
        if (!entries.isEmpty()) {
            ctx.setDatabasePath(entries.getFirst().dbPath().toAbsolutePath());
        }
        return ctx;
    }

    private record DiscoveredEntry(Path dbPath, ChatType chatType, String chatName) {
    }

    private static class JsonChatHistoryFile {

        private final Map<String, Map<String, List<JsonMessage>>> data;

        private JsonChatHistoryFile(Map<String, Map<String, List<JsonMessage>>> data) {
            this.data = data;
        }

        static JsonChatHistoryFile parse(URL url) throws Exception {
            String json;
            try (var in = url.openStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            var mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            Map<String, Map<String, List<JsonMessage>>> result = new LinkedHashMap<>();
            root.properties().forEach(dbEntry -> {
                Map<String, List<JsonMessage>> byKey = new LinkedHashMap<>();
                dbEntry.getValue().properties().forEach(keyEntry -> {
                    List<JsonMessage> msgs = new ArrayList<>();
                    keyEntry.getValue().forEach(node -> {
                        String role = node.get("role").asText();
                        String text = node.get("text").asText();
                        msgs.add(new JsonMessage(roleFromJson(role), text));
                    });
                    byKey.put(keyEntry.getKey(), msgs);
                });
                result.put(dbEntry.getKey(), byKey);
            });
            return new JsonChatHistoryFile(result);
        }

        List<JsonMessage> messagesFor(Path dbPath, String citationKey) {
            var byKey = data.get(dbPath.toString());
            if (byKey == null) {
                return null;
            }
            return byKey.get(citationKey);
        }

        private static ChatMessage.Role roleFromJson(String role) {
            return switch (role) {
                case "user" ->
                        ChatMessage.Role.USER;
                case "assistant" ->
                        ChatMessage.Role.AI;
                default ->
                        ChatMessage.Role.valueOf(role.toUpperCase());
            };
        }

        record JsonMessage(ChatMessage.Role role, String content) {
        }
    }
}
