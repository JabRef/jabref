package org.jabref.gui.ai.components.summary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.logic.ai.summarization.Summary;

import com.airhacks.afterburner.views.ViewLoader;

public class SummaryShowingComponent extends VBox {
    @FXML private TextArea summaryTextArea;
    @FXML private Text summaryInfoText;

    private final Summary summary;
    private final Runnable regenerateCallback;

    public SummaryShowingComponent(Summary summary, Runnable regenerateCallback) {
        this.summary = summary;
        this.regenerateCallback = regenerateCallback;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
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
    private void onRegenerateButtonClick() {
        regenerateCallback.run();
    }
}
