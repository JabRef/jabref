package org.jabref.logic.ai.summarization.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseContext;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Migrates summaries from the old v1 MVStore file to the new v2 repository.
///
/// Old format (v1): Stored in "ai/1/summaries.mv" file.
/// Map keys like "summaries-bibDatabasePath" containing Map&lt;String, Summary&gt;
/// where Summary = record(LocalDateTime timestamp, AiProvider aiProvider, String model, String content)
/// and AiProvider was at org.jabref.model.ai.AiProvider (now moved to org.jabref.model.ai.llm.AiProvider).
///
/// New format (v2): Stored in "ai/2/summaries.mv" file via repository.
/// Uses AiSummaryIdentifier(libraryId, citationKey) to store AiSummary objects.
///
/// **Problem:** Both the Summary class and AiProvider enum no longer exist at their old paths,
/// so MVStore's internal `ObjectDataType` fails with `ClassNotFoundException` before
/// our code can intercept it.
///
/// **Solution:** Open the map with a custom {@link RawBytesDataType} that reads MVStore's binary
/// page format and returns the raw Java-serialized bytes without invoking the standard
/// `ObjectInputStream`. Then a {@link ClassRemappingObjectInputStream} deserializes those
/// bytes while remapping the two deleted class names to current inner types.
public final class SummariesMigrationV1 {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummariesMigrationV1.class);

    private static final String OLD_SUMMARIES_FILE_NAME = "summaries.mv";
    private static final String SUMMARIES_MAP_PREFIX = "summaries";
    private static final String OLD_SUMMARY_CLASS = "org.jabref.logic.ai.summarization.Summary";
    private static final String OLD_AI_PROVIDER_CLASS = "org.jabref.model.ai.AiProvider";

    private SummariesMigrationV1() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Migrates old summary data from v1 file to v2 repository.
    ///
    /// @param bibDatabaseContext  The database context containing the AI library ID
    /// @param repository          The new v2 summaries repository
    /// @param notificationService Service for notifying user of errors
    public static void migrate(
            BibDatabaseContext bibDatabaseContext,
            SummariesRepository repository,
            NotificationService notificationService
    ) {
        if (bibDatabaseContext.getMetaData().getAiLibraryId().isEmpty()) {
            LOGGER.warn("Cannot migrate summaries: AI library ID is not set");
            return;
        }

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.debug("Cannot migrate summaries: database path is not set");
            return;
        }

        String libraryId = bibDatabaseContext.getMetaData().getAiLibraryId().get();
        Path bibDatabasePath = bibDatabaseContext.getDatabasePath().get();

        Path oldFilePath = Directories.getAiFilesDirectory()
                                      .getParent()  // Go from ai/2/ to ai/
                                      .resolve("1")  // Go to ai/1/
                                      .resolve(OLD_SUMMARIES_FILE_NAME);

        migrate(oldFilePath, libraryId, bibDatabasePath, repository, notificationService);
    }

    /// Migrates old summary data from a given v1 MVStore file path to v2 repository.
    /// Package-private to allow direct use in tests without depending on {@link Directories}.
    static void migrate(
            Path oldFilePath,
            String libraryId,
            Path bibDatabasePath,
            SummariesRepository repository,
            NotificationService notificationService
    ) {
        if (!oldFilePath.toFile().exists()) {
            LOGGER.debug("No old summaries file found at {} - skipping migration", oldFilePath);
            return;
        }

        // Open the old v1 MVStore as read-only to avoid modifying it
        try (MVStore oldMvStore = new MVStore.Builder()
                .fileName(oldFilePath.toString())
                .readOnly()
                .open()) {
            String oldMapName = SUMMARIES_MAP_PREFIX + "-" + bibDatabasePath;

            if (!oldMvStore.hasMap(oldMapName)) {
                LOGGER.debug("No summaries found for this database in old file");
                return;
            }

            // Open the map with RawBytesDataType so we get the raw Java-serialized bytes for each
            // value, bypassing MVStore's ObjectDataType which would throw ClassNotFoundException
            // before our remapping code could run.
            // Maps stored with the default openMap(name) have no "type" entry in their metadata,
            // so MVStore skips type-validation and accepts our custom DataType here.
            MVMap<String, byte[]> oldMap = oldMvStore.openMap(
                    oldMapName,
                    new MVMap.Builder<String, byte[]>().valueType(new RawBytesDataType())
            );

            if (oldMap.isEmpty()) {
                LOGGER.debug("Old summaries map is empty");
                return;
            }

            LOGGER.info("Starting migration of {} summaries from v1 to v2", oldMap.size());

            int migratedCount = 0;
            int failedCount = 0;

            for (Map.Entry<String, byte[]> entry : oldMap.entrySet()) {
                String citationKey = entry.getKey();

                try {
                    Optional<OldSummary> oldSummary = deserializeOldSummary(entry.getValue());

                    if (oldSummary.isEmpty()) {
                        continue;
                    }

                    AiSummaryIdentifier newIdentifier = new AiSummaryIdentifier(libraryId, citationKey);

                    // Check if already migrated
                    if (repository.get(newIdentifier).isPresent()) {
                        LOGGER.debug("Skipping {} - already has summary in new format", citationKey);
                        continue;
                    }

                    // Convert to new format
                    AiSummary newSummary = convertToNewSummary(oldSummary.get());
                    repository.set(newIdentifier, newSummary);

                    migratedCount++;
                    LOGGER.debug("Migrated summary for: {}", citationKey);
                } catch (Exception e) {
                    LOGGER.error("Failed to migrate summary for {}: {}", citationKey, e.getMessage(), e);
                    failedCount++;
                }
            }

            LOGGER.info("Successfully migrated {} summaries, {} failed", migratedCount, failedCount);
        } catch (Exception e) {
            LOGGER.error("Failed to migrate summaries from v1 to v2", e);
            notificationService.notify(Localization.lang("Failed to migrate AI summaries. See logs for details."));
        }
    }

    /// Deserializes old Summary using a custom ObjectInputStream that remaps the old AiProvider class.
    static Optional<OldSummary> deserializeOldSummary(byte[] data) {
        try (ClassRemappingObjectInputStream ois = new ClassRemappingObjectInputStream(
                new java.io.ByteArrayInputStream(data))) {
            Object obj = ois.readObject();

            if (obj instanceof OldSummary summary) {
                return Optional.of(summary);
            } else {
                LOGGER.warn("Deserialized object is not OldSummary: {}", obj.getClass());
                return Optional.empty();
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("Failed to deserialize old summary", e);
            return Optional.empty();
        }
    }

    private static AiSummary convertToNewSummary(OldSummary oldSummary) {
        Instant timestamp = oldSummary.timestamp().atZone(ZoneId.systemDefault()).toInstant();

        return new AiSummary(
                new AiMetadata(oldSummary.aiProvider(), oldSummary.model(), timestamp),
                SummarizatorKind.CHUNKED,  // Old summaries were made using only the chunked summarizator.
                oldSummary.content()
        );
    }

    /// Custom ObjectInputStream that remaps the deleted/moved class names to current inner types,
    /// so that Java's serialization machinery can instantiate them from the raw bytes.
    private static class ClassRemappingObjectInputStream extends ObjectInputStream {
        public ClassRemappingObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass desc = super.readClassDescriptor();

            // Remap the old (deleted) Summary record to our local OldSummary record.
            // Both are records with the same 4 field names and serialVersionUID = 0 (spec default
            // for records), so deserialization will match fields by name and call OldSummary's
            // canonical constructor.
            if (OLD_SUMMARY_CLASS.equals(desc.getName())) {
                return ObjectStreamClass.lookup(OldSummary.class);
            }

            // Remap AiProvider moved from org.jabref.model.ai to org.jabref.model.ai.llm.
            // Java reads the enum constant by name via Enum.valueOf(remappedClass, name), so as
            // long as the constant names are the same in both enums this works transparently.
            if (OLD_AI_PROVIDER_CLASS.equals(desc.getName())) {
                return ObjectStreamClass.lookup(AiProvider.class);
            }

            return desc;
        }
    }

    /// Reads raw Java-serialized bytes from MVStore's binary page format without invoking
    /// Java deserialization. MVStore's `ObjectDataType` stores each serializable value as:
    ///
    /// - 1 byte: type identifier — `19` (`TYPE_SERIALIZED_OBJECT` in H2 2.3.232)
    /// - VarInt: byte length of the serialized payload
    /// - N bytes: the raw Java-serialized payload (starts with `0xACED 0x0005`)
    ///
    /// By returning the raw bytes we defer deserialization to {@link ClassRemappingObjectInputStream},
    /// which can remap deleted/moved class names before they ever reach the class loader.
    static class RawBytesDataType extends BasicDataType<byte[]> {

        // Mirrors the private constant ObjectDataType.TYPE_SERIALIZED_OBJECT in H2 2.3.232.
        // Verified by inspecting H2 bytecode: ConstantValue int 19 for TYPE_SERIALIZED_OBJECT.
        private static final int TYPE_SERIALIZED_OBJECT = 19;

        @Override
        public byte[] read(ByteBuffer buff) {
            int type = buff.get() & 0xff;
            if (type != TYPE_SERIALIZED_OBJECT) {
                throw new IllegalStateException(
                        "Unexpected MVStore type byte " + type
                                + " while reading summaries for migration (expected 19 = serialized object).");
            }
            int len = readVarInt(buff);
            byte[] data = new byte[len];
            buff.get(data);
            return data;
        }

        @Override
        public void write(WriteBuffer buff, byte[] obj) {
            // Migration opens the store as read-only; this path should never execute.
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

        /// Reads a variable-length encoded int from `buff`, using the same encoding that
        /// H2's `DataUtils.readVarInt` writes. Implemented inline to avoid a dependency
        /// on whether `org.h2.mvstore.DataUtils` is exported by the H2 module.
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

    /// Represents the old Summary format from v1.
    /// This must match the structure of the old record.
    record OldSummary(
            LocalDateTime timestamp,
            AiProvider aiProvider,
            String model,
            String content
    ) implements java.io.Serializable {
    }
}
