package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.stream.Collectors;

import com.google.common.io.Files;
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
        assertTrue(output.toFile().exists());
        assertTrue(key.toFile().exists());
        assertFalse(Files.readLines(output.toFile(), Charset.defaultCharset()).stream().anyMatch((s) -> s.contains("Newton1999")));
    }

    @Test
    public void automaticFileCreation(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib");
        Path copy = tempDir.resolve("origin.bib");
        Files.copy(origin.toFile(), copy.toFile());
        commandLine.execute("pseudonymize", "--input=" + copy);
        Path output = tempDir.resolve("origin.pseudo.bib");
        Path key = tempDir.resolve("origin.pseudo.csv");
        assertTrue(output.toFile().exists(), Files.list(tempDir).map(f -> f.toString()).collect(Collectors.joining(", ")));
        assertTrue(key.toFile().exists(), Files.list(tempDir).map(f -> f.toString()).collect(Collectors.joining(", ")));
    }

    @Test
    public void forceUsage(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path output = tempDir.resolve("new.pseudo.bib");
        Path key = tempDir.resolve("new.pseudo.csv");
        Files.write("some".getBytes(StandardCharsets.UTF_8), output.toFile());
        commandLine.execute("pseudonymize", "-f", "--input=" + origin, "--output=" + output, "--key=" + key);
        assertTrue(output.toFile().exists());
        assertTrue(key.toFile().exists());
        assertTrue(Files.readLines(output.toFile(), Charset.defaultCharset()).size() > 1);
    }

    @Test
    public void noForceUsage(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path output = tempDir.resolve("new.pseudo.bib");
        Path key = tempDir.resolve("new.pseudo.csv");
        Files.write("some".getBytes(StandardCharsets.UTF_8), key.toFile());
        commandLine.execute("pseudonymize", "--input=" + origin, "--output=" + output, "--key=" + key);
        java.nio.file.Files.list(tempDir).forEach(System.out::println);
        assertTrue(output.toFile().exists());
        assertTrue(key.toFile().exists());
        assertFalse(Files.readLines(key.toFile(), Charset.defaultCharset()).size() > 1);
    }
}
