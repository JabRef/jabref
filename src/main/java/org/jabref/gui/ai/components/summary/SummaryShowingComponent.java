package org.jabref.gui.ai.components.summary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.ai.summarization.Summary;
import org.jabref.logic.layout.format.MarkdownFormatter;
import org.jabref.logic.util.WebViewStore;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class SummaryShowingComponent extends VBox {
    @FXML private Text summaryInfoText;
    @FXML private CheckBox markdownCheckbox;

    @Inject private ThemeManager themeManager;

    private WebView contentWebView;
    private final Summary summary;
    private final Runnable regenerateCallback;
    private final MarkdownFormatter markdownFormatter = new MarkdownFormatter();

    public SummaryShowingComponent(Summary summary, Runnable regenerateCallback) {
        this.summary = summary;
        this.regenerateCallback = regenerateCallback;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        initializeWebView();
        updateContent(false); // Start in plain text mode
        updateInfoText();
    }

    private void initializeWebView() {
        contentWebView = WebViewStore.get();
        VBox.setVgrow(contentWebView, Priority.ALWAYS);

        themeManager.installCss(contentWebView.getEngine());

        getChildren().addFirst(contentWebView);
    }

    private void updateContent(boolean isMarkdown) {
        String content = summary.content();
        if (isMarkdown) {
            contentWebView.getEngine().loadContent(markdownFormatter.format(content));
        } else {
            contentWebView.getEngine().loadContent(
                    "<body style='margin: 0; padding: 0; width: 100vw'>" +
                            "<div style='white-space: pre-wrap; word-wrap: break-word; width: 100vw'>" +
                            content +
                            "</div></body>"
            );
        }
    }

    private void updateInfoText() {
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
        updateContent(markdownCheckbox.isSelected());
    }

    @FXML
    private void onRegenerateButtonClick() {
        regenerateCallback.run();
    }
}
