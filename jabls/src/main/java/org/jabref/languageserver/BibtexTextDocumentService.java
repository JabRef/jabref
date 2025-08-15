package org.jabref.languageserver;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

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
    private static final String DIAGNOSTIC_SOURCE = "JabRef";
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
            bibDatabaseContext = new BibtexParser(jabRefCliPreferences.getImportFormatPreferences()).parse(StringReader.of(content)).getDatabaseContext();
        } catch (Exception e) {
            Diagnostic parseDiagnostic = new Diagnostic(
                    NULL_RANGE,
                    "Parse error: " + e.getMessage(),
                    DiagnosticSeverity.Error,
                    DIAGNOSTIC_SOURCE
            );
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of(parseDiagnostic), version));
            return;
        }

        List<Diagnostic> diagnostics = new ArrayList<>();

        diagnostics.addAll(integrityCheck(bibDatabaseContext, content));
        diagnostics.addAll(consistencyCheck(bibDatabaseContext, content));

        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics, version));
    }

    private List<Diagnostic> consistencyCheck(BibDatabaseContext bibDatabaseContext, String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(bibDatabaseContext.getEntries(), (_, _) -> { });

        result.entryTypeToResultMap().forEach((entryType, entryTypeResult) -> {
            Optional<BibEntryType> bibEntryType = new BibEntryTypesManager().enrich(entryType, bibDatabaseContext.getMode());
            Set<Field> requiredFields = bibEntryType
                    .map(BibEntryType::getRequiredFields)
                    .stream()
                    .flatMap(Collection::stream)
                    .flatMap(orFields -> orFields.getFields().stream())
                    .collect(Collectors.toSet());

            entryTypeResult.sortedEntries().forEach(entry -> {
                requiredFields.forEach(requiredField -> {
                    if (!entry.hasField(requiredField)) {
                        diagnostics.add(createGeneralDiagnostic(Localization.lang("required field '%0' is missing", requiredField.getName()), content, entry));
                    }
                });
            });
        });

        return diagnostics;
    }

    private List<Diagnostic> integrityCheck(BibDatabaseContext bibDatabaseContext, String content) {
        IntegrityCheck integrityCheck = new IntegrityCheck(
                bibDatabaseContext,
                jabRefCliPreferences.getFilePreferences(),
                jabRefCliPreferences.getCitationKeyPatternPreferences(),
                abbreviationRepository,
                true
        );

        return bibDatabaseContext.getEntries().stream().flatMap(entry -> integrityCheck.checkEntry(entry).stream().map(message -> {
            if (entry.getField(message.field()).isPresent()) {
                return createFieldDiagnostic(message.message(), message.field(), content, entry);
            } else {
                return createGeneralDiagnostic(message.message(), content, entry);
            }
        })).toList();
    }

    private Diagnostic createFieldDiagnostic(String message, Field field, String content, BibEntry entry) {
        return new Diagnostic(findTextRange(content, entry.getField(field).get()), message, DiagnosticSeverity.Warning, DIAGNOSTIC_SOURCE);
    }

    private Diagnostic createGeneralDiagnostic(String message, String content, BibEntry entry) {
        return new Diagnostic(findTextRange(content, entry.getCitationKey().orElse("")), message, DiagnosticSeverity.Warning, DIAGNOSTIC_SOURCE);
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
