package org.jabref.languageserver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

public class BibtexTextDocumentService implements TextDocumentService {

    private static final Range NULL_RANGE = new Range(new Position(0, 0), new Position(0, 0));
    private final CliPreferences jabRefCliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;
    private LanguageClient client;

    public BibtexTextDocumentService(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.jabRefCliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
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
            Diagnostic parseDiagnostic = new Diagnostic(
                    NULL_RANGE,
                    "Parse error: " + e.getMessage(),
                    DiagnosticSeverity.Error,
                    "JabRef"
            );
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of(parseDiagnostic), version));
            return;
        }

        IntegrityCheck integrityCheck = new IntegrityCheck(
                bibDatabaseContext,
                jabRefCliPreferences.getFilePreferences(),
                jabRefCliPreferences.getCitationKeyPatternPreferences(),
                abbreviationRepository,
                true
        );

        List<Diagnostic> diagnostics = bibDatabaseContext.getEntries().stream()
                                                         .flatMap(entry -> integrityCheck.checkEntry(entry).stream()
                                                                                         .map(message -> new Diagnostic(
                                                                                                 findTextRange(content, entry.getParsedSerialization()),
                                                                                                 message.message(),
                                                                                                 DiagnosticSeverity.Warning,
                                                                                                 "JabRef"
                                                                                         ))
                                                         )
                                                         .toList();

        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics, version));
    }

    private static Range findTextRange(String content, String searchText) {
        int startOffset = content.indexOf(searchText);
        if (startOffset == -1) {
            return NULL_RANGE;
        }
        int endOffset = startOffset + searchText.length();
        return new Range(offsetToPosition(content, startOffset), offsetToPosition(content, endOffset));
    }

    private static Position offsetToPosition(String content, int offset) {
        int line = 0;
        int col = 0;
        for (int i = 0; i < offset; i++) {
            if (content.charAt(i) == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }
        return new Position(line, col);
    }
}
