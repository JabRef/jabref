package org.jabref.gui.preferences.citationkeypattern;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.commonfxcontrols.CitationKeyPatternsPanel;
import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;

public class CitationKeyPatternTab extends AbstractFormTabView<CitationKeyPatternTabViewModel> {

    private static final String KEY_PATTERNS_HELP_URL = "https://docs.jabref.org/setup/citationkeypatterns";
    private static final String REGEX_HELP_URL = KEY_PATTERNS_HELP_URL + "#replace-via-regular-expression";

    private final CitationKeyPatternsPanel keyPatternsPanel = new CitationKeyPatternsPanel();

    public CitationKeyPatternTab() {
        this.viewModel = new CitationKeyPatternTabViewModel(
                preferences.getCitationKeyPatternPreferences(),
                preferences.getImporterPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Citation key generator");
    }

    private void buildView() {
        getChildren().add(form()
                .title(Localization.lang("Citation key patterns"))

                .section(Localization.lang("General"), general -> general
                        .checkbox(Localization.lang("Overwrite existing keys"), viewModel.overwriteAllowProperty())
                        .checkbox(Localization.lang("Warn before overwriting existing keys"), viewModel.overwriteWarningProperty(),
                                warn -> warn.disableWhen(viewModel.overwriteAllowProperty().not())
                                            .styleClass("prefIndent"))
                        .checkbox(Localization.lang("Generate keys before saving (only for entries without a key)"), viewModel.generateOnSaveProperty())
                        .checkbox(Localization.lang("Generate new keys for imported entries (overwriting their default)"), viewModel.generateKeyOnImportProperty())

                        .label(Localization.lang("Letters after duplicate generated keys"))
                        .group(letters -> letters
                                .radioGroup(suffix -> suffix
                                        .radio(Localization.lang("Start on second duplicate key with letter A (a, b, ...)"), viewModel.letterStartAProperty())
                                        .radio(Localization.lang("Start on second duplicate key with letter B (b, c, ...)"), viewModel.letterStartBProperty())
                                        .radio(Localization.lang("Always add letter (a, b, ...) to generated keys"), viewModel.letterAlwaysAddProperty())),
                            indent -> indent.styleClass("prefIndent"))

                        .customField(Localization.lang("Replace (regular expression)"), buildRegexReplacementRow())
                        .stringField(Localization.lang("Remove the following characters:"), viewModel.unwantedCharactersProperty())
                        .checkbox(Localization.lang("Transliterate fields that are used for generating the citation key"), viewModel.transliterateFieldsForCitationKeyProperty()))

                .sectionWithHelp(Localization.lang("Key patterns"), KEY_PATTERNS_HELP_URL, patterns -> patterns
                        .label(Localization.lang("( Note: Press return to commit changes in the table! )"))
                        .custom(buildKeyPatternsRegion()))

                .build());
    }

    /// `[regex] by [replacement]` — a single labelled cell, so the two fields stay adjacent.
    private Node buildRegexReplacementRow() {
        TextField regex = new TextField();
        regex.textProperty().bindBidirectional(viewModel.keyPatternRegexProperty());
        HBox.setHgrow(regex, Priority.ALWAYS);

        TextField replacement = new TextField();
        replacement.textProperty().bindBidirectional(viewModel.keyPatternReplacementProperty());
        HBox.setHgrow(replacement, Priority.ALWAYS);

        HBox row = new HBox(4.0, regex, new Label(Localization.lang("by")), replacement, new HelpButton(REGEX_HELP_URL));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /// The pattern table with a "Reset All" button overlaid in its top-right corner.
    private Node buildKeyPatternsRegion() {
        keyPatternsPanel.patternListProperty().bindBidirectional(viewModel.patternListProperty());
        keyPatternsPanel.defaultKeyPatternProperty().bindBidirectional(viewModel.defaultKeyPatternProperty());
        keyPatternsPanel.setPrefHeight(180.0);
        AnchorPane.setLeftAnchor(keyPatternsPanel, 0.0);
        AnchorPane.setRightAnchor(keyPatternsPanel, 0.0);

        Button resetAll = new Button(Localization.lang("Reset All"));
        resetAll.setOnAction(_ -> keyPatternsPanel.resetAll());
        AnchorPane.setRightAnchor(resetAll, 0.0);
        AnchorPane.setTopAnchor(resetAll, 0.0);

        return new AnchorPane(keyPatternsPanel, resetAll);
    }

    @Override
    public void setValues() {
        viewModel.setValues();
        BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        keyPatternsPanel.setValues(
                entryTypesManager.getAllTypes(preferences.getLibraryPreferences().getDefaultBibDatabaseMode()),
                preferences.getCitationKeyPatternPreferences().getKeyPatterns());
    }
}
