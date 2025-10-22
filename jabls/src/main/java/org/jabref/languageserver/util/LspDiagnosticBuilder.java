package org.jabref.languageserver.util;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class LspDiagnosticBuilder {

    private static final String DEFAULT_SOURCE = "JabRef";

    @Nullable
    private String message;
    private DiagnosticSeverity severity = DiagnosticSeverity.Warning;
    private String source = DEFAULT_SOURCE;

    @Nullable
    private BibEntry entry;
    @Nullable
    private ParserResult parserResult;
    @Nullable
    private Field field;
    @Nullable
    private Range explicitRange;

    private LspDiagnosticBuilder() {
    }

    public static LspDiagnosticBuilder create(ParserResult parserResult, String message) {
        return new LspDiagnosticBuilder().setMessage(message).setParserResult(parserResult);
    }

    public static LspDiagnosticBuilder create(String message) {
        return new LspDiagnosticBuilder().setMessage(message);
    }

    public LspDiagnosticBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public LspDiagnosticBuilder setSeverity(DiagnosticSeverity severity) {
        this.severity = severity;
        return this;
    }

    public LspDiagnosticBuilder setSource(String source) {
        this.source = source;
        return this;
    }

    public LspDiagnosticBuilder setEntry(BibEntry entry) {
        this.entry = entry;
        return this;
    }

    public LspDiagnosticBuilder setField(Field field) {
        this.field = field;
        return this;
    }

    public LspDiagnosticBuilder setRange(Range range) {
        this.explicitRange = range;
        return this;
    }

    public LspDiagnosticBuilder setRange(ParserResult.Range range) {
        this.explicitRange = LspRangeUtil.convertToLspRange(range);
        return this;
    }

    public LspDiagnosticBuilder setParserResult(ParserResult parserResult) {
        this.parserResult = parserResult;
        return this;
    }

    public Diagnostic build() {
        Range range = explicitRange;
        if (explicitRange == null) {
            range = LspRangeUtil.convertToLspRange(computeRange());
        }
        return new Diagnostic(range, message, severity, source);
    }

    private ParserResult.Range computeRange() {
        if (parserResult == null || entry == null) {
            return ParserResult.Range.NULL_RANGE;
        }

        if (field == null) {
            return parserResult.getCompleteEntryIndicator(entry);
        }

        return parserResult.getFieldRange(entry, field);
    }
}
