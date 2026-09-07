package org.jabref.gui.preferences.openoffice;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OpenOfficeTab extends AbstractPreferenceTabView<OpenOfficeTabViewModel> {

    public OpenOfficeTab() {
        JournalAbbreviationRepository journalAbbreviationRepository =
                Injector.instantiateModelOrService(JournalAbbreviationRepository.class);

        OpenOfficePreferences openOfficePreferences =
                preferences.getOpenOfficePreferences(journalAbbreviationRepository);

        this.viewModel = new OpenOfficeTabViewModel(
                dialogService,
                preferences.getFilePreferences(),
                openOfficePreferences,
                taskExecutor);

        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("OpenOffice/LibreOffice");
    }

    private void buildView() {
        setContent(form()
                .section(Localization.lang("Pandoc"), pandoc -> pandoc
                        .custom(buildPandocPathRow()))
                .section(Localization.lang("Zotero"), zotero -> zotero
                        .checkbox(Localization.lang("Zotero compatibility mode"), viewModel.zoteroCompatibilityModeProperty(), mode -> mode
                                .disableWhen(viewModel.zoteroCompatibilityModeDisabledProperty())))
                .build());
    }

    private Node buildPandocPathRow() {
        TextField pandocPath = new TextField();
        pandocPath.setPromptText(Localization.lang("Path to pandoc"));
        pandocPath.textProperty().bindBidirectional(viewModel.pandocPathProperty());

        HBox.setHgrow(pandocPath, Priority.ALWAYS);

        Button browseButton = ControlHelper.narrowIconButton(
                IconTheme.JabRefIcons.FOLDER,
                Localization.lang("Browse pandoc path"),
                viewModel::browsePandocPath);

        Button autoDetectButton = ControlHelper.narrowIconButton(
                IconTheme.JabRefIcons.SEARCH,
                Localization.lang("Auto-detect pandoc path"),
                viewModel::autoDetectPandocPath);

        HBox row = new HBox(
                8.0,
                new Label(Localization.lang("Pandoc path")),
                pandocPath,
                browseButton,
                autoDetectButton);

        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }
}
