package org.jabref.gui.maintable.columns;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
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
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.Rating;

/// A column that displays a SpecialField
public class SpecialFieldColumn extends MainTableColumn<Optional<SpecialFieldValueViewModel>> {

    private final CliPreferences preferences;
    private final StateManager stateManager;

    public SpecialFieldColumn(MainTableColumnModel model, CliPreferences preferences, StateManager stateManager) {
        super(model);
        this.preferences = preferences;
        this.stateManager = stateManager;

        SpecialField specialField = (SpecialField) FieldFactory.parseField(model.getQualifier());
        Action fieldAction = SpecialFieldViewModel.getAction(specialField);

        Node headerGraphic = fieldAction.getIcon().orElseThrow().getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(fieldAction.getText()));
        this.setGraphic(headerGraphic);
        this.getStyleClass().add(MainTableColumnFactory.STYLE_ICON_COLUMN);

        if (specialField == SpecialField.RANKING) {
            MainTableColumnFactory.setExactWidth(this, SpecialFieldsPreferences.COLUMN_RANKING_WIDTH);
            this.setResizable(false);
            new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                    .withGraphic(this::createSpecialRating)
                    .install(this);
        } else {
            MainTableColumnFactory.setExactWidth(this, ColumnPreferences.ICON_COLUMN_WIDTH);
            this.setResizable(false);

            if (specialField.isSingleValueField()) {
                new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                        .withGraphic((entry, value) -> createSpecialFieldIcon(value, fieldAction))
                        .withOnMouseClickedEvent((entry, value) -> event -> {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                writerFor(specialField, entry).toggle(entry.getEntry());
                            }
                        })
                        .install(this);
            } else {
                new OptionalValueTableCellFactory<BibEntryTableViewModel, SpecialFieldValueViewModel>()
                        .withGraphic((entry, value) -> createSpecialFieldIcon(value, fieldAction))
                        .withMenu((entry, value) -> createSpecialFieldMenu(entry.getEntry(), writerFor(specialField, entry)))
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

    private Rating createSpecialRating(BibEntryTableViewModel entry, Optional<SpecialFieldValueViewModel> value) {
        Rating ranking = new Rating();

        if (value.isPresent()) {
            ranking.setRating(value.get().getValue().toRating());
        } else {
            ranking.setRating(0);
        }

        ranking.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                ranking.setRating(0);
                event.consume();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });

        EasyBind.subscribe(ranking.ratingProperty(), rating ->
                writerFor(SpecialField.RANKING, entry)
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

    /// Writes go to the journal of the library the row belongs to. One column serves rows of
    /// several libraries in the global search results, so the journal is a property of the row
    /// rather than of the column.
    private SpecialFieldViewModel writerFor(SpecialField specialField, BibEntryTableViewModel entry) {
        return new SpecialFieldViewModel(specialField, preferences, stateManager.getUndoManager(entry.getBibDatabaseContext()));
    }

    private Node createSpecialFieldIcon(Optional<SpecialFieldValueViewModel> fieldValue, Action fieldAction) {
        return fieldValue.flatMap(SpecialFieldValueViewModel::getIcon)
                         .map(JabRefIcon::getGraphicNode)
                         .orElseGet(() -> {
                             Node node = fieldAction.getIcon().orElseThrow().getGraphicNode();
                             node.getStyleClass().add("empty-special-field");
                             return node;
                         });
    }
}
