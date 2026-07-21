package org.jabref.gui.preferences.linkedfiles;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

public class LinkedFilesTab extends AbstractFormTabView<LinkedFilesTabViewModel> {

    public LinkedFilesTab() {
        this.viewModel = new LinkedFilesTabViewModel(dialogService, preferences);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Linked files");
    }

    private void buildView() {
        getChildren().add(form()
                .title(Localization.lang("Linked files"))

                .section(Localization.lang("File directory"))
                .radioGroup(directory -> directory
                        .radioWithBrowse(Localization.lang("Main file directory"),
                                viewModel.useMainFileDirectoryProperty(), viewModel.mainFileDirectoryProperty(), viewModel::mainFileDirBrowse,
                                path -> path.disableWhen(viewModel.useBibLocationAsPrimaryProperty())
                                            .validate(viewModel.mainFileDirValidationStatus()))
                        .radio(Localization.lang("Search and store files relative to library file location"),
                                viewModel.useBibLocationAsPrimaryProperty(),
                                relative -> relative.tooltip(Localization.lang("When downloading files, or moving linked files to the file directory, use the bib file location."))))

                .section(Localization.lang("Open file explorer"))
                .radioGroup(explorer -> explorer
                        .radio(Localization.lang("Open file explorer in files directory"), viewModel.openFileExplorerInFilesDirectoryProperty())
                        .radio(Localization.lang("Open file explorer in last opened directory"), viewModel.openFileExplorerInLastDirectoryProperty()))

                .section(Localization.lang("Autolink files"))
                .radioGroup(autolink -> autolink
                        .radio(Localization.lang("Autolink files with names starting with the citation key"), viewModel.autolinkFileStartsBibtexProperty())
                        .radio(Localization.lang("Autolink only files that match the citation key"), viewModel.autolinkFileExactBibtexProperty())
                        .radioWithField(Localization.lang("Use regular expression search"),
                                viewModel.autolinkUseRegexProperty(), viewModel.autolinkRegexKeyProperty(),
                                regex -> regex.help(StandardActions.HELP_REGEX_SEARCH, HelpFile.REGEX_SEARCH)))

                .section(Localization.lang("Fulltext Index"))
                .checkbox(Localization.lang("Automatically index all linked files for fulltext search"), viewModel.fulltextIndexProperty())

                .section(Localization.lang("Linked file name conventions"))
                .checkbox(Localization.lang("Auto rename files if entry changes"), viewModel.autoRenameFilesOnChangeProperty())
                .stringCombo(Localization.lang("Filename format pattern"),
                        viewModel.defaultFileNamePatternsProperty(), viewModel.fileNamePatternProperty(), true, Localization.lang("Choose pattern"))
                .stringField(Localization.lang("File directory pattern"), viewModel.fileDirectoryPatternProperty())

                .section(Localization.lang("Attached files"))
                .checkbox(Localization.lang("Show confirmation dialog when deleting attached files"), viewModel.confirmLinkedFileDeleteProperty())
                .checkbox(Localization.lang("Move deleted files to trash (instead of deleting them)"), viewModel.moveToTrashProperty(),
                        trash -> trash.disabled(!NativeDesktop.get().moveToTrashSupported()))
                .checkbox(Localization.lang("Update linked file paths during entry transfer if the files are accessible"), viewModel.adjustLinkedFilesOnTransferProperty())
                .checkbox(Localization.lang("Copy linked files on entry transfer when they would otherwise be inaccessible"), viewModel.copyLinkedFilesOnTransferProperty(),
                        copy -> copy.disableWhen(viewModel.adjustLinkedFilesOnTransferProperty().not()))
                .checkbox(Localization.lang("Move linked files on entry transfer when they would otherwise be inaccessible"), viewModel.moveFilesOnTransferProperty(),
                        move -> move.disableWhen(viewModel.adjustLinkedFilesOnTransferProperty().not()))

                .build());
    }
}
