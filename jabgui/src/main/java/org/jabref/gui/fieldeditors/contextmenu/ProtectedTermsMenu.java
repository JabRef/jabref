package org.jabref.gui.fieldeditors.contextmenu;

import java.util.Optional;

import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;

import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.formatter.casechanger.UnprotectTermsFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;

class ProtectedTermsMenu extends Menu {

    private final Formatter formatter;
    private final TextInputControl textInputControl;
    private final ActionFactory factory = new ActionFactory();

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
            textInputControl.setText(formatter.format(textInputControl.getText()));
        }
    }

    private class AddToProtectedTermsAction extends SimpleCommand {
        ProtectedTermsList list;

        public AddToProtectedTermsAction(@NonNull ProtectedTermsList list) {
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

    private static final class DisabledCommand extends SimpleCommand {
        DisabledCommand() {
            this.executable.set(false);
        }

        @Override
        public void execute() {
            // Disabled placeholder item
        }
    }

    public ProtectedTermsMenu(final TextInputControl textInputControl) {
        super(Localization.lang("Protect terms"));
        this.textInputControl = textInputControl;
        ProtectedTermsLoader loader = Injector.instantiateModelOrService(ProtectedTermsLoader.class);
        this.formatter = new ProtectTermsFormatter(loader);

        getItems().addAll(factory.createMenuItem(protectSelectionActionInformation, new ProtectSelectionAction()),
                getExternalFilesMenu(loader),
                new SeparatorMenuItem(),
                factory.createMenuItem(() -> Localization.lang("Format field"), new FormatFieldAction()),
                factory.createMenuItem(unprotectSelectionActionInformation, new UnprotectSelectionAction()));
    }

    private Menu getExternalFilesMenu(ProtectedTermsLoader loader) {
        Menu protectedTermsMenu = factory.createSubMenu(() -> Localization.lang("Add selected text to list"));
        loader.getProtectedTermsLists().stream()
              .filter(list -> !list.isInternalList())
              .forEach(list -> protectedTermsMenu.getItems().add(
                      factory.createMenuItem(list::getDescription, new AddToProtectedTermsAction(list))));

        if (protectedTermsMenu.getItems().isEmpty()) {
            Action noListEnabledAction = new Action() {
                @Override
                public String getText() {
                    return Localization.lang("No list enabled");
                }

                @Override
                public String getDescription() {
                    return Localization.lang("Please create a user-defined list (Preferences > Protected Terms Files)");
                }
            };
            protectedTermsMenu.getItems().add(factory.createMenuItem(noListEnabledAction, new DisabledCommand()));
        }

        return protectedTermsMenu;
    }
}
