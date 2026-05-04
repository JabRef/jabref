package org.jabref.languageserver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.languageserver.ExtensionSettings;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.Sets;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

public class LspConsistencyCheck {

    private final ExtensionSettings settings;

    public LspConsistencyCheck(ExtensionSettings settings) {
        this.settings = settings;
    }

    public List<Diagnostic> check(ParserResult parserResult, BibEntryTypesManager bibEntryTypesManager) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(parserResult.getDatabaseContext(), bibEntryTypesManager, (_, _) -> {
        });

        Set<Field> allReportedFields = result.entryTypeToResultMap().values().stream().flatMap(entryTypeResult -> entryTypeResult.fields().stream()).collect(Collectors.toUnmodifiableSet());

        result.entryTypeToResultMap().forEach((entryType, entryTypeResult) -> {
            Optional<BibEntryType> bibEntryType = new BibEntryTypesManager().enrich(entryType, parserResult.getDatabaseContext().getMode());
            Set<Field> requiredFields = bibEntryType.map(BibEntryType::getRequiredFields)
                                                    .stream()
                                                    .flatMap(Collection::stream)
                                                    .flatMap(orFields -> orFields.getFields().stream())
                                                    .collect(Collectors.toUnmodifiableSet());

            if (settings.isConsistencyCheckRequired()) {
                entryTypeResult.sortedEntries().forEach(entry -> requiredFields.forEach(requiredField -> {
                    if (entry.getFieldOrAlias(requiredField).isEmpty()) {
                        LspDiagnosticBuilder diagnosticBuilder = LspDiagnosticBuilder.create(parserResult, Localization.lang("Required field \"%0\" is empty.", requiredField.getName()));
                        diagnosticBuilder.setSeverity(DiagnosticSeverity.Error);
                        diagnosticBuilder.setField(requiredField);
                        diagnosticBuilder.setEntry(entry);
                        diagnostics.add(diagnosticBuilder.build());
                    }
                }));
            }

            Set<Field> optionalFields = bibEntryType.map(BibEntryType::getOptionalFields)
                                                    .stream()
                                                    .flatMap(Collection::stream)
                                                    .map(BibField::field)
                                                    .filter(allReportedFields::contains)
                                                    .collect(Collectors.toUnmodifiableSet());

            if (settings.isConsistencyCheckOptional()) {
                optionalFields.forEach(optionalField -> entryTypeResult.sortedEntries().forEach(entry -> {
                    if (entry.getFieldOrAlias(optionalField).isEmpty()) {
                        LspDiagnosticBuilder diagnosticBuilder = LspDiagnosticBuilder.create(parserResult, Localization.lang("Optional field \"%0\" is empty.", optionalField.getName()));
                        diagnosticBuilder.setEntry(entry);
                        diagnosticBuilder.setField(optionalField);
                        diagnostics.add(diagnosticBuilder.build());
                    }
                }));
            }

            Set<Field> unknownFields = Sets.difference(allReportedFields, Sets.union(requiredFields, optionalFields));

            if (settings.isConsistencyCheckUnknown()) {
                unknownFields.forEach(unknownField -> entryTypeResult.sortedEntries().forEach(entry -> {
                    if (entry.getField(unknownField).isPresent()) {
                        LspDiagnosticBuilder diagnosticBuilder = LspDiagnosticBuilder.create(parserResult, Localization.lang("Unknown field \"%0\".", unknownField.getName()));
                        diagnosticBuilder.setEntry(entry);
                        diagnosticBuilder.setField(unknownField);
                        diagnostics.add(diagnosticBuilder.build());
                    }
                }));
            }
        });

        return diagnostics;
    }
}
