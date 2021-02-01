package org.jabref.gui.maintable.columns;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnFactory;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.util.OptionalValueTableCellFactory;
import org.jabref.gui.util.comparator.RankingFieldComparator;
import org.jabref.gui.util.comparator.SpecialFieldComparator;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.Rating;

/**
 * A column that displays a SpecialField
 */
public class SpecialFieldColumn extends MainTableColumn<Optional<SpecialFieldValueViewModel>> {

    private final PreferencesService preferencesService;
    private final UndoManager undoManager;

    public SpecialFieldColumn(MainTableColumnModel model, PreferencesService preferencesService, UndoManager undoManager) {
        super(model);
        this.preferencesService = preferencesService;
        this.undoManager = undoManager;

        SpecialField specialField = (SpecialField) FieldFactory.parseField(model.getQualifier());
        SpecialFieldViewModel specialFieldViewModel = new SpecialFieldViewModel(specialField, preferencesService, undoManager);

        Node headerGraphic = specialFieldViewModel.getIcon().getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(specialFieldViewModel.getLocalization()));
        this.setGraphic(headerGraphic);
        this.getStyleClass().add(MainTableColumnFactory.STYLE_ICON_COLUMN);

        if (specialField == SpecialField.RANKING) {
            MainTableColumnFactory.setExactWidth(this, SpecialFieldsPreferences.COLUMN_RANKING_WIDTH);
            this.setResizable(false);
            new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                    .withGraphicIfPresent(this::createSpecialRating)
                    .install(this);
        } else {
            MainTableColumnFactory.setExactWidth(this, ColumnPreferences.ICON_COLUMN_WIDTH);
            this.setResizable(false);

            if (specialField.isSingleValueField()) {
                new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                        .withGraphic((entry, value) -> createSpecialFieldIcon(value, specialFieldViewModel))
                        .withOnMouseClickedEvent((entry, value) -> event -> {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                specialFieldViewModel.toggle(entry.getEntry());
                            }
                        })
                        .install(this);
            } else {
                new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                        .withGraphic((entry, value) -> createSpecialFieldIcon(value, specialFieldViewModel))
                        .withMenu((entry, value) -> createSpecialFieldMenu(entry.getEntry(), specialFieldViewModel))
                        .install(this);
            }
        }

        this.setCellValueFactory(cellData -> cellData.getValue().getSpecialField(specialField));

        if (specialField == SpecialField.RANKING) {
            this.setComparator(new RankingFieldComparator());
        } else {
            this.setComparator(new SpecialFieldComparator());
        }

        this.setSortable(true);
    }

    private Rating createSpecialRating(BibEntryTableViewModel entry, SpecialFieldValueViewModel value) {
        Rating ranking = new Rating();
        ranking.setRating(value.getValue().toRating());
        EasyBind.subscribe(ranking.ratingProperty(), rating ->
                new SpecialFieldViewModel(SpecialField.RANKING, preferencesService, undoManager)
                        .setSpecialFieldValue(entry.getEntry(), SpecialFieldValue.getRating(rating.intValue())));

        return ranking;
    }

    private ContextMenu createSpecialFieldMenu(BibEntry entry, SpecialFieldViewModel specialField) {
        ContextMenu contextMenu = new ContextMenu();

        for (SpecialFieldValueViewModel value : specialField.getValues()) {
            MenuItem menuItem = new MenuItem(value.getMenuString(), value.getIcon().map(JabRefIcon::getGraphicNode).orElse(null));
            menuItem.setOnAction(event -> specialField.setSpecialFieldValue(entry, value.getValue()));
            contextMenu.getItems().add(menuItem);
        }

        return contextMenu;
    }

    private Node createSpecialFieldIcon(Optional<SpecialFieldValueViewModel> fieldValue, SpecialFieldViewModel specialField) {
        return fieldValue.flatMap(SpecialFieldValueViewModel::getIcon)
                         .map(JabRefIcon::getGraphicNode)
                         .orElseGet(() -> {
                             Node node = specialField.getEmptyIcon().getGraphicNode();
                             node.getStyleClass().add("empty-special-field");
                             return node;
                         });
    }
}
