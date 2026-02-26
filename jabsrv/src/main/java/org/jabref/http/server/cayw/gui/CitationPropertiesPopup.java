package org.jabref.http.server.cayw.gui;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import org.jabref.http.server.cayw.CitationProperties;
import org.jabref.http.server.cayw.LocatorType;
import org.jabref.logic.l10n.Localization;

public class CitationPropertiesPopup extends Popup {

    public CitationPropertiesPopup(CAYWEntry entry) {
        CitationProperties properties = entry.citationProperties();

        Label titleLabel = new Label(entry.description());
        titleLabel.getStyleClass().add("popup-title");

        Label authorLabel = new Label(entry.label());
        authorLabel.getStyleClass().add("popup-author");

        ComboBox<LocatorType> locatorTypeCombo = new ComboBox<>();
        locatorTypeCombo.getItems().addAll(LocatorType.values());
        locatorTypeCombo.setValue(properties.getLocatorType().orElse(LocatorType.PAGE));
        properties.setLocatorType(locatorTypeCombo.getValue());

        TextField locatorValueField = new TextField();
        properties.getLocatorValue().ifPresent(locatorValueField::setText);

        TextField prefixField = new TextField();
        properties.getPrefix().ifPresent(prefixField::setText);

        TextField suffixField = new TextField();
        properties.getSuffix().ifPresent(suffixField::setText);

        CheckBox omitAuthorCheck = new CheckBox(Localization.lang("Omit author"));
        omitAuthorCheck.setSelected(properties.isOmitAuthor());

        locatorTypeCombo.valueProperty().addListener((_, _, val) -> properties.setLocatorType(val));
        locatorValueField.textProperty().addListener((_, _, val) -> properties.setLocatorValue(val));
        prefixField.textProperty().addListener((_, _, val) -> properties.setPrefix(val));
        suffixField.textProperty().addListener((_, _, val) -> properties.setSuffix(val));
        omitAuthorCheck.selectedProperty().addListener((_, _, val) -> properties.setOmitAuthor(val));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        grid.add(locatorTypeCombo, 0, 0);
        grid.add(locatorValueField, 1, 0);
        grid.add(new Label(Localization.lang("Prefix")), 0, 1);
        grid.add(prefixField, 1, 1);
        grid.add(new Label(Localization.lang("Suffix")), 0, 2);
        grid.add(suffixField, 1, 2);
        grid.add(omitAuthorCheck, 0, 3, 2, 1);

        VBox content = new VBox(8, titleLabel, authorLabel, grid);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-radius: 4; -fx-background-radius: 4;");
        content.setEffect(new DropShadow(10, Color.gray(0, 0.3)));

        getContent().add(content);
        setAutoHide(true);
    }

    public void showBelow(Node anchor) {
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        show(anchor, bounds.getMinX(), bounds.getMaxY() + 5);
    }
}
