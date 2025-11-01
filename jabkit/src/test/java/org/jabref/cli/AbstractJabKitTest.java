package org.jabref.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;

import javafx.collections.FXCollections;

import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import picocli.CommandLine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractJabKitTest {
    protected final CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
    protected final BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
    protected final ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    protected final ExportPreferences exportPreferences = mock(ExportPreferences.class, Answers.RETURNS_DEEP_STUBS);
    protected final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

    protected CommandLine commandLine;

    private ByteArrayOutputStream outWriter;
    private ByteArrayOutputStream errWriter;

    @BeforeEach()
    void setup() {
        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());
        when(exportPreferences.getCustomExporters()).thenReturn(FXCollections.emptyObservableList());

        when(preferences.getExportPreferences()).thenReturn(exportPreferences);
        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferences.getSearchPreferences()).thenReturn(new SearchPreferences(
                SearchDisplayMode.FILTER,
                EnumSet.noneOf(SearchFlags.class),
                false,
                false,
                0,
                0,
                0));

        ArgumentProcessor argumentProcessor = new ArgumentProcessor(preferences, entryTypesManager);
        commandLine = new CommandLine(argumentProcessor);

        outWriter = new ByteArrayOutputStream();
        errWriter = new ByteArrayOutputStream();
    }

    /**
     * Executes the configured {@link picocli.CommandLine} command while capturing its
     * standard output and error streams.
     *
     * <p>This method temporarily redirects {@code System.out} and {@code System.err} to
     * internal buffers during the command execution, allowing the captured output to be
     * retrieved later using {@link #getStandardOutput()} and {@link #getErrorOutput()}.</p>
     *
     * @param args the command line arguments to parse
     * @return the error code
     */
    int executeToLog(String... args) {
        var or = System.out;
        var orErr = System.err;

        System.setOut(new PrintStream(outWriter, true));
        System.setErr(new PrintStream(errWriter, true));

        int result = commandLine.execute(args);

        System.setOut(or);
        System.setErr(orErr);

        return result;
    }

    /**
     * Returns the captured standard output from the command line execution.
     *
     * @return The captured stdout string.
     */
    protected String getStandardOutput() {
        return outWriter.toString().replace("\r\n", "\n");
    }

    /**
     * Returns the captured error output from the command line execution.
     *
     * @return The captured stderr string.
     */
    protected String getErrorOutput() {
        return errWriter.toString().replace("\r\n", "\n");
    }

    /**
     * Gets class resource as fully qualified string.
     * Useful for scenarios where you want a resource as a command line argument
     * <p>
     * Throws a runtime exception if the resource URL cannot be turned into a URI.
     *
     * @param resourceName the resource name
     * @return the class resource as fully qualified string
     */
    String getClassResourceAsFullyQualifiedString(String resourceName) {
        return getClassResourceAsPath(resourceName).toAbsolutePath().toString();
    }

    /**
     * Gets class resource as a path.
     * <p>
     * Throws a runtime exception if the resource URL cannot be turned into a URI.
     *
     * @param resourceName the resource name
     * @return the class resource as path
     */
    Path getClassResourceAsPath(String resourceName) {
        try {
            return Path.of(Objects.requireNonNull(this.getClass().getResource(resourceName), "Could not find resource: " + resourceName).toURI())
                       .toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    "Wrong resource name %s for class %s".formatted(resourceName, this.getClass()), e
            );
        }
    }
}
