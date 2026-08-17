package org.jabref.logic.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void interleavedSavesUseSeparateTemporaryFiles(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("simultaneous-save.txt");
        Files.writeString(targetFile, FIFTY_CHARS);

        AtomicFileOutputStream firstSave = new AtomicFileOutputStream(targetFile);
        AtomicFileOutputStream secondSave = new AtomicFileOutputStream(targetFile);
        firstSave.write("first".getBytes());
        secondSave.write("second".getBytes());

        firstSave.close();
        assertEquals("first", Files.readString(targetFile));

        secondSave.close();
        assertEquals("second", Files.readString(targetFile));
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
    void abortDoesNotDeleteExistingBackup(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("abort.txt");
        AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(targetFile);
        Files.writeString(atomicFileOutputStream.getBackup(), FIFTY_CHARS);

        atomicFileOutputStream.abort();

        assertEquals(FIFTY_CHARS, Files.readString(atomicFileOutputStream.getBackup()));
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
