package org.jabref.gui.fieldeditors.contextmenu;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.protectedterms.NewProtectedTermsFileDialog;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;

class ProtectedTermsMenu extends Menu {

    private static final ProtectTermsFormatter FORMATTER = new ProtectTermsFormatter(Globals.protectedTermsLoader);
    private final Menu externalFiles;
    private final TextArea opener;

    public ProtectedTermsMenu(TextArea opener) {
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
        for (ProtectedTermsList list : Globals.protectedTermsLoader.getProtectedTermsLists()) {
            if (!list.isInternalList()) {
                MenuItem fileItem = new MenuItem(list.getDescription());
                fileItem.setOnAction(event -> {
                    String selectedText = opener.getSelectedText();
                    if ((selectedText != null) && !selectedText.isEmpty()) {
                        list.addProtectedTerm(selectedText);
                    }
                });
                externalFiles.getItems().add(fileItem);
            }
        }
        externalFiles.getItems().add(new SeparatorMenuItem());
        MenuItem addToNewFileItem = new MenuItem(Localization.lang("New") + "...");
        addToNewFileItem.setOnAction(event -> {
            NewProtectedTermsFileDialog dialog = new NewProtectedTermsFileDialog(JabRefGUI.getMainFrame(),
                    Globals.protectedTermsLoader);
            dialog.setVisible(true);
            if (dialog.isOKPressed()) {
                // Update preferences with new list
                Globals.prefs.setProtectedTermsPreferences(Globals.protectedTermsLoader);
            }
        });
        externalFiles.getItems().add(addToNewFileItem);
    }
}
