package org.jabref.languageserver.util;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public final class LspDiagnosticBuilder {

    private static final Range NULL_RANGE = new Range(new Position(0, 0), new Position(0, 0));
    private static final String DEFAULT_SOURCE = "JabRef";

    private String message;
    private DiagnosticSeverity severity = DiagnosticSeverity.Warning;
    private String source = DEFAULT_SOURCE;

    private String content;
    private BibEntry entry;
    private Field field;
    private Range explicitRange;

    private LspDiagnosticBuilder() { }

    public static LspDiagnosticBuilder create(String message) {
        return new LspDiagnosticBuilder().setMessage(message);
    }

    public LspDiagnosticBuilder setMessage(String message) {
        this.message = Objects.requireNonNull(message, "message must not be null");
        return this;
    }

    public LspDiagnosticBuilder setSeverity(DiagnosticSeverity severity) {
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        return this;
    }

    public LspDiagnosticBuilder setSource(String source) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        return this;
    }

    public LspDiagnosticBuilder setContent(String content) {
        this.content = content;
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

    public Diagnostic build() {
        Objects.requireNonNull(message, "message must be set");

        Range range = computeRange();
        return new Diagnostic(range, message, severity, source);
    }

    private Range computeRange() {
        if (explicitRange != null) {
            return explicitRange;
        }

        if (content == null || entry == null) {
            return NULL_RANGE;
        }

        if (field != null) {
            Optional<String> entryField = entry.getFieldOrAlias(field);
            if (entryField.isPresent()) {
                return findTextRange(content, entryField.get());
            }
            return entry.getCitationKey()
                        .map(key -> findTextRange(content, key))
                        .orElse(NULL_RANGE);
        }

        return entry.getCitationKey()
                    .map(key -> findTextRange(content, key))
                    .orElse(NULL_RANGE);
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
