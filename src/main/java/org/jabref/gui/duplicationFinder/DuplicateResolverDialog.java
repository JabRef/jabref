package org.jabref.gui.duplicationFinder;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog.DuplicateResolverResult;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.mergeentries.newmergedialog.ThreeWayMergeView;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DialogWindowState;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class DuplicateResolverDialog extends BaseDialog<DuplicateResolverResult> {

    private final BibDatabaseContext database;
    private final StateManager stateManager;

    public enum DuplicateResolverType {
        DUPLICATE_SEARCH,
        IMPORT_CHECK,
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

    private ThreeWayMergeView threeWayMerge;
    private final DialogService dialogService;
    private final ActionFactory actionFactory;

    public DuplicateResolverDialog(BibEntry one, BibEntry two, DuplicateResolverType type, BibDatabaseContext database, StateManager stateManager, DialogService dialogService, PreferencesService prefs) {
        this.setTitle(Localization.lang("Possible duplicate entries"));
        this.database = database;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.actionFactory = new ActionFactory(prefs.getKeyBindingRepository());
        init(one, two, type);
    }

    private void init(BibEntry one, BibEntry two, DuplicateResolverType type) {
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        this.setX(primaryScreenBounds.getMinX());
        this.setY(primaryScreenBounds.getMinY());
        this.setWidth(primaryScreenBounds.getWidth());
        this.setHeight(primaryScreenBounds.getHeight());

        ButtonType cancel = ButtonType.CANCEL;
        ButtonType merge = new ButtonType(Localization.lang("Keep merged"), ButtonData.OK_DONE);

        ButtonType both;
        ButtonType second;
        ButtonType first;
        ButtonType removeExact = new ButtonType(Localization.lang("Automatically remove exact duplicates"), ButtonData.LEFT);
        boolean removeExactVisible = false;

        switch (type) {
            case DUPLICATE_SEARCH -> {
                first = new ButtonType(Localization.lang("Keep left"), ButtonData.LEFT);
                second = new ButtonType(Localization.lang("Keep right"), ButtonData.LEFT);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.LEFT);
                threeWayMerge = new ThreeWayMergeView(one, two);
            }
            case DUPLICATE_SEARCH_WITH_EXACT -> {
                first = new ButtonType(Localization.lang("Keep left"), ButtonData.LEFT);
                second = new ButtonType(Localization.lang("Keep right"), ButtonData.LEFT);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.LEFT);
                removeExactVisible = true;
                threeWayMerge = new ThreeWayMergeView(one, two);
            }
            case IMPORT_CHECK -> {
                first = new ButtonType(Localization.lang("Keep old entry"), ButtonData.LEFT);
                second = new ButtonType(Localization.lang("Keep from import"), ButtonData.LEFT);
                both = new ButtonType(Localization.lang("Keep both"), ButtonData.LEFT);
                threeWayMerge = new ThreeWayMergeView(one, two, Localization.lang("Old entry"),
                        Localization.lang("From import"));
            }
            default -> throw new IllegalStateException("Switch expression should be exhaustive");
        }

        this.getDialogPane().getButtonTypes().addAll(first, second, both, merge, cancel);
        this.getDialogPane().setFocusTraversable(false);

        if (removeExactVisible) {
            this.getDialogPane().getButtonTypes().add(removeExact);

            // This will prevent all dialog buttons from having the same size
            // Read more: https://stackoverflow.com/questions/45866249/javafx-8-alert-different-button-sizes
            getDialogPane().getButtonTypes().stream()
                           .map(getDialogPane()::lookupButton)
                           .forEach(btn-> ButtonBar.setButtonUniformSize(btn, false));
        }

        // Retrieves the previous window state and sets the new dialog window size and position to match it
        DialogWindowState state = stateManager.getDialogWindowState(getClass().getSimpleName());
        if (state != null) {
            this.getDialogPane().setPrefSize(state.getWidth(), state.getHeight());
            this.setX(state.getX());
            this.setY(state.getY());
        }

        BorderPane borderPane = new BorderPane(threeWayMerge);

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

        HelpAction helpCommand = new HelpAction(HelpFile.FIND_DUPLICATES, dialogService);
        Button helpButton = actionFactory.createIconButton(StandardActions.HELP, helpCommand);
        borderPane.setRight(helpButton);

        getDialogPane().setContent(borderPane);
    }

    public BibEntry getMergedEntry() {
        return threeWayMerge.getMergedEntry();
    }

    public BibEntry getNewLeftEntry() {
        return threeWayMerge.getLeftEntry();
    }

    public BibEntry getNewRightEntry() {
        return threeWayMerge.getRightEntry();
    }
}
