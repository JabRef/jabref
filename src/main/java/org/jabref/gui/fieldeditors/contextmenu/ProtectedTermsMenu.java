package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;

import org.jabref.gui.Globals;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;

class ProtectedTermsMenu extends Menu {

    private static final Formatter FORMATTER = new ProtectTermsFormatter(Globals.protectedTermsLoader);
    private final Menu externalFiles;
    private final TextInputControl opener;

    public ProtectedTermsMenu(final TextInputControl opener) {
        super(Localization.lang("Protect terms"));
        this.opener = opener;
        MenuItem protectItem = new MenuItem(Localization.lang("Add {} around selected text"));
        protectItem.setOnAction(event -> {
            String selectedText = opener.getSelectedText();
            if ((selectedText != null) && !selectedText.isEmpty()) {
                opener.replaceSelection("{" + selectedText + "}");
            }
        });

        MenuItem formatItem = new MenuItem(Localization.lang("Format field"));
        formatItem.setOnAction(event -> opener.setText(FORMATTER.format(opener.getText())));

        externalFiles = new Menu(Localization.lang("Add selected text to list"));
        updateFiles();

        this.getItems().add(protectItem);
        this.getItems().add(externalFiles);
        this.getItems().add(new SeparatorMenuItem());
        this.getItems().add(formatItem);
    }

    private void updateFiles() {
        externalFiles.getItems().clear();
        ProtectedTermsLoader loader = Globals.protectedTermsLoader;
        List<ProtectedTermsList> nonInternal = loader
                .getProtectedTermsLists().stream()
                .filter(list -> !list.isInternalList())
                .collect(Collectors.toList());
        for (ProtectedTermsList list : nonInternal) {
            MenuItem fileItem = new MenuItem(list.getDescription());
            fileItem.setOnAction(event -> {
                String selectedText = opener.getSelectedText();
                if ((selectedText != null) && !selectedText.isEmpty()) {
                    list.addProtectedTerm(selectedText);
                }
            });
            externalFiles.getItems().add(fileItem);
        }
        externalFiles.getItems().add(new SeparatorMenuItem());
    }
}
