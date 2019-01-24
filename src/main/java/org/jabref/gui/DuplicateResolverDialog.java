package org.jabref.gui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;

import org.jabref.Globals;
import org.jabref.gui.DuplicateResolverDialog.DuplicateResolverResult;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.mergeentries.MergeEntries;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class DuplicateResolverDialog extends BaseDialog<DuplicateResolverResult> {

    public enum DuplicateResolverType {
        DUPLICATE_SEARCH,
        IMPORT_CHECK,
        INSPECTION,
        DUPLICATE_SEARCH_WITH_EXACT
    }

    public enum DuplicateResolverResult {
        KEEP_BOTH,
        KEEP_LEFT,
        KEEP_RIGHT,
        AUTOREMOVE_EXACT,
        KEEP_MERGE,
        BREAK
    }

    ActionFactory factory = new ActionFactory(Globals.prefs.getKeyBindingRepository());

    HelpAction helpCommand = new HelpAction(HelpFile.FIND_DUPLICATES);
    ButtonType helpButton = new ButtonType(Localization.lang("Help"), ButtonData.HELP);
    private final ButtonType cancel = ButtonType.CANCEL;
    private final ButtonType merge = new ButtonType(Localization.lang("Keep merged entry only"), ButtonData.APPLY);
    private final JabRefFrame frame;
    private final ButtonBar options = new ButtonBar();
    private MergeEntries me;

    public DuplicateResolverDialog(JabRefFrame frame, BibEntry one, BibEntry two, DuplicateResolverType type) {
        this.frame = frame;
        this.setTitle(Localization.lang("Possible duplicate entries"));
        init(one, two, type);

    }

    private void init(BibEntry one, BibEntry two, DuplicateResolverType type) {

        ButtonType both;
        ButtonType second;
        ButtonType first;
        ButtonType removeExact = new ButtonType(Localization.lang("Automatically remove exact duplicates"), ButtonData.APPLY);
        boolean removeExactVisible = false;

        switch (type) {
            case DUPLICATE_SEARCH:
                first = new ButtonType(Localization.lang("Keep left"), ButtonData.APPLY);
                second = new ButtonType(Localization.lang("Keep right"), ButtonData.APPLY);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.APPLY);
                me = new MergeEntries(one, two, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
            case INSPECTION:
                first = new ButtonType(Localization.lang("Remove old entry"), ButtonData.APPLY);
                second = new ButtonType(Localization.lang("Remove entry from import"), ButtonData.APPLY);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.APPLY);
                me = new MergeEntries(one, two, Localization.lang("Old entry"),
                                      Localization.lang("From import"), frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
            case DUPLICATE_SEARCH_WITH_EXACT:
                first = new ButtonType(Localization.lang("Keep left"), ButtonData.APPLY);
                second = new ButtonType(Localization.lang("Keep right"), ButtonData.APPLY);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.APPLY);

                removeExactVisible = true;

                me = new MergeEntries(one, two, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
            default:
                first = new ButtonType(Localization.lang("Import and remove old entry"), ButtonData.APPLY);
                second = new ButtonType(Localization.lang("Do not import entry"), ButtonData.APPLY);
                both = new ButtonType(Localization.lang("Import and keep old entry"), ButtonData.APPLY);
                me = new MergeEntries(one, two, Localization.lang("Old entry"),
                                      Localization.lang("From import"), frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
        }
        if (removeExactVisible) {
            this.getDialogPane().getButtonTypes().add(removeExact);
        }

        this.getDialogPane().getButtonTypes().addAll(first, second, both, merge, cancel, helpButton);

        BorderPane borderPane = new BorderPane(me);
        borderPane.setBottom(options);

        this.setResultConverter(button -> {
            if (button.equals(first)) {
                return DuplicateResolverResult.KEEP_LEFT;
            }
            if (button.equals(second)) {
                return DuplicateResolverResult.KEEP_RIGHT;
            }
            if (button.equals(both)) {
                return DuplicateResolverResult.KEEP_BOTH;
            }
            if (button.equals(merge)) {
                return DuplicateResolverResult.KEEP_MERGE;
            }
            if (button.equals(removeExact)) {
                return DuplicateResolverResult.AUTOREMOVE_EXACT;
            }
            return DuplicateResolverResult.BREAK;
        });

        getDialogPane().setContent(borderPane);
    }

    public BibEntry getMergedEntry() {
        return me.getMergeEntry();
    }

}
