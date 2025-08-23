package org.jabref.languageserver.util;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class LspDiagnosticUtil {

    private static final Range NULL_RANGE = new Range(new Position(0, 0), new Position(0, 0));
    private static final String DIAGNOSTIC_SOURCE = "JabRef";

    public static Diagnostic createFieldDiagnostic(String message, Field field, String content, BibEntry entry) {
        if (entry.getFieldOrAlias(field).isEmpty()) {
            return createGeneralEntryDiagnostic(message, content, entry);
        }
        return new Diagnostic(findTextRange(content, entry.getFieldOrAlias(field).get()), message, DiagnosticSeverity.Warning, DIAGNOSTIC_SOURCE);
    }

    public static Diagnostic createGeneralEntryDiagnostic(String message, String content, BibEntry entry) {
        if (entry.getCitationKey().isEmpty()) {
            return createGeneralDiagnostic(message);
        }
        return new Diagnostic(findTextRange(content, entry.getCitationKey().get()), message, DiagnosticSeverity.Warning, DIAGNOSTIC_SOURCE);
    }

    public static Diagnostic createGeneralDiagnostic(String message) {
        return new Diagnostic(NULL_RANGE, message, DiagnosticSeverity.Warning, DIAGNOSTIC_SOURCE);
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
