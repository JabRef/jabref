package org.jabref.logic.exporter;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

class AtomicFileOutputStreamTest {

    private static final String FIFTY_CHARS = Strings.repeat("1234567890", 5);
    private static final String FIVE_THOUSAND_CHARS = Strings.repeat("A", 5_000);

    @Test
    public void normalSaveWorks(@TempDir Path tempDir) throws Exception {
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
    public void originalContentExistsAtWriteError(@TempDir Path tempDir) throws Exception {
        Path pathToTestFile = tempDir.resolve("error-during-save.txt");
        Files.writeString(pathToTestFile, FIFTY_CHARS);

        Path pathToTmpFile = tempDir.resolve("error-during-save.txt.tmp");

        try (FileOutputStream outputStream = new FileOutputStream(pathToTmpFile.toFile())) {
            FileOutputStream spiedOutputStream = spy(outputStream);
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
