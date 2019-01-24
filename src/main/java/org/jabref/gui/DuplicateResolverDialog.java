package org.jabref.gui;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import org.jabref.Globals;
import org.jabref.gui.DuplicateResolverDialog.DuplicateResolverResult;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.ImportInspectionDialog;
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
        NOT_CHOSEN,
        KEEP_BOTH,
        KEEP_LEFT,
        KEEP_RIGHT,
        AUTOREMOVE_EXACT,
        KEEP_MERGE,
        BREAK
    }

    ActionFactory factory = new ActionFactory(Globals.prefs.getKeyBindingRepository());

    HelpAction helpCommand = new HelpAction(HelpFile.FIND_DUPLICATES);
    Button helpButton = factory.createIconButton(StandardActions.HELP, helpCommand.getCommand());
    private final Button cancel = new Button(Localization.lang("Cancel"));
    private final Button merge = new Button(Localization.lang("Keep merged entry only"));
    private final JabRefFrame frame;
    private final FlowPane options = new FlowPane();
    private DuplicateResolverResult status = DuplicateResolverResult.NOT_CHOSEN;
    private MergeEntries me;

    public DuplicateResolverDialog(JabRefFrame frame, BibEntry one, BibEntry two, DuplicateResolverType type) {
        this.frame = frame;
        init(one, two, type);

    }

    public DuplicateResolverDialog(ImportInspectionDialog dialog, BibEntry one, BibEntry two,
                                   DuplicateResolverType type) {

        //super(dialog, Localization.lang("Possible duplicate entries"), true, DuplicateResolverDialog.class);
        this.frame = dialog.getFrame();
        init(one, two, type);

    }

    private void init(BibEntry one, BibEntry two, DuplicateResolverType type) {

        this.setResultConverter(button -> {
            return status;
        });

        Button both;
        Button second;
        Button first;
        Button removeExact = null;
        switch (type) {
            case DUPLICATE_SEARCH:
                first = new Button(Localization.lang("Keep left"));
                second = new Button(Localization.lang("Keep right"));
                both = new Button(Localization.lang("Keep both"));
                me = new MergeEntries(one, two, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
            case INSPECTION:
                first = new Button(Localization.lang("Remove old entry"));
                second = new Button(Localization.lang("Remove entry from import"));
                both = new Button(Localization.lang("Keep both"));
                me = new MergeEntries(one, two, Localization.lang("Old entry"),
                                      Localization.lang("From import"), frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
            case DUPLICATE_SEARCH_WITH_EXACT:
                first = new Button(Localization.lang("Keep left"));
                second = new Button(Localization.lang("Keep right"));
                both = new Button(Localization.lang("Keep both"));
                removeExact = new Button(Localization.lang("Automatically remove exact duplicates"));
                me = new MergeEntries(one, two, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
            default:
                first = new Button(Localization.lang("Import and remove old entry"));
                second = new Button(Localization.lang("Do not import entry"));
                both = new Button(Localization.lang("Import and keep old entry"));
                me = new MergeEntries(one, two, Localization.lang("Old entry"),
                                      Localization.lang("From import"), frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                break;
        }

        if (removeExact != null) {
            options.getChildren().add(removeExact);
        }
        options.getChildren().addAll(first, second, both, merge, cancel, helpButton);

        first.setOnAction(e -> buttonPressed(DuplicateResolverResult.KEEP_LEFT));
        second.setOnAction(e -> buttonPressed(DuplicateResolverResult.KEEP_RIGHT));
        both.setOnAction(e -> buttonPressed(DuplicateResolverResult.KEEP_BOTH));
        merge.setOnAction(e -> buttonPressed(DuplicateResolverResult.KEEP_MERGE));
        if (removeExact != null) {
            removeExact.setOnAction(e -> buttonPressed(DuplicateResolverResult.AUTOREMOVE_EXACT));
        }

        cancel.setOnAction(e -> buttonPressed(DuplicateResolverResult.BREAK));
        BorderPane borderPane = new BorderPane(me);
        borderPane.setBottom(options);

        getDialogPane().setContent(borderPane);
    }

    private void buttonPressed(DuplicateResolverResult result) {
        status = result;
    }

    public BibEntry getMergedEntry() {
        return me.getMergeEntry();
    }

}
