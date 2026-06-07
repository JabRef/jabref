package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PseudonymizeTest extends AbstractJabKitTest {
    @Test
    public void normalUsage(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path output = tempDir.resolve("new.pseudo.bib");
        Path key = tempDir.resolve("new.pseudo.csv");
        commandLine.execute("pseudonymize", "--input=" + origin, "--output=" + output, "--key=" + key);
        assertFileExists(output);
        assertFileExists(key);
        assertFalse(Files.readAllLines(output, Charset.defaultCharset()).stream().anyMatch((s) -> s.contains("Newton1999")));
    }

    @Test
    public void automaticFileCreation(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib");
        Path copy = tempDir.resolve("origin.bib");
        Files.copy(origin, copy);
        commandLine.execute("pseudonymize", "--input=" + copy);
        Path output = tempDir.resolve("origin.pseudo.bib");
        Path key = tempDir.resolve("origin.pseudo.csv");
        assertFileExists(output);
        assertFileExists(key);
    }

    @Test
    public void forceUsage(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path output = tempDir.resolve("new.pseudo.bib");
        Path key = tempDir.resolve("new.pseudo.csv");
        Files.write(output, "some".getBytes(StandardCharsets.UTF_8));
        commandLine.execute("pseudonymize", "-f", "--input=" + origin, "--output=" + output, "--key=" + key);
        assertFileExists(output);
        assertFileExists(key);
        assertTrue(Files.readAllLines(output, Charset.defaultCharset()).size() > 1);
    }

    @Test
    public void noForceUsage(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path output = tempDir.resolve("new.pseudo.bib");
        Path key = tempDir.resolve("new.pseudo.csv");
        Files.write(key, "some".getBytes(StandardCharsets.UTF_8));
        commandLine.execute("pseudonymize", "--input=" + origin, "--output=" + output, "--key=" + key);
        assertFileExists(output);
        assertFileExists(key);
        assertFalse(Files.readAllLines(key, Charset.defaultCharset()).size() > 1);
    }
}
