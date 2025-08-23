package org.jabref.languageserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jabref.languageserver.util.LspConsistencyCheck;
import org.jabref.languageserver.util.LspDiagnosticUtil;
import org.jabref.languageserver.util.LspIntegrityCheck;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

public class BibtexTextDocumentService implements TextDocumentService {

    private final CliPreferences jabRefCliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final LspIntegrityCheck lspIntegrityCheck;
    private final LspConsistencyCheck lspConsistencyCheck;

    private LanguageClient client;
    private boolean checkConsistency = true;

    public BibtexTextDocumentService(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.jabRefCliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
        this.lspIntegrityCheck = new LspIntegrityCheck(cliPreferences, abbreviationRepository);
        this.lspConsistencyCheck = new LspConsistencyCheck();
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        handleDiagnostics(params.getTextDocument().getUri(), params.getTextDocument().getText(), params.getTextDocument().getVersion());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        handleDiagnostics(params.getTextDocument().getUri(), params.getContentChanges().getFirst().getText(), params.getTextDocument().getVersion());
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        client.publishDiagnostics(new PublishDiagnosticsParams(params.getTextDocument().getUri(), List.of()));
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return TextDocumentService.super.completion(position);
    }

    private void handleDiagnostics(String uri, String content, int version) {
        BibDatabaseContext bibDatabaseContext;
        try {
            bibDatabaseContext = BibDatabaseContext.of(content, jabRefCliPreferences.getImportFormatPreferences());
        } catch (Exception e) {
            Diagnostic parseDiagnostic = LspDiagnosticUtil.createGeneralDiagnostic(Localization.lang(
                    "Failed to parse entries.\nThe following error was encountered:\n%0",
                    e.getMessage()));
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of(parseDiagnostic), version));
            return;
        }

        List<Diagnostic> diagnostics = new ArrayList<>();

        diagnostics.addAll(lspIntegrityCheck.check(bibDatabaseContext, content));

        if (checkConsistency) {
            diagnostics.addAll(lspConsistencyCheck.check(bibDatabaseContext, content));
        }

        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics, version));
    }
}
