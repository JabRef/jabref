package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

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
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.model.cleanup.Formatter;

class ProtectedTermsMenu extends Menu {

    private static final Formatter FORMATTER = new ProtectTermsFormatter(Globals.protectedTermsLoader);
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
        ProtectedTermsLoader loader = Globals.protectedTermsLoader;
        List<ProtectedTermsList> nonInternal = loader.getProtectedTermsLists().stream()
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
        MenuItem addToNewFileItem = new MenuItem(Localization.lang("New") + "...");
        addToNewFileItem.setOnAction(event -> {
            NewProtectedTermsFileDialog dialog = new NewProtectedTermsFileDialog(JabRefGUI.getMainFrame(),
                    loader);

            SwingUtilities.invokeLater(() -> {
                dialog.setVisible(true);

                if (dialog.isOKPressed()) {
                    // Update preferences with new list
                    Globals.prefs.setProtectedTermsPreferences(loader);
                    this.updateFiles();
                }
            });
        });
        externalFiles.getItems().add(addToNewFileItem);
    }
}
