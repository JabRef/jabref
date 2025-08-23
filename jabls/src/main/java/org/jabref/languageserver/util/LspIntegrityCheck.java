package org.jabref.languageserver.util;

import java.util.List;

import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.lsp4j.Diagnostic;

public class LspIntegrityCheck {

    private final CliPreferences cliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;

    public LspIntegrityCheck(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.cliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
    }

    public List<Diagnostic> check(BibDatabaseContext bibDatabaseContext, String content) {
        IntegrityCheck integrityCheck = new IntegrityCheck(
                bibDatabaseContext,
                cliPreferences.getFilePreferences(),
                cliPreferences.getCitationKeyPatternPreferences(),
                abbreviationRepository,
                true
        );

        return bibDatabaseContext.getEntries().stream().flatMap(entry -> integrityCheck.checkEntry(entry).stream().map(message -> {
            if (entry.getField(message.field()).isPresent()) {
                return LspDiagnosticUtil.createFieldDiagnostic(message.message(), message.field(), content, entry);
            } else {
                return LspDiagnosticUtil.createGeneralEntryDiagnostic(message.message(), content, entry);
            }
        })).toList();
    }
}
