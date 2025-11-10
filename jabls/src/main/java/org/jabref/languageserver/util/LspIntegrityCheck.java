package org.jabref.languageserver.util;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;

import org.eclipse.lsp4j.Diagnostic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LspIntegrityCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(LspIntegrityCheck.class);
    private static final boolean ALLOW_INTEGER_EDITION = true;

    private final CliPreferences cliPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;

    public LspIntegrityCheck(CliPreferences cliPreferences, JournalAbbreviationRepository abbreviationRepository) {
        this.cliPreferences = cliPreferences;
        this.abbreviationRepository = abbreviationRepository;
    }

    public List<Diagnostic> check(ParserResult parserResult) {
        IntegrityCheck integrityCheck = new IntegrityCheck(
                parserResult.getDatabaseContext(),
                cliPreferences.getFilePreferences(),
                cliPreferences.getCitationKeyPatternPreferences(),
                abbreviationRepository,
                ALLOW_INTEGER_EDITION
        );

        return parserResult.getDatabaseContext().getEntries().stream().flatMap(entry -> {
            try {
                return integrityCheck.checkEntry(entry).stream().map(message -> {
                    if (entry.getFieldOrAlias(message.field()).isPresent()) {
                        return LspDiagnosticBuilder.create(parserResult, message.message()).setField(message.field()).setEntry(entry).build();
                    } else {
                        return LspDiagnosticBuilder.create(parserResult, message.message()).setEntry(entry).build();
                    }
                });
            } catch (NullPointerException nullPointerException) {
                LOGGER.error("Error while performing integrity check.", nullPointerException);
            }
            return Stream.of();
        }).toList();
    }
}
