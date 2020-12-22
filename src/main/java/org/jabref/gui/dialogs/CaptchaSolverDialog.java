package org.jabref.gui.dialogs;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.web.WebView;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;

import org.jsoup.helper.W3CDom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class CaptchaSolverDialog extends BaseDialog<String> implements org.jabref.logic.importer.fetcher.CaptchaSolver {

    public static final Logger LOGGER = LoggerFactory.getLogger(CaptchaSolverDialog.class);

    private WebView webView;

    public CaptchaSolverDialog() {
        super();
        this.setTitle(Localization.lang("Captcha Solver"));
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(true);

        webView = new WebView();
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setUserAgent(URLDownload.USER_AGENT);
        getDialogPane().setContent(webView);
    }

    @Override
    public String solve(String queryURL) {
        // slim implementation of https://news.kynosarges.org/2014/05/01/simulating-platform-runandwait/
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            webView.getEngine().load(queryURL);
            // For the quick implementation, we ignore the result
            // Later, at "webView", we directly extract it from the web view
            this.showAndWait();
            doneLatch.countDown();
        });
        try {
            doneLatch.await();
            Document document = webView.getEngine().getDocument();
            return W3CDom.asString(document, null);
        } catch (InterruptedException e) {
            LOGGER.error("Issues with the UI", e);
        }
        return "";
    }
}
