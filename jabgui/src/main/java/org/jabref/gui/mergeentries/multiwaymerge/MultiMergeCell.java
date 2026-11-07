package org.jabref.gui.mergeentries.multiwaymerge;

import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.tobiasdiez.easybind.EasyObservableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiMergeCell extends HBox {
    public static final PseudoClass ODD_PSEUDO_CLASS = PseudoClass.getPseudoClass("odd");
    public static final PseudoClass EVEN_PSEUDO_CLASS = PseudoClass.getPseudoClass("even");
    public static final String DEFAULT_STYLE_CLASS = "merge-cell";

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiMergeCell.class);

    private final String content;
    private final ToggleButton cellButton = new ToggleButton();

    public MultiMergeCell(String content,
                          Field field,
                          int columnIndex,
                          int rowIndex,
                          MultiMergeFieldRow row,
                          Region headerRegion,
                          Region editorRegion,
                          EasyObservableValue<String> fieldBinding,
                          ObjectProperty<DiffMode> diffMode,
                          GuiPreferences preferences,
                          BiConsumer<String, Supplier<BibEntry>> onAddSource) {
        this.content = content;

        getStyleClass().addAll("field-cell", DEFAULT_STYLE_CLASS);
        pseudoClassStateChanged(ODD_PSEUDO_CLASS, rowIndex % 2 == 1);
        pseudoClassStateChanged(EVEN_PSEUDO_CLASS, rowIndex % 2 == 0);
        setAlignment(Pos.CENTER_LEFT);

        /*
        If this is not explicitly done on the JavaFX thread, the bindings to the text fields don't work properly.
        The text only shows up after one text in that same row is selected by the user.
        */
        UiTaskExecutor.runInJavaFXThread(() -> {
            prefWidthProperty().bind(headerRegion.widthProperty());
            setMinWidth(Control.USE_PREF_SIZE);
            setMaxWidth(Control.USE_PREF_SIZE);

            prefHeightProperty().bind(editorRegion.heightProperty());
            setMinHeight(Control.USE_PREF_SIZE);
            setMaxHeight(Control.USE_PREF_SIZE);

            // Button
            cellButton.getStyleClass().addAll("merge-field-value", "selection-box");
            cellButton.prefHeightProperty().bind(heightProperty());
            cellButton.setMinHeight(Control.USE_PREF_SIZE);
            cellButton.setMaxHeight(Control.USE_PREF_SIZE);
            cellButton.setGraphicTextGap(0);
            cellButton.maxWidthProperty().bind(widthProperty());
            if (content.isEmpty()) {
                cellButton.prefWidthProperty().bind(widthProperty());
            }
            HBox.setHgrow(cellButton, Priority.ALWAYS);
            getChildren().add(cellButton);

            // Text
            DiffHighlightingEllipsingTextFlow buttonText = new DiffHighlightingEllipsingTextFlow(
                    content,
                    fieldBinding,
                    diffMode);

            buttonText.maxWidthProperty().bind(widthProperty().subtract(16));
            buttonText.maxHeightProperty().bind(heightProperty());
            cellButton.setGraphic(buttonText);
            cellButton.setContentDisplay(ContentDisplay.CENTER);

            // Tooltip
            Tooltip buttonTooltip = new Tooltip(content);
            buttonTooltip.setWrapText(true);
            buttonTooltip.prefWidthProperty().bind(widthProperty());
            buttonTooltip.setTextAlignment(TextAlignment.LEFT);
            cellButton.setTooltip(buttonTooltip);

            cellButton.setToggleGroup(row.getToggleGroup());

            // An empty cell is only added so the user can explicitly clear the merged field. Auto-selecting it
            // would clear the value that was just merged (rows are created as a reaction to that value being set).
            if (!content.isEmpty() && (row.getToggleGroup().getSelectedToggle() == null)) {
                cellButton.setSelected(true);
            }

            if (field.equals(StandardField.DOI) && !content.isEmpty()) {
                Button doiButton = ControlHelper.iconButton(IconTheme.JabRefIcons.LOOKUP_IDENTIFIER);
                HBox.setHgrow(doiButton, Priority.NEVER);
                doiButton.prefHeightProperty().bind(cellButton.heightProperty());
                doiButton.setMinHeight(Control.USE_PREF_SIZE);
                doiButton.setMaxHeight(Control.USE_PREF_SIZE);

                getChildren().add(doiButton);

                doiButton.setOnAction(_ -> {
                    DoiFetcher doiFetcher = new DoiFetcher(preferences.getImportFormatPreferences());
                    doiButton.setDisable(true);
                    onAddSource.accept(Localization.lang("From DOI"), () -> {
                        try {
                            return doiFetcher.performSearchById(content).get();
                        } catch (FetcherException | NoSuchElementException e) {
                            LOGGER.warn("Failed to fetch BibEntry for DOI {}", content, e);
                            return null;
                        }
                    });
                });
            }
        });
    }

    public String getContent() {
        return content;
    }

    public ToggleButton getCellButton() {
        return cellButton;
    }
}
