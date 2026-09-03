package org.jabref.logic.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.jabref.logic.util.io.FileSnapshot;
import org.jabref.logic.util.io.FileUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("exporter")
class AtomicFileOutputStreamTest {

    private static final String FIFTY_CHARS = "1234567890".repeat(5);
    private static final String FIVE_THOUSAND_CHARS = "A".repeat(5_000);

    @Test
    void normalSaveWorks(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("normal-save.txt");
        Files.writeString(out, FIFTY_CHARS);

        try (AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(out)) {
            InputStream inputStream = new ByteArrayInputStream(FIVE_THOUSAND_CHARS.getBytes());
            inputStream.transferTo(atomicFileOutputStream);
        }

        // Written file still has the contents as before the error
        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(out));
    }

    @Test
    void userDefinedAttributesArePreserved(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("tagged.txt");
        Files.writeString(out, FIFTY_CHARS);
        UserDefinedFileAttributeView view = Files.getFileAttributeView(out, UserDefinedFileAttributeView.class);
        assumeTrue(view != null, "file system has no user-defined attribute view");
        try {
            view.write("jabref.test", StandardCharsets.UTF_8.encode("tagged"));
        } catch (IOException exception) {
            assumeTrue(false, "file system does not support user-defined attributes: " + exception);
        }

        try (AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(out)) {
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        UserDefinedFileAttributeView savedView = Files.getFileAttributeView(out, UserDefinedFileAttributeView.class);
        ByteBuffer value = ByteBuffer.allocate(savedView.size("jabref.test"));
        savedView.read("jabref.test", value);
        value.flip();
        assertEquals("tagged", StandardCharsets.UTF_8.decode(value).toString());
        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(out));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void saveWorksForTargetAtMaximumFileNameLength(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("a".repeat(FileUtil.MAXIMUM_FILE_NAME_LENGTH));
        Files.writeString(targetFile, FIFTY_CHARS);

        try (AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile)) {
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(targetFile));
    }

    // [utest->req~logic.exporter.concurrent-save-detection~1]
    @Test
    void interleavedSavesDoNotOverwriteEachOther(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("simultaneous-save.txt");
        Files.writeString(targetFile, FIFTY_CHARS);

        AtomicFileOutputStream firstSave = new AtomicFileOutputStream(targetFile);
        AtomicFileOutputStream secondSave = new AtomicFileOutputStream(targetFile);
        firstSave.write("first".getBytes());
        secondSave.write("second".getBytes());

        firstSave.close();
        assertEquals("first", Files.readString(targetFile));

        // The save finishing last must not win: it would overwrite the content committed by the first save
        assertThrows(FileChangedException.class, secondSave::close);
        assertEquals("first", Files.readString(targetFile));
        try (Stream<Path> remainingFiles = Files.list(tempDir)) {
            // Neither a temporary nor a backup file of the aborted save is left behind
            assertEquals(List.of(targetFile), remainingFiles.toList());
        }
    }

    @Test
    void externalChangeOfTargetAbortsSave(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("externally-changed.txt");
        Files.writeString(targetFile, FIFTY_CHARS);

        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile);
        atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        Files.writeString(targetFile, "externally changed");

        assertThrows(FileChangedException.class, atomicFileOutputStream::close);
        assertEquals("externally changed", Files.readString(targetFile));
    }

    @Test
    void externalCreationOfTargetAbortsSave(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("externally-created.txt");

        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile);
        atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        Files.writeString(targetFile, "externally created");

        assertThrows(FileChangedException.class, atomicFileOutputStream::close);
        assertEquals("externally created", Files.readString(targetFile));
    }

    @Test
    void externalChangeDuringBackupCreationLeavesNoBackupBehind(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("changed-during-backup.txt");
        Path temporaryFile = tempDir.resolve("changed-during-backup.txt.tmp");
        Files.writeString(targetFile, FIFTY_CHARS);

        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(
                targetFile,
                temporaryFile,
                Files.newOutputStream(temporaryFile),
                false,
                (source, target) -> {
                    throw new AssertionError("The aborted save must not commit");
                },
                (source, target) -> {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    // Simulate a concurrent writer hitting the target while the backup copy is running
                    Files.writeString(source, "changed during backup");
                });
        atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());

        assertThrows(FileChangedException.class, atomicFileOutputStream::close);

        assertEquals("changed during backup", Files.readString(targetFile));
        assertFalse(Files.exists(atomicFileOutputStream.getBackup()));
    }

    @Test
    void inheritedBaselineDetectsChangePredatingStreamCreation(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("inherited-baseline.txt");
        Files.writeString(targetFile, FIFTY_CHARS);
        FileSnapshot baseline = FileSnapshot.read(targetFile);
        // The concurrent write happens before the stream is opened — only the inherited baseline can detect it
        Files.writeString(targetFile, "changed before stream creation");

        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile, false, baseline);
        atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());

        assertThrows(FileChangedException.class, atomicFileOutputStream::close);
        assertEquals("changed before stream creation", Files.readString(targetFile));
    }

    @Test
    void committedTargetFileStateMatchesFileAfterSuccessfulClose(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("committed-state.txt");
        Files.writeString(targetFile, FIFTY_CHARS);

        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile);
        assertNull(atomicFileOutputStream.getCommittedTargetFileState());
        atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        atomicFileOutputStream.close();

        assertEquals(FileSnapshot.read(targetFile), atomicFileOutputStream.getCommittedTargetFileState());
    }

    @Test
    void externalDeletionOfTargetAbortsSave(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("externally-deleted.txt");
        Files.writeString(targetFile, FIFTY_CHARS);

        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile);
        atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        Files.delete(targetFile);

        assertThrows(FileChangedException.class, atomicFileOutputStream::close);
        assertFalse(Files.exists(targetFile));
    }

    @Test
    void failedSingleByteWriteDoesNotCommitTemporaryFile(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("single-byte-write-error.txt");
        Files.writeString(targetFile, FIFTY_CHARS);
        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(
                targetFile,
                tempDir.resolve("single-byte-write-error.txt.tmp"),
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        throw new IOException();
                    }
                },
                false);

        assertThrows(IOException.class, () -> atomicFileOutputStream.write(1));
        atomicFileOutputStream.close();

        assertEquals(FIFTY_CHARS, Files.readString(targetFile));
    }

    @Test
    void failedFlushDoesNotCommitTemporaryFile(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("flush-error.txt");
        Path temporaryFile = tempDir.resolve("flush-error.txt.tmp");
        Files.writeString(targetFile, FIFTY_CHARS);
        OutputStream temporaryFileOutputStream = new OutputStream() {
            private boolean failFlush = true;

            @Override
            public void write(int b) {
            }

            @Override
            public void flush() throws IOException {
                if (failFlush) {
                    failFlush = false;
                    throw new IOException();
                }
            }
        };
        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile, temporaryFile, temporaryFileOutputStream, false);
        atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());

        assertThrows(IOException.class, atomicFileOutputStream::flush);
        atomicFileOutputStream.close();

        assertEquals(FIFTY_CHARS, Files.readString(targetFile));
    }

    @Test
        // [utest->req~jabgui.autosaveandbackup.complete-backup~1]
    void abortedWriteDoesNotCommitPartialContent(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("aborted-write.txt");
        Files.writeString(targetFile, FIFTY_CHARS);

        try (AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile)) {
            atomicFileOutputStream.write("partial content".getBytes());
            atomicFileOutputStream.abort();
        }

        assertEquals(FIFTY_CHARS, Files.readString(targetFile));
    }

    @Test
    void abortDoesNotDeleteExistingBackup(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("abort.txt");
        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile);
        Files.writeString(atomicFileOutputStream.getBackup(), FIFTY_CHARS);

        atomicFileOutputStream.abort();

        assertEquals(FIFTY_CHARS, Files.readString(atomicFileOutputStream.getBackup()));
    }

    @Test
        // [utest->req~jabgui.autosaveandbackup.complete-backup~1]
    void failedBackupStagingDoesNotReplaceExistingBackup(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("backup-staging.txt");
        Path temporaryFile = tempDir.resolve("backup-staging.txt.tmp");
        Files.writeString(targetFile, FIFTY_CHARS);

        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(
                targetFile,
                temporaryFile,
                Files.newOutputStream(temporaryFile),
                true,
                (source, target) -> Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING),
                (source, target) -> {
                    Files.writeString(target, "partial backup");
                    throw new IOException("Simulated interruption while creating backup");
                });
        Path backupFile = atomicFileOutputStream.getBackup();
        try (atomicFileOutputStream) {
            Files.writeString(atomicFileOutputStream.getBackup(), FIFTY_CHARS);
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        assertEquals(FIFTY_CHARS, Files.readString(backupFile));
        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(targetFile));
    }

    @Test
    void fallsBackToInPlaceSaveWhenAtomicMoveIsNotSupported(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("atomic-move-not-supported.txt");
        Path temporaryFile = tempDir.resolve("atomic-move-not-supported.txt.tmp");
        Files.writeString(targetFile, FIFTY_CHARS);

        try (OutputStream temporaryFileOutputStream = Files.newOutputStream(temporaryFile);
             AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(
                     targetFile,
                     temporaryFile,
                     temporaryFileOutputStream,
                     false,
                     (source, target) -> {
                         throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "test");
                     })) {
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(targetFile));
    }

    @Test
    void fallsBackToInPlaceSaveAfterMoveRetriesAreExhausted(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("atomic-move-fails.txt");
        Path temporaryFile = tempDir.resolve("atomic-move-fails.txt.tmp");
        AtomicInteger moveAttempts = new AtomicInteger();
        Files.writeString(targetFile, FIFTY_CHARS);

        try (OutputStream temporaryFileOutputStream = Files.newOutputStream(temporaryFile);
             AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(
                     targetFile,
                     temporaryFile,
                     temporaryFileOutputStream,
                     false,
                     (source, target) -> {
                         moveAttempts.incrementAndGet();
                         throw new FileSystemException(source.toString(), target.toString(), "test");
                     })) {
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        assertEquals(5, moveAttempts.get());
        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(targetFile));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void savePreservesHardLinksOnLinux(@TempDir Path tempDir) throws IOException {
        assertSavePreservesHardLinks(tempDir);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void savePreservesHardLinksOnMacOs(@TempDir Path tempDir) throws IOException {
        assertSavePreservesHardLinks(tempDir);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void savePreservesSymbolicLinks(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("symbolic-link-target.txt");
        Path symbolicLink = tempDir.resolve("symbolic-link.txt");
        Files.writeString(targetFile, FIFTY_CHARS);
        Files.createSymbolicLink(symbolicLink, targetFile);

        try (AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(symbolicLink)) {
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        assertEquals(targetFile, Files.readSymbolicLink(symbolicLink));
        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(targetFile));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void saveReplacesSymbolicLinkWhenBackupAndAtomicMoveAreUnavailable(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("symbolic-link-target.txt");
        Path symbolicLink = tempDir.resolve("symbolic-link.txt");
        Files.writeString(targetFile, FIFTY_CHARS);
        Files.createSymbolicLink(symbolicLink, targetFile);

        Path temporaryFile = tempDir.resolve("symbolic-link.txt.tmp");
        try (OutputStream temporaryFileOutputStream = Files.newOutputStream(temporaryFile);
             AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(
                     symbolicLink,
                     temporaryFile,
                     temporaryFileOutputStream,
                     true,
                     (source, target) -> {
                         throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "test");
                     },
                     (source, target) -> {
                         throw new IOException("test");
                     })) {
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        assertFalse(Files.isSymbolicLink(symbolicLink));
        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(symbolicLink));
        assertEquals(FIFTY_CHARS, Files.readString(targetFile));
    }

    private void assertSavePreservesHardLinks(Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("hard-linked-save.txt");
        Path hardLink = tempDir.resolve("hard-linked-save-link.txt");
        Files.writeString(targetFile, FIFTY_CHARS);
        Files.createLink(hardLink, targetFile);

        try (AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile)) {
            atomicFileOutputStream.write(FIVE_THOUSAND_CHARS.getBytes());
        }

        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(targetFile));
        assertEquals(FIVE_THOUSAND_CHARS, Files.readString(hardLink));
        assertEquals(Files.getAttribute(targetFile, "unix:ino"), Files.getAttribute(hardLink, "unix:ino"));
    }

    @Test
    void originalContentExistsAtWriteError(@TempDir Path tempDir) throws IOException {
        Path pathToTestFile = tempDir.resolve("error-during-save.txt");
        Files.writeString(pathToTestFile, FIFTY_CHARS);

        Path pathToTmpFile = tempDir.resolve("error-during-save.txt.tmp");

        try (OutputStream outputStream = Files.newOutputStream(pathToTmpFile)) {
            OutputStream spiedOutputStream = spy(outputStream);
            doAnswer(invocation -> {
                // by writing one byte, we ensure that the `.tmp` file is created
                outputStream.write(((byte[]) invocation.getRawArguments()[0])[0]);
                outputStream.flush();
                throw new IOException();
            }).when(spiedOutputStream)
              .write(Mockito.any(byte[].class), anyInt(), anyInt());

            assertThrows(IOException.class, () -> {
                try (AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(pathToTestFile, pathToTmpFile, spiedOutputStream, false);
                     InputStream inputStream = new ByteArrayInputStream(FIVE_THOUSAND_CHARS.getBytes())) {
                    inputStream.transferTo(atomicFileOutputStream);
                }
            });
        }

        // Written file still has the contents as before the error
        assertEquals(FIFTY_CHARS, Files.readString(pathToTestFile));
    }
}
