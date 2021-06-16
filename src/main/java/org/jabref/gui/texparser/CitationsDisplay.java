package org.jabref.gui.texparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.model.strings.LatexToUnicodeAdapter;
import org.jabref.model.texparser.Citation;

public class CitationsDisplay extends ListView<Citation> {

    private final ObjectProperty<Path> basePath;

    public CitationsDisplay() {
        this.basePath = new SimpleObjectProperty<>(null);
        new ViewModelListCellFactory<Citation>().withGraphic(this::getDisplayGraphic)
                                                .withTooltip(this::getDisplayTooltip)
                                                .install(this);

        this.getStyleClass().add("citationsList");
    }

    public ObjectProperty<Path> basePathProperty() {
        return basePath;
    }

    private Node getDisplayGraphic(Citation item) {
        if (basePath.get() == null) {
            basePath.set(item.getPath().getRoot());
        }

        Node citationIcon = IconTheme.JabRefIcons.LATEX_COMMENT.getGraphicNode();
        Text contextText = new Text(LatexToUnicodeAdapter.format(item.getContext()));
        contextText.wrappingWidthProperty().bind(this.widthProperty().subtract(85));
        HBox contextBox = new HBox(8, citationIcon, contextText);
        contextBox.getStyleClass().add("contextBox");

        Label fileNameLabel = new Label(String.format("%s", basePath.get().relativize(item.getPath())));
        fileNameLabel.setGraphic(IconTheme.JabRefIcons.LATEX_FILE.getGraphicNode());
        Label positionLabel = new Label(String.format("(%s:%s-%s)", item.getLine(), item.getColStart(), item.getColEnd()));
        positionLabel.setGraphic(IconTheme.JabRefIcons.LATEX_LINE.getGraphicNode());
        HBox dataBox = new HBox(5, fileNameLabel, positionLabel);

        return new VBox(contextBox, dataBox);
    }

    private Tooltip getDisplayTooltip(Citation item) {
        String line = item.getLineText();
        int start = item.getColStart();
        int end = item.getColEnd();

        List<Text> texts = new ArrayList<>(3);

        // Text before the citation.
        if (start > 0) {
            texts.add(new Text(line.substring(0, start)));
        }

        // Citation text (highlighted).
        Text citation = new Text(line.substring(start, end));
        citation.getStyleClass().setAll("tooltip-text-bold");
        texts.add(citation);

        // Text after the citation.
        if (end < line.length()) {
            texts.add(new Text(line.substring(end)));
        }

        Tooltip tooltip = new Tooltip();
        tooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tooltip.setGraphic(new TextFlow(texts.toArray(new Text[0])));
        tooltip.setMaxHeight(10);
        tooltip.setMinWidth(200);
        tooltip.maxWidthProperty().bind(this.widthProperty().subtract(85));
        tooltip.setWrapText(true);

        return tooltip;
    }
}
