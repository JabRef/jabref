package org.jabref.gui.fieldeditors.contextmenu;

import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;

class ProtectedTermsMenu extends Menu {

    private static final Formatter FORMATTER = new ProtectTermsFormatter(Globals.protectedTermsLoader);
    private final TextInputControl textInputControl;
    private final ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

    private final Action protectSelectionActionInformation = new Action() {
        @Override
        public String getText() {
            return Localization.lang("Protect selection");
        }

        @Override
        public Optional<JabRefIcon> getIcon() {
            return Optional.of(IconTheme.JabRefIcons.PROTECT_STRING);
        }

        @Override
        public String getDescription() {
            return Localization.lang("Add {} around selected text");
        }
    };

    private class ProtectSelectionAction extends SimpleCommand {
        ProtectSelectionAction() {
            this.executable.bind(textInputControl.selectedTextProperty().isNotEmpty());
        }

        @Override
        public void execute() {
            String selectedText = textInputControl.getSelectedText();
            textInputControl.replaceSelection("{" + selectedText + "}");
        }
    }

    private class FormatFieldAction extends SimpleCommand {
        FormatFieldAction() {
            this.executable.bind(textInputControl.textProperty().isNotEmpty());
        }

        @Override
        public void execute() {
            textInputControl.setText(FORMATTER.format(textInputControl.getText()));
        }
    }

    private class AddToProtectedTermsAction extends SimpleCommand {
        ProtectedTermsList list;

        public AddToProtectedTermsAction(ProtectedTermsList list) {
            Objects.requireNonNull(list);

            this.list = list;
            this.executable.bind(textInputControl.selectedTextProperty().isNotEmpty());
        }

        @Override
        public void execute() {
            list.addProtectedTerm(textInputControl.getSelectedText());
        }
    }

    public ProtectedTermsMenu(final TextInputControl textInputControl) {
        super(Localization.lang("Protect terms"));
        this.textInputControl = textInputControl;

        getItems().addAll(factory.createMenuItem(protectSelectionActionInformation, new ProtectSelectionAction()),
                getExternalFilesMenu(),
                new SeparatorMenuItem(),
                factory.createMenuItem(() -> Localization.lang("Format field"), new FormatFieldAction()));
    }

    private Menu getExternalFilesMenu() {
        Menu protectedTermsMenu = factory.createSubMenu(() -> Localization.lang("Add selected text to list"));

        Globals.protectedTermsLoader.getProtectedTermsLists().stream()
                                    .filter(list -> !list.isInternalList())
                                    .forEach(list -> protectedTermsMenu.getItems().add(
                                            factory.createMenuItem(list::getDescription, new AddToProtectedTermsAction(list))));

        if (protectedTermsMenu.getItems().isEmpty()) {
            MenuItem emptyItem = new MenuItem(Localization.lang("No list enabled"));
            emptyItem.setDisable(true);
            protectedTermsMenu.getItems().add(emptyItem);
        }

        return protectedTermsMenu;
    }
}
