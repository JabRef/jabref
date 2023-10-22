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
import org.jabref.logic.formatter.casechanger.UnprotectTermsFormatter;
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

    private final Action unprotectSelectionActionInformation = new Action() {
        @Override
        public String getText() {
            return Localization.lang("Unprotect selection");
        }

        @Override
        public String getDescription() {
            return Localization.lang("Remove all {} in selected text");
        }
    };

    private class ProtectSelectionAction extends SimpleCommand {
        ProtectSelectionAction() {
            this.executable.bind(textInputControl.selectedTextProperty().isNotEmpty());
        }

        @Override
        public void execute() {
            String selectedText = textInputControl.getSelectedText();
            String firstStr = "{";
            String lastStr = "}";
            // If the selected text contains spaces at the beginning and end, then add spaces before or after the brackets
            if (selectedText.startsWith(" ")) {
                firstStr = " {";
            }
            if (selectedText.endsWith(" ")) {
                lastStr = "} ";
            }
            textInputControl.replaceSelection(firstStr + selectedText.strip() + lastStr);
        }
    }

    private class UnprotectSelectionAction extends SimpleCommand {

        public UnprotectSelectionAction() {
            this.executable.bind(textInputControl.selectedTextProperty().isNotEmpty());
        }

        @Override
        public void execute() {
            String selectedText = textInputControl.getSelectedText();
            String formattedString = new UnprotectTermsFormatter().format(selectedText);
            textInputControl.replaceSelection(formattedString);
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
            this.executable.bind(textInputControl.focusedProperty());
        }

        @Override
        public void execute() {
            // If no selected term, then add the word after or at the cursor
            if (textInputControl.getSelectedText().isEmpty()) {
                int beginIdx = textInputControl.getCaretPosition();
                int endIdx = textInputControl.getCaretPosition();
                String text = textInputControl.getText();
                // While the beginIdx > 0 and the previous char is not a space
                while (beginIdx > 0 && text.charAt(beginIdx - 1) != ' ') {
                    --beginIdx;
                }
                // While the endIdx < length and the current char is not a space
                while (endIdx < text.length() && text.charAt(endIdx) != ' ') {
                    ++endIdx;
                }
                list.addProtectedTerm(text.substring(beginIdx, endIdx));
            } else {
                // Remove leading and trailing whitespaces
                list.addProtectedTerm(textInputControl.getSelectedText().strip());
            }
        }
    }

    public ProtectedTermsMenu(final TextInputControl textInputControl) {
        super(Localization.lang("Protect terms"));
        this.textInputControl = textInputControl;

        getItems().addAll(factory.createMenuItem(protectSelectionActionInformation, new ProtectSelectionAction()),
                getExternalFilesMenu(),
                new SeparatorMenuItem(),
                factory.createMenuItem(() -> Localization.lang("Format field"), new FormatFieldAction()),
                factory.createMenuItem(unprotectSelectionActionInformation, new UnprotectSelectionAction()));
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
