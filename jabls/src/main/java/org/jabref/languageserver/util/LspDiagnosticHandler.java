package org.jabref.languageserver.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jabref.languageserver.ExtensionSettings;
import org.jabref.languageserver.LspClientHandler;
import org.jabref.logic.JabRefException;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LspDiagnosticHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspDiagnosticHandler.class);
    private static final int NO_VERSION = -1;

    private final CliPreferences jabRefCliPreferences;
    private final LspIntegrityCheck lspIntegrityCheck;
    private final LspConsistencyCheck lspConsistencyCheck;
    private final LspClientHandler clientHandler;
    private final Map<String, List<Diagnostic>> integrityDiagnosticsCache; // Maps file URIs to the corresponding list of integrity diagnostics
    private final Map<String, List<Diagnostic>> consistencyDiagnosticsCache; // Maps file URIs to the corresponding list of consistency diagnostics

    private LanguageClient client;

    public LspDiagnosticHandler(LspClientHandler clientHandler, CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.clientHandler = clientHandler;
        this.jabRefCliPreferences = cliPreferences;
        this.lspIntegrityCheck = new LspIntegrityCheck(cliPreferences, abbreviationRepository);
        this.lspConsistencyCheck = new LspConsistencyCheck();
        this.integrityDiagnosticsCache = new ConcurrentHashMap<>();
        this.consistencyDiagnosticsCache = new ConcurrentHashMap<>();
    }

    public void computeAndPublishDiagnostics(LanguageClient client, String uri, String content, Integer version) {
        List<Diagnostic> diagnostics = computeDiagnostics(content, uri);
        publishDiagnostics(client, uri, version, diagnostics);
    }

    public void publishDiagnostics(LanguageClient client, String uri, Integer version, List<Diagnostic> diagnostics) {
        PublishDiagnosticsParams params = new PublishDiagnosticsParams();
        params.setUri(uri);
        params.setVersion(version);
        params.setDiagnostics(diagnostics);
        client.publishDiagnostics(params);
    }

    public void publishDiagnostics(LanguageClient client, String uri, List<Diagnostic> diagnostics) {
        publishDiagnostics(client, uri, NO_VERSION, diagnostics);
    }

    private List<Diagnostic> computeDiagnostics(String content, String uri) {
        BibDatabaseContext bibDatabaseContext;
        try {
            bibDatabaseContext = BibDatabaseContext.of(content, jabRefCliPreferences.getImportFormatPreferences());
        } catch (JabRefException e) {
            Diagnostic parseDiagnostic = LspDiagnosticBuilder.create(Localization.lang(
                    "Failed to parse entries.\nThe following error was encountered:\n%0",
                    e.getMessage())).setSeverity(DiagnosticSeverity.Error).build();
            return List.of(parseDiagnostic);
        }

        if (clientHandler.getSettings().isIntegrityCheck()) {
            integrityDiagnosticsCache.put(uri, lspIntegrityCheck.check(bibDatabaseContext, content));
            LOGGER.debug("Cached integrity diagnostics for {}", uri);
        }

        if (clientHandler.getSettings().isConsistencyCheck()) {
            consistencyDiagnosticsCache.put(uri, lspConsistencyCheck.check(bibDatabaseContext, content));
            LOGGER.debug("Cached consistency diagnostics for {}", uri);
        }

        return getFinalDiagnosticsList(uri);
    }

    private List<Diagnostic> getFinalDiagnosticsList(String uri) {
        ExtensionSettings settings = clientHandler.getSettings();
        return Stream.concat(
                settings.isIntegrityCheck() ? integrityDiagnosticsCache.getOrDefault(uri, List.of()).stream() : Stream.empty(),
                settings.isConsistencyCheck() ? consistencyDiagnosticsCache.getOrDefault(uri, List.of()).stream() : Stream.empty()
        ).toList();
    }

    public void refreshDiagnostics(LanguageClient client) {
        Set<String> allUris = new HashSet<>(integrityDiagnosticsCache.keySet());
        allUris.addAll(consistencyDiagnosticsCache.keySet());
        allUris.forEach(uri -> {
            List<Diagnostic> diagnostics = getFinalDiagnosticsList(uri);
            publishDiagnostics(client, uri, diagnostics);
        });
    }
}
