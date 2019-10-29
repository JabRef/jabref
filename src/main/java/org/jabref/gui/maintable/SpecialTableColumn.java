package org.jabref.gui.maintable;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

import org.jabref.gui.GUIGlobals;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.gui.util.MainTableUtils;
import org.jabref.gui.util.OptionalValueTableCellFactory;
import org.jabref.gui.util.comparator.RankingFieldComparator;
import org.jabref.gui.util.comparator.ReadStatusFieldComparator;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;

import org.controlsfx.control.Rating;
import org.fxmisc.easybind.EasyBind;

/**
 * A column that displays a specialField
 */
public class SpecialTableColumn extends MainTableColumn<Optional<SpecialFieldValueViewModel>> {

    private final UndoManager undoManager;

    public SpecialTableColumn(UndoManager undoManager, SpecialField specialField) {
        super(MainTableColumnType.SPECIALFIELD);

        this.undoManager = undoManager;

        SpecialFieldViewModel specialFieldViewModel = new SpecialFieldViewModel(specialField, undoManager);
        Node headerGraphic = specialFieldViewModel.getIcon().getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(specialFieldViewModel.getLocalization()));
        setGraphic(headerGraphic);
        getStyleClass().add(MainTableUtils.STYLE_ICON_COLUMN);
        if (specialField == SpecialField.RANKING) {
            MainTableUtils.setExactWidth(this, GUIGlobals.WIDTH_ICON_COL_RANKING);
            new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                    .withGraphicIfPresent(this::createRating)
                    .install(this);
        } else {
            MainTableUtils.setExactWidth(this, GUIGlobals.WIDTH_ICON_COLUMN);

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
        }

        // Added comparator for Read Status
        if (specialField == SpecialField.READ_STATUS) {
            this.setComparator(new ReadStatusFieldComparator());
        }

        this.setSortable(true);
    }

    private Rating createRating(BibEntryTableViewModel entry, SpecialFieldValueViewModel value) {
        Rating ranking = new Rating();
        ranking.setRating(value.getValue().toRating());
        EasyBind.subscribe(ranking.ratingProperty(), rating ->
                new SpecialFieldViewModel(SpecialField.RANKING, undoManager).setSpecialFieldValue(entry.getEntry(), SpecialFieldValue.getRating(rating.intValue()))
        );
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
        return fieldValue
                .flatMap(SpecialFieldValueViewModel::getIcon)
                .map(JabRefIcon::getGraphicNode)
                .orElseGet(() -> {
                    Node node = specialField.getEmptyIcon().getGraphicNode();
                    node.getStyleClass().add("empty-special-field");
                    return node;
                });
    }
}
