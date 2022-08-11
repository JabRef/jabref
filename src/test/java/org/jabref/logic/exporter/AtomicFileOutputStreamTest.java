package org.jabref.logic.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Strings;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtomicFileOutputStreamTest {

    /**
     * Tests whether a failing write to a file keeps the respective .sav file
     */
    @Test
    public void savFileExistsOnDiskFull() throws Exception {
        String fiftyChars = Strings.repeat("1234567890", 5);
        String fiveThousandChars = Strings.repeat("A", 5_000);

        FileSystem fileSystem = Jimfs.newFileSystem(
                Configuration.unix().toBuilder()
                             .setMaxSize(1_000)
                             .setBlockSize(1_000).build());

        Path out = fileSystem.getPath("out.bib");
        Files.writeString(out, fiftyChars);

        // Running out of disk space should occur
        assertThrows(IOException.class, () -> {
            AtomicFileOutputStream atomicFileOutputStream = new AtomicFileOutputStream(out);
            InputStream inputStream = new ByteArrayInputStream(fiveThousandChars.getBytes());
            inputStream.transferTo(atomicFileOutputStream);
            atomicFileOutputStream.close();
        });

        // Written file still has the contents as before the error
        assertEquals(fiftyChars, Files.readString(out));
    }

    @Test
    void tempAndBackupDifferentPaths() {
        Path testFile = Path.of("test.bib");
        assertNotEquals(AtomicFileOutputStream.getPathOfTemporaryFile(testFile), AtomicFileOutputStream.getPathOfBackupFile(testFile));
    }
}
