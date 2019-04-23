package org.jabref.gui.importer;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

/**
 * Asks for details about merge database operation.
 */
public class AppendDatabaseDialog extends BaseDialog<Boolean> {

    private final CheckBox entries = new CheckBox();
    private final CheckBox strings = new CheckBox();
    private final CheckBox groups = new CheckBox();
    private final CheckBox selector = new CheckBox();

    public AppendDatabaseDialog() {
        this.setTitle(Localization.lang("Append library"));

        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        this.setResultConverter(button -> button == ButtonType.OK);
        init();
    }

    private void init() {
        entries.setSelected(true);
        entries.setText(Localization.lang("Import entries"));
        strings.setSelected(true);
        strings.setText(Localization.lang("Import strings"));
        groups.setText(Localization.lang("Import group definitions"));
        selector.setText(Localization.lang("Import word selector definitions"));

        GridPane container = new GridPane();
        getDialogPane().setContent(container);
        container.setHgap(10);
        container.setVgap(10);
        container.add(entries, 0, 0);
        container.add(strings, 0, 1);
        container.add(groups, 0, 2);
        container.add(selector, 0, 3);
        container.setPadding(new Insets(15, 5, 0, 5));
        container.setGridLinesVisible(false);
    }

    public boolean importEntries() {
        return entries.isSelected();
    }

    public boolean importGroups() {
        return groups.isSelected();
    }

    public boolean importStrings() {
        return strings.isSelected();
    }

    public boolean importSelectorWords() {
        return selector.isSelected();
    }
}
