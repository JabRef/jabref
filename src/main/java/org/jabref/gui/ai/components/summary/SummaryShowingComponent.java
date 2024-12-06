package org.jabref.gui.ai.components.summary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

import org.jabref.logic.ai.summarization.Summary;
import org.jabref.logic.layout.format.MarkdownFormatter;

import com.airhacks.afterburner.views.ViewLoader;

public class SummaryShowingComponent extends VBox {
    @FXML private TextArea summaryTextArea;
    @FXML private Text summaryInfoText;
    @FXML private CheckBox markdownCheckbox;

    private WebView markdownWebView;

    private final Summary summary;
    private final Runnable regenerateCallback;
    private final MarkdownFormatter markdownFormatter;

    public SummaryShowingComponent(Summary summary, Runnable regenerateCallback) {
        this.summary = summary;
        this.regenerateCallback = regenerateCallback;
        this.markdownFormatter = new MarkdownFormatter();

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        markdownWebView = new WebView();
        markdownWebView.setVisible(false);
        markdownWebView.setManaged(false);

        VBox.setVgrow(markdownWebView, Priority.ALWAYS);

        int indexOfTextArea = getChildren().indexOf(summaryTextArea);
        getChildren().add(indexOfTextArea + 1, markdownWebView);

        summaryTextArea.setText(summary.content());

        String newInfo = summaryInfoText
                .getText()
                .replaceAll("%0", formatTimestamp(summary.timestamp()))
                .replaceAll("%1", summary.aiProvider().getLabel() + " " + summary.model());

        summaryInfoText.setText(newInfo);
    }

    private static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
    }

    @FXML
    private void onMarkdownToggle() {
        if (markdownCheckbox.isSelected()) {
            String htmlContent = markdownFormatter.format(summaryTextArea.getText());
            markdownWebView.getEngine().loadContent(htmlContent);

            markdownWebView.setPrefHeight(summaryTextArea.getPrefHeight());
            markdownWebView.setPrefWidth(summaryTextArea.getPrefWidth());

            markdownWebView.setVisible(true);
            markdownWebView.setManaged(true);

            summaryTextArea.setVisible(false);
            summaryTextArea.setManaged(false);
        } else {
            summaryTextArea.setVisible(true);
            summaryTextArea.setManaged(true);

            markdownWebView.setVisible(false);
            markdownWebView.setManaged(false);
        }
    }

    @FXML
    private void onRegenerateButtonClick() {
        regenerateCallback.run();
    }
}
