package org.jabref.gui.duplicationFinder;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog.DuplicateResolverResult;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.mergeentries.MergeEntries;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DialogWindowState;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class DuplicateResolverDialog extends BaseDialog<DuplicateResolverResult> {

    private final BibDatabaseContext database;
    private final StateManager stateManager;

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

    private MergeEntries me;

    public DuplicateResolverDialog(BibEntry one, BibEntry two, DuplicateResolverType type, BibDatabaseContext database, StateManager stateManager) {
        this.setTitle(Localization.lang("Possible duplicate entries"));
        this.database = database;
        this.stateManager = stateManager;
        init(one, two, type);
    }

    private void init(BibEntry one, BibEntry two, DuplicateResolverType type) {

        HelpAction helpCommand = new HelpAction(HelpFile.FIND_DUPLICATES);
        ButtonType help = new ButtonType(Localization.lang("Help"), ButtonData.HELP);

        ButtonType cancel = ButtonType.CANCEL;
        ButtonType merge = new ButtonType(Localization.lang("Keep merged entry only"), ButtonData.APPLY);

        ButtonBar options = new ButtonBar();
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
                me = new MergeEntries(one, two);
                break;
            case INSPECTION:
                first = new ButtonType(Localization.lang("Remove old entry"), ButtonData.APPLY);
                second = new ButtonType(Localization.lang("Remove entry from import"), ButtonData.APPLY);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.APPLY);
                me = new MergeEntries(one, two, Localization.lang("Old entry"),
                        Localization.lang("From import"));
                break;
            case DUPLICATE_SEARCH_WITH_EXACT:
                first = new ButtonType(Localization.lang("Keep left"), ButtonData.APPLY);
                second = new ButtonType(Localization.lang("Keep right"), ButtonData.APPLY);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.APPLY);

                removeExactVisible = true;

                me = new MergeEntries(one, two);
                break;
            default:
                first = new ButtonType(Localization.lang("Import and remove old entry"), ButtonData.APPLY);
                second = new ButtonType(Localization.lang("Do not import entry"), ButtonData.APPLY);
                both = new ButtonType(Localization.lang("Import and keep old entry"), ButtonData.APPLY);
                me = new MergeEntries(one, two, Localization.lang("Old entry"),
                        Localization.lang("From import"));
                break;
        }
        if (removeExactVisible) {
            this.getDialogPane().getButtonTypes().add(removeExact);
        }

        this.getDialogPane().getButtonTypes().addAll(first, second, both, merge, cancel, help);

        // Retrieves the previous window state and sets the new dialog window size and position to match it
        DialogWindowState state = stateManager.getDialogWindowState(getClass().getSimpleName());
        if (state != null) {
            this.getDialogPane().setPrefSize(state.getWidth(), state.getHeight());
            this.setX(state.getX());
            this.setY(state.getY());
        }

        BorderPane borderPane = new BorderPane(me);
        borderPane.setBottom(options);

        this.setResultConverter(button -> {
            // Updates the window state on button press
            stateManager.setDialogWindowState(getClass().getSimpleName(), new DialogWindowState(this.getX(), this.getY(), this.getDialogPane().getHeight(), this.getDialogPane().getWidth()));

            if (button.equals(first)) {
                return DuplicateResolverResult.KEEP_LEFT;
            } else if (button.equals(second)) {
                return DuplicateResolverResult.KEEP_RIGHT;
            } else if (button.equals(both)) {
                return DuplicateResolverResult.KEEP_BOTH;
            } else if (button.equals(merge)) {
                return DuplicateResolverResult.KEEP_MERGE;
            } else if (button.equals(removeExact)) {
                return DuplicateResolverResult.AUTOREMOVE_EXACT;
            }
            return null;
        });

        getDialogPane().setContent(borderPane);
        Button helpButton = (Button) this.getDialogPane().lookupButton(help);
        helpButton.setOnAction(evt -> helpCommand.execute());
    }

    public BibEntry getMergedEntry() {
        return me.getMergeEntry();
    }
}
