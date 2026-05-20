package org.jabref.logic.ai.chatting.migrations;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.chatting.ChatHistoryRecord;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.ChatType;
import org.jabref.model.ai.chatting.ErrorMessage;
import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Migrates chat history from the old v1 MVStore file to the new v2 repository.
///
/// Old format (v1): Stored in "chat-history.mv" file.
/// Map keys like "bibDatabasePath-entry-citationKey" or "bibDatabasePath-group-groupName"
/// containing Map&lt;Integer, ChatHistoryRecord&gt; where ChatHistoryRecord was a private inner
/// class of MVStoreChatHistoryStorage (full name:
/// org.jabref.logic.ai.chatting.chathistory.storages.MVStoreChatHistoryStorage$ChatHistoryRecord).
///
/// New format (v2): Stored via repository using ChatIdentifier(libraryId, chatType, chatName).
///
/// **Problem:** The old inner class no longer exists, so MVStore's ObjectDataType fails with
/// ClassNotFoundException before our code can intercept.
///
/// **Solution:** Open the map with a custom {@link RawBytesDataType} that returns the raw
/// Java-serialized bytes for each value without invoking standard Java deserialization.
/// Then a {@link ClassRemappingObjectInputStream} deserializes those bytes while remapping
/// the deleted inner class name to the current {@link ChatHistoryRecord}.
public final class ChatHistoryMigrationV1 {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryMigrationV1.class);

    private static final String OLD_CHAT_HISTORY_FILE_NAME = "chat-histories.mv";
    private static final String ENTRY_CHAT_HISTORY_INFIX = "-entry-";
    private static final String GROUP_CHAT_HISTORY_INFIX = "-group-";

    /// Old inner class full name that is no longer in the classpath.
    /// Was: private record ChatHistoryRecord inside MVStoreChatHistoryStorage.
    private static final String OLD_CHAT_HISTORY_RECORD_CLASS =
            "org.jabref.logic.ai.chatting.chathistory.storages.MVStoreChatHistoryStorage$ChatHistoryRecord";

    private ChatHistoryMigrationV1() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Migrates old chat history data from v1 file to v2 repository.
    ///
    /// @param bibDatabaseContext  The database context containing the AI library ID
    /// @param repository          The new v2 chat history repository to migrate to
    /// @param notificationService Service for notifying user of errors
    public static void migrate(
            BibDatabaseContext bibDatabaseContext,
            ChatHistoryRepository repository,
            NotificationService notificationService
    ) {
        if (bibDatabaseContext.getMetaData().getAiLibraryId().isEmpty()) {
            LOGGER.warn("Cannot migrate chat history: AI library ID is not set");
            return;
        }

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Cannot migrate chat history: database path is not set");
            return;
        }

        String libraryId = bibDatabaseContext.getMetaData().getAiLibraryId().get();

        Path oldFilePath = Directories.getAiFilesDirectory()
                                      .getParent()
                                      .resolve("1")
                                      .resolve(OLD_CHAT_HISTORY_FILE_NAME);

        migrate(oldFilePath, libraryId, bibDatabaseContext, repository, notificationService);
    }

    public static void migrate(
            Path oldFilePath,
            String libraryId,
            BibDatabaseContext bibDatabaseContext,
            ChatHistoryRepository repository,
            NotificationService notificationService
    ) {
        if (!oldFilePath.toFile().exists()) {
            LOGGER.debug("No old chat history file found at {} - skipping migration", oldFilePath);
            return;
        }

        try (MVStore oldMvStore = new MVStore.Builder()
                .fileName(oldFilePath.toString())
                .readOnly()
                .open()) {
            List<String> oldMapNames = new ArrayList<>();
            List<String> migratedMapNames = new ArrayList<>();

            for (String mapName : oldMvStore.getMapNames()) {
                if (isOldChatHistoryMap(mapName)) {
                    oldMapNames.add(mapName);
                }
            }

            if (oldMapNames.isEmpty()) {
                LOGGER.debug("No old chat history data found for migration");
                return;
            }

            LOGGER.info("Starting migration of {} chat history maps from v1 to v2", oldMapNames.size());

            for (String oldMapName : oldMapNames) {
                try {
                    if (migrateOldMap(oldMapName, libraryId, bibDatabaseContext, repository, oldMvStore)) {
                        migratedMapNames.add(oldMapName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to migrate chat history map: {}", oldMapName, e);
                }
            }

            LOGGER.info("Successfully migrated {} of {} chat history maps",
                    migratedMapNames.size(), oldMapNames.size());
        } catch (Exception e) {
            LOGGER.error("Failed to migrate chat history from v1 to v2", e);
            notificationService.notify(Localization.lang("Failed to migrate AI chat history. See logs for details."));
        }
    }

    private static boolean isOldChatHistoryMap(String mapName) {
        return mapName.contains(ENTRY_CHAT_HISTORY_INFIX) || mapName.contains(GROUP_CHAT_HISTORY_INFIX);
    }

    private static boolean migrateOldMap(
            String oldMapName,
            String libraryId,
            BibDatabaseContext bibDatabaseContext,
            ChatHistoryRepository repository,
            MVStore oldMvStore
    ) {
        ChatType chatType;
        String chatName;
        String pathPrefix;

        if (oldMapName.contains(ENTRY_CHAT_HISTORY_INFIX)) {
            chatType = ChatType.WITH_ENTRY;
            int index = oldMapName.lastIndexOf(ENTRY_CHAT_HISTORY_INFIX);
            pathPrefix = oldMapName.substring(0, index);
            chatName = oldMapName.substring(index + ENTRY_CHAT_HISTORY_INFIX.length());

            if (!pathMatchesCurrentLibrary(pathPrefix, bibDatabaseContext)) {
                LOGGER.debug("Skipping chat history migration for {}: path prefix '{}' does not match current library '{}'",
                        oldMapName, pathPrefix, bibDatabaseContext.getDatabasePath().map(Path::toString).orElse("<none>"));
                return false;
            }

            if (bibDatabaseContext.getDatabase().getEntriesByCitationKey(chatName).isEmpty()) {
                LOGGER.debug("Skipping chat history migration for non-existent entry: {}", chatName);
                return false;
            }
        } else if (oldMapName.contains(GROUP_CHAT_HISTORY_INFIX)) {
            chatType = ChatType.WITH_GROUP;
            int index = oldMapName.lastIndexOf(GROUP_CHAT_HISTORY_INFIX);
            pathPrefix = oldMapName.substring(0, index);
            chatName = oldMapName.substring(index + GROUP_CHAT_HISTORY_INFIX.length());

            if (!pathMatchesCurrentLibrary(pathPrefix, bibDatabaseContext)) {
                LOGGER.debug("Skipping chat history migration for {}: path prefix '{}' does not match current library '{}'",
                        oldMapName, pathPrefix, bibDatabaseContext.getDatabasePath().map(Path::toString).orElse("<none>"));
                return false;
            }
        } else {
            LOGGER.error("Unknown chat history map format: {}", oldMapName);
            return false;
        }

        // Open with RawBytesDataType so values are returned as raw Java-serialized bytes,
        // bypassing ObjectDataType which would throw ClassNotFoundException for the deleted class.
        MVMap<Integer, byte[]> oldMap = oldMvStore.openMap(
                oldMapName,
                new MVMap.Builder<Integer, byte[]>().valueType(new RawBytesDataType())
        );

        if (oldMap.isEmpty()) {
            LOGGER.debug("Skipping empty chat history map: {}", oldMapName);
            return true;
        }

        ChatIdentifier newIdentifier = new ChatIdentifier(libraryId, chatType, chatName);

        if (!repository.isEmpty(newIdentifier)) {
            LOGGER.debug("Skipping migration for {} - new format already has data", oldMapName);
            return false;
        }

        List<ChatMessage> newMessages = new ArrayList<>();

        Instant baseTime = Instant.now();
        int index = 0;
        for (Map.Entry<Integer, byte[]> entry : oldMap.entrySet()
                                                      .stream()
                                                      .sorted(Comparator.comparingInt(Map.Entry::getKey))
                                                      .toList()) {
            try {
                ChatHistoryRecord record = deserializeOldRecord(entry.getValue());
                if (record != null) {
                    ChatMessage msg = convertToNewChatMessage(record, baseTime.plusMillis(index));
                    newMessages.add(msg);
                    index++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to deserialize chat history record at index {}: {}",
                        entry.getKey(), e.getMessage(), e);
            }
        }

        for (ChatMessage message : newMessages) {
            repository.addMessage(newIdentifier, message);
        }

        LOGGER.debug("Migrated {} messages from {}", newMessages.size(), oldMapName);
        return true;
    }

    /// Returns {@code true} if {@code pathPrefix} (extracted from an old map name) refers to the
    /// same file as the current library.
    ///
    /// - If the prefix is an **absolute** path, it is compared after normalization.
    /// - If the prefix is a **relative** path, the current library's absolute path is checked with
    ///   {@link Path#endsWith(Path)}, so {@code a/lib1.bib} matches {@code D:/my/a/lib1.bib}.
    private static boolean pathMatchesCurrentLibrary(String pathPrefix, BibDatabaseContext bibDatabaseContext) {
        if (pathPrefix.isEmpty() || bibDatabaseContext.getDatabasePath().isEmpty()) {
            return false;
        }

        Path oldPath = Path.of(pathPrefix);
        Path currentAbsolute = bibDatabaseContext.getDatabasePath().get().toAbsolutePath().normalize();

        if (oldPath.isAbsolute()) {
            return oldPath.normalize().equals(currentAbsolute);
        }
        return currentAbsolute.endsWith(oldPath);
    }

    /// Deserializes old ChatHistoryRecord bytes using a remapping ObjectInputStream.
    /// Remaps the deleted inner class name to the current {@link ChatHistoryRecord}.
    private static ChatHistoryRecord deserializeOldRecord(byte[] data) {
        try (ClassRemappingObjectInputStream ois = new ClassRemappingObjectInputStream(
                new java.io.ByteArrayInputStream(data))) {
            Object obj = ois.readObject();
            if (obj instanceof ChatHistoryRecord record) {
                return record;
            }
            LOGGER.warn("Deserialized object is not ChatHistoryRecord: {}", obj.getClass());
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize old chat history record: {}", e.getMessage(), e);
            return null;
        }
    }

    private static ChatMessage convertToNewChatMessage(ChatHistoryRecord oldRecord, Instant timestamp) {
        String className = oldRecord.className();
        String content = oldRecord.content();

        ChatMessage.Role role;

        if (className.equals(AiMessage.class.getName())) {
            role = ChatMessage.Role.AI;
        } else if (className.equals(UserMessage.class.getName())) {
            role = ChatMessage.Role.USER;
        } else if (className.equals(ErrorMessage.class.getName())) {
            role = ChatMessage.Role.ERROR;
        } else {
            LOGGER.warn("Unknown message type during migration: {}", className);
            role = ChatMessage.Role.AI;
        }

        return new ChatMessage(
                UUID.randomUUID().toString(),
                timestamp,
                role,
                content,
                List.of()
        );
    }

    /// Custom ObjectInputStream that remaps the deleted old inner class name
    /// to the current {@link ChatHistoryRecord} class.
    ///
    /// The old and new records have identical components (className: String, content: String),
    /// so Java record deserialization will call the canonical constructor successfully.
    private static class ClassRemappingObjectInputStream extends ObjectInputStream {
        public ClassRemappingObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass desc = super.readClassDescriptor();

            // Remap the old deleted inner class to the current ChatHistoryRecord.
            // Both records have identical components (className, content) with serialVersionUID = 0.
            if (OLD_CHAT_HISTORY_RECORD_CLASS.equals(desc.getName())) {
                return ObjectStreamClass.lookup(ChatHistoryRecord.class);
            }

            return desc;
        }
    }

    /// Reads raw Java-serialized bytes from MVStore's binary page format without invoking
    /// Java deserialization. MVStore's ObjectDataType stores each serializable value as:
    /// <ol>
    /// - 1 byte: type identifier — `19` (TYPE_SERIALIZED_OBJECT in H2 2.3.232)
    /// - VarInt: byte length of the serialized payload
    /// - N bytes: the raw Java-serialized payload
    /// </ol>
    private static class RawBytesDataType extends BasicDataType<byte[]> {

        // TYPE_SERIALIZED_OBJECT = 19, verified from H2 2.3.232 bytecode.
        private static final int TYPE_SERIALIZED_OBJECT = 19;

        @Override
        public byte[] read(ByteBuffer buff) {
            int type = buff.get() & 0xff;
            if (type != TYPE_SERIALIZED_OBJECT) {
                throw new IllegalStateException(
                        "Unexpected MVStore type byte " + type
                                + " while reading chat history for migration (expected 19 = serialized object).");
            }
            int len = readVarInt(buff);
            byte[] data = new byte[len];
            buff.get(data);
            return data;
        }

        @Override
        public void write(WriteBuffer buff, byte[] obj) {
            throw new UnsupportedOperationException("RawBytesDataType is read-only (migration use only)");
        }

        @Override
        public int getMemory(byte[] obj) {
            return obj == null ? 0 : (obj.length + 4);
        }

        @Override
        public byte[][] createStorage(int size) {
            return new byte[size][];
        }

        private static int readVarInt(ByteBuffer buff) {
            int b = buff.get() & 0xFF;
            if (b < 0x80) {
                return b;
            }
            int value = b & 0x7F;
            for (int shift = 7; ; shift += 7) {
                b = buff.get() & 0xFF;
                value |= (b & 0x7F) << shift;
                if (b < 0x80) {
                    break;
                }
            }
            return value;
        }
    }
}
