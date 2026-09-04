package org.jabref.gui.maintable.columns;

import java.util.List;
import java.util.Optional;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.util.OptionalValueTableCellFactory;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.undo.UndoableFieldChange;

import com.tobiasdiez.easybind.EasyBind;

/// A column for fields with content selectors.
public class ContentSelectorColumn extends MainTableColumn<Optional<String>> {

    private final List<String> values;
    private final Field field;
    private final StateManager stateManager;

    public ContentSelectorColumn(MainTableColumnModel model, List<String> values, StateManager stateManager) {
        super(model);
        this.values = values;
        this.stateManager = stateManager;
        this.field = FieldFactory.parseField(model.getQualifier());

        setText(field.getName());
        setCellValueFactory(param -> EasyBind.map(param.getValue().getFields(new OrFields(field)),
                value -> StringUtil.isBlank(value) ? Optional.empty() : Optional.of(value)));

        new OptionalValueTableCellFactory<BibEntryTableViewModel, String>()
                .withText(value -> value.orElse(""))
                .withMenu(this::createMenu)
                .install(this);

        this.setSortable(true);
    }

    private ContextMenu createMenu(BibEntryTableViewModel model, Optional<String> value) {
        ContextMenu menu = new ContextMenu();
        BibEntry entry = model.getEntry();

        for (String item : values) {
            MenuItem menuItem = new MenuItem(item);
            menuItem.setOnAction(event -> {
                UndoableFieldChange change = new UndoableFieldChange(entry, field, entry.getField(field).orElse(null), item);
                stateManager.getUndoManager(model.getBibDatabaseContext()).applyEdit(change);
            });
            menu.getItems().add(menuItem);
        }
        return menu;
    }
}
