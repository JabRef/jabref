package org.jabref.languageserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

public class LspConsistencyCheck {

    public List<Diagnostic> check(BibDatabaseContext bibDatabaseContext, String content) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(bibDatabaseContext, (_, _) -> {
        });

        List<Field> allReportedFields = result.entryTypeToResultMap().values().stream()
                                              .flatMap(entryTypeResult -> entryTypeResult.fields().stream())
                                              .sorted(Comparator.comparing(Field::getName))
                                              .distinct()
                                              .toList();

        result.entryTypeToResultMap().forEach((entryType, entryTypeResult) -> {
            Optional<BibEntryType> bibEntryType = new BibEntryTypesManager().enrich(entryType, bibDatabaseContext.getMode());
            Set<Field> requiredFields = bibEntryType
                    .map(BibEntryType::getRequiredFields)
                    .stream()
                    .flatMap(Collection::stream)
                    .flatMap(orFields -> orFields.getFields().stream())
                    .collect(Collectors.toSet());

            entryTypeResult.sortedEntries().forEach(entry -> requiredFields.forEach(requiredField -> {
                if (entry.getFieldOrAlias(requiredField).isEmpty()) {
                    LspDiagnosticBuilder diagnosticBuilder = LspDiagnosticBuilder.create(Localization.lang("Required field \"%0\" is empty.", requiredField.getName()));
                    diagnosticBuilder.setSeverity(DiagnosticSeverity.Error);
                    diagnosticBuilder.setContent(content);
                    diagnosticBuilder.setEntry(entry);
                    diagnostics.add(diagnosticBuilder.build());
                }
            }));

            Set<Field> optionalFields = bibEntryType
                    .map(BibEntryType::getOptionalFields)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(BibField::field)
                    .filter(allReportedFields::contains)
                    .collect(Collectors.toSet());

            optionalFields.forEach(optionalField -> entryTypeResult.sortedEntries().forEach(entry -> {
                if (entry.getFieldOrAlias(optionalField).isEmpty()) {
                    LspDiagnosticBuilder diagnosticBuilder = LspDiagnosticBuilder.create(Localization.lang("Optional field \"%0\" is empty.", optionalField.getName()));
                    diagnosticBuilder.setContent(content);
                    diagnosticBuilder.setEntry(entry);
                    diagnostics.add(diagnosticBuilder.build());
                }
            }));
        });

        return diagnostics;
    }
}
