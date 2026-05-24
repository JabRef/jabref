package org.jabref.http.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.http.JabRefSrvStateManager;
import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.GlobalExceptionMapper;
import org.jabref.http.dto.GsonFactory;
import org.jabref.http.dto.GsonMessageBodyReader;
import org.jabref.http.dto.GsonMessageBodyWriter;
import org.jabref.http.server.cayw.format.FormatterService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.LastFilesOpenedPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.metadata.UserHostInfo;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.google.gson.Gson;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Abstract test class to
///
/// - Initialize the JCL to SLF4J bridge
/// - Provide injection capabilities of JabRef's preferences and Gson<./li>
///
/// More information on testing with Jersey is available at <a href="https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/test-framework.html">the Jersey's testing documentation</a>.
public abstract class ServerTest extends JerseyTest {

    private static CliPreferences preferences;

    /// Holds the libraries the next `configure()` will parse and pass to
    /// [JabRefSrvStateManager]. Tests mutate this before the test container starts.
    private static List<Path> filesToServe = List.of();

    @BeforeAll
    static void installLoggingBridge() {
        // Grizzly uses java.commons.logging, but we use TinyLog
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        initializePreferencesService();
    }

    @BeforeEach
    public void setUp() throws Exception {
        // The state manager bound in configure() (invoked from super.setUp()) snapshots
        // the file list at construction time, so it must be set first.
        filesToServe = List.of(TestBibFile.GENERAL_SERVER_TEST.path);
        super.setUp();
    }

    protected void addGuiBridgeToResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new JabRefSrvStateManager(preferences.getBibEntryPreferences(), parseFilesToServe())).to(SrvStateManager.class);
            }
        });
    }

    /// Parses every library currently in [#filesToServe] so the state manager owns the
    /// same [BibDatabaseContext] instances that resources will look up by id. Without
    /// this, [SrvStateManager#getSearchContext] is called with a re-parsed (and therefore
    /// unregistered) database and fires the lifecycle assertion.
    private static List<BibDatabaseContext> parseFilesToServe() {
        BibtexImporter importer = new BibtexImporter(preferences.getImportFormatPreferences(), new DummyFileUpdateMonitor());
        List<BibDatabaseContext> contexts = new ArrayList<>(filesToServe.size());
        for (Path file : filesToServe) {
            try {
                contexts.add(importer.importDatabase(file).getDatabaseContext());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not parse library " + file, e);
            }
        }
        return contexts;
    }

    protected void addGsonToResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new GsonFactory().provide()).to(Gson.class).ranked(2);
            }
        });
        resourceConfig.register(GsonMessageBodyReader.class);
        resourceConfig.register(GsonMessageBodyWriter.class);
    }

    protected void addFormatterServiceToResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new FormatterService()).to(FormatterService.class);
            }
        });
    }

    protected void addPreferencesToResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(preferences).to(CliPreferences.class).ranked(2);
            }
        });
    }

    /// Restarts the Jersey test container so the state manager is rebuilt with the new
    /// files. Necessary because the state manager snapshots the file list at
    /// `configure()` time (see [#parseFilesToServe()]).
    protected void setAvailableLibraries(EnumSet<TestBibFile> files) {
        try {
            tearDown();
            filesToServe = files.stream().map(file -> file.path).toList();
            setUp();
        } catch (Exception e) {
            throw new IllegalStateException("Could not restart test container with new files", e);
        }
    }

    private static void initializePreferencesService() {
        preferences = mock(CliPreferences.class);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);

        BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');

        FieldPreferences fieldWriterPreferences = mock(FieldPreferences.class);
        when(preferences.getFieldPreferences()).thenReturn(fieldWriterPreferences);
        when(fieldWriterPreferences.shouldResolveStrings()).thenReturn(false);

        // defaults are in {@link org.jabref.logic.preferences.JabRefPreferences.NON_WRAPPABLE_FIELDS}
        FieldPreferences fieldContentFormatterPreferences = new FieldPreferences(false, List.of(), List.of());
        // used twice, once for reading and once for writing
        when(importFormatPreferences.fieldPreferences()).thenReturn(fieldContentFormatterPreferences);

        LastFilesOpenedPreferences lastFilesOpenedPreferences = mock(LastFilesOpenedPreferences.class);
        when(preferences.getLastFilesOpenedPreferences()).thenReturn(lastFilesOpenedPreferences);
        when(lastFilesOpenedPreferences.getLastFilesOpened()).thenReturn(FXCollections.emptyObservableList());

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getUserAndHost()).thenReturn(new UserHostInfo("user", "host").getUserHostString());
        when(importFormatPreferences.filePreferences()).thenReturn(filePreferences);
    }

    protected void addGlobalExceptionMapperToResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(GlobalExceptionMapper.class);
    }
}
