package org.jabref.http.server;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;

import org.jabref.http.dto.GsonFactory;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.preferences.BibEntryPreferences;
import org.jabref.preferences.GuiPreferences;
import org.jabref.preferences.PreferencesService;

import com.google.gson.Gson;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Abstract test class to
 * <ul>
 *   <li>Initialize the JCL to SLF4J bridge</li>
 *   <li>Provide injection capabilities of JabRef's preferences and Gson<./li>
 * </ul>
 * <p>More information on testing with Jersey is available at <a href="https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/test-framework.html">the Jersey's testing documentation</a></p>.
 */
abstract class ServerTest extends JerseyTest {

    private static PreferencesService preferencesService;
    private static GuiPreferences guiPreferences;

    @BeforeAll
    static void installLoggingBridge() {
        // Grizzly uses java.commons.logging, but we use TinyLog
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        initializePreferencesService();
    }

    protected void addGsonToResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new GsonFactory().provide()).to(Gson.class).ranked(2);
            }
        });
    }

    protected void addPreferencesToResourceConfig(ResourceConfig resourceConfig) {
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(preferencesService).to(PreferencesService.class).ranked(2);
            }
        });
    }

    protected void setAvailableLibraries(EnumSet<TestBibFile> files) {
        when(guiPreferences.getLastFilesOpened()).thenReturn(
                FXCollections.observableArrayList(
                        files.stream()
                             .map(file -> file.path)
                             .collect(Collectors.toList())));
    }

    private static void initializePreferencesService() {
        preferencesService = mock(PreferencesService.class);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(preferencesService.getImportFormatPreferences()).thenReturn(importFormatPreferences);

        BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
        when(importFormatPreferences.bibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');

        FieldPreferences fieldWriterPreferences = mock(FieldPreferences.class);
        when(preferencesService.getFieldPreferences()).thenReturn(fieldWriterPreferences);
        when(fieldWriterPreferences.shouldResolveStrings()).thenReturn(false);

        // defaults are in {@link org.jabref.preferences.JabRefPreferences.NON_WRAPPABLE_FIELDS}
        FieldPreferences fieldContentFormatterPreferences = new FieldPreferences(false, List.of(), List.of());
        // used twice, once for reading and once for writing
        when(importFormatPreferences.fieldPreferences()).thenReturn(fieldContentFormatterPreferences);

        guiPreferences = mock(GuiPreferences.class);
        when(preferencesService.getGuiPreferences()).thenReturn(guiPreferences);

        when(guiPreferences.getLastFilesOpened()).thenReturn(FXCollections.observableArrayList(TestBibFile.GENERAL_SERVER_TEST.path));
    }
}
