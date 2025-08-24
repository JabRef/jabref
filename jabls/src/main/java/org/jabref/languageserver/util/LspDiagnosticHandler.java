package org.jabref.languageserver.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jabref.languageserver.ExtensionSettings;
import org.jabref.languageserver.LspClientHandler;
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

    private final CliPreferences jabRefCliPreferences;
    private final LspIntegrityCheck lspIntegrityCheck;
    private final LspConsistencyCheck lspConsistencyCheck;
    private final LspClientHandler clientHandler;

    private LanguageClient client;

    private final Map<String, List<Diagnostic>> integrityDiagnosticsCache;
    private final Map<String, List<Diagnostic>> consistencyDiagnosticsCache;

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
        publishDiagnostics(client, uri, -1, diagnostics);
    }

    private List<Diagnostic> computeDiagnostics(String content, String uri) {
        BibDatabaseContext bibDatabaseContext;
        try {
            bibDatabaseContext = BibDatabaseContext.of(content, jabRefCliPreferences.getImportFormatPreferences());
        } catch (Exception e) {
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
        for (String uri : Stream.concat(integrityDiagnosticsCache.keySet().stream(), consistencyDiagnosticsCache.keySet().stream()).distinct().toList()) {
            List<Diagnostic> diagnostics = getFinalDiagnosticsList(uri);
            publishDiagnostics(client, uri, diagnostics);
        }
    }
}
