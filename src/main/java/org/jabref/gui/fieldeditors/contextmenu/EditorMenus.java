package org.jabref.gui.fieldeditors.contextmenu;

import javafx.scene.control.*;
import org.jabref.gui.actions.CopyDoiUrlAction;
import org.jabref.logic.formatter.bibtexfields.CleanupURLFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class EditorMenus {

    public static List<MenuItem> getDefaultMenu(TextArea textArea) {
        List<MenuItem> menuItems = new ArrayList<>(5);
        menuItems.add(new CaseChangeMenu(textArea.textProperty()));
        menuItems.add(new ConversionMenu(textArea.textProperty()));
        menuItems.add(new SeparatorMenuItem());
        menuItems.add(new ProtectedTermsMenu(textArea));
        menuItems.add(new SeparatorMenuItem());
        return menuItems;
    }

    public static List<MenuItem> getNameMenu(TextArea textArea) {
        CustomMenuItem normalizeNames = new CustomMenuItem(new Label(Localization.lang("Normalize to BibTeX name format")));
        normalizeNames.setOnAction(event -> textArea.setText(new NormalizeNamesFormatter().format(textArea.getText())));
        Tooltip toolTip = new Tooltip(Localization.lang("If possible, normalize this list of names to conform to standard BibTeX name formatting"));
        Tooltip.install(normalizeNames.getContent(),toolTip);

        List<MenuItem> menuItems = new ArrayList<>(6);
        menuItems.add(normalizeNames);
        menuItems.addAll(getDefaultMenu(textArea));

        return menuItems;
    }

    public static List<MenuItem> getDOIMenu(TextArea textArea) {
        AbstractAction copyDoiUrlAction = new CopyDoiUrlAction(textArea);
        MenuItem copyDoiUrlMenuItem = new MenuItem((String) copyDoiUrlAction.getValue(Action.NAME));
        copyDoiUrlMenuItem.setOnAction(event -> copyDoiUrlAction.actionPerformed(null));

        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(copyDoiUrlMenuItem);
        menuItems.add(new SeparatorMenuItem());
        return menuItems;
    }

    public static List<MenuItem> getCleanupURLMenu(TextArea textArea){

        CustomMenuItem cleanupURL = new CustomMenuItem(new Label(Localization.lang("Cleanup URL Link")));
        //cleanupURL.setDisable(true);
        cleanupURL.setOnAction(event -> textArea.setText(new CleanupURLFormatter().format(textArea.getText())));

        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(cleanupURL);
        menuItems.add(new SeparatorMenuItem());
        menuItems.addAll(getDefaultMenu(textArea));

        return menuItems;
    }
}
