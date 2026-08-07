package org.jabref.gui.relatedwork;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.relatedwork.RelatedWorkMatchResult;
import org.jabref.logic.relatedwork.RelatedWorkMatcher;
import org.jabref.logic.relatedwork.RelatedWorkReferenceResolver;
import org.jabref.logic.relatedwork.RelatedWorkTextParser;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;

public class RelatedWorkDialogViewModel extends AbstractViewModel {

    private final BibDatabaseContext databaseContext;
    private final BibEntry sourceEntry;
    private final LinkedFile linkedPDFFile;
    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final RelatedWorkMatcher relatedWorkMatcher;

    private final StringProperty sourceEntryCitationKey = new SimpleStringProperty("");
    private final StringProperty linkedPDFFileName = new SimpleStringProperty("");
    private final StringProperty userName = new SimpleStringProperty("");
    private final StringProperty relatedWorkText = new SimpleStringProperty("");
    private final BooleanExpression parseDisabled;

    public RelatedWorkDialogViewModel(BibDatabaseContext databaseContext,
                                      BibEntry sourceEntry,
                                      LinkedFile linkedPDFFile,
                                      String sourceEntryCitationKey,
                                      DialogService dialogService,
                                      CliPreferences preferences,
                                      BibEntryTypesManager entryTypesManager) {
        this.databaseContext = databaseContext;
        this.sourceEntry = sourceEntry;
        this.linkedPDFFile = linkedPDFFile;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.relatedWorkMatcher = new RelatedWorkMatcher(
                new RelatedWorkTextParser(),
                new RelatedWorkReferenceResolver(),
                new DuplicateCheck(entryTypesManager)
        );

        this.sourceEntryCitationKey.set(sourceEntryCitationKey);
        this.linkedPDFFileName.set(linkedPDFFile.getFileName().orElse(linkedPDFFile.getLink()));
        this.userName.set(preferences.getOwnerPreferences().getDefaultOwner());

        parseDisabled = Bindings.createBooleanBinding(
                () -> StringUtil.isBlank(userName.get()) || StringUtil.isBlank(relatedWorkText.getValue()),
                userName,
                relatedWorkText
        );
    }

    public StringProperty sourceEntryCitationKeyProperty() {
        return sourceEntryCitationKey;
    }

    public StringProperty userNameProperty() {
        return userName;
    }

    public StringProperty linkedPDFFileProperty() {
        return linkedPDFFileName;
    }

    public StringProperty relatedWorkTextProperty() {
        return relatedWorkText;
    }

    public BooleanExpression parseDisabledProperty() {
        return parseDisabled;
    }

    public Optional<List<RelatedWorkMatchResult>> matchRelatedWork() {
        String text = relatedWorkText.get();

        try {
            List<RelatedWorkMatchResult> matchResults = relatedWorkMatcher.matchRelatedWork(
                    databaseContext,
                    sourceEntry,
                    linkedPDFFile,
                    text,
                    preferences.getFilePreferences()
            );

            if (matchResults.isEmpty()) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Insert related work text"),
                        Localization.lang("No citations were found in the related work text.")
                );
                return Optional.empty();
            }

            List<RelatedWorkMatchResult> matchedResults = matchResults.stream()
                                                                      .filter(RelatedWorkMatchResult::hasMatchedLibraryEntry)
                                                                      .toList();
            if (matchedResults.isEmpty()) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Insert related work text"),
                        Localization.lang("No matching references were found.")
                );
                return Optional.empty();
            }

            return Optional.of(matchedResults);
        } catch (IOException exception) {
            dialogService.showErrorDialogAndWait(exception);
            return Optional.empty();
        }
    }
}
