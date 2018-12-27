package org.jabref.gui.util;

import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebView;

import org.jabref.gui.desktop.JabRefDesktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

/**
 * A Hyperlink Click Listener for javafx.WebView to open links on click in the browser
 *  Code adapted from: <a href="https://stackoverflow.com/a/33445383/">https://stackoverflow.com/a/33445383/</a>
 */
public class OpenHyperlinksInExternalBrowser implements ChangeListener<Worker.State>, EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHyperlinksInExternalBrowser.class);
    private static final String CLICK_EVENT = "click";
    private static final String ANCHOR_TAG = "a";

    private final WebView webView;

    public OpenHyperlinksInExternalBrowser(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
        if (State.SUCCEEDED.equals(newValue)) {
            Document document = webView.getEngine().getDocument();
            NodeList anchors = document.getElementsByTagName(ANCHOR_TAG);
            for (int i = 0; i < anchors.getLength(); i++) {
                Node node = anchors.item(i);
                EventTarget eventTarget = (EventTarget) node;
                eventTarget.addEventListener(CLICK_EVENT, this, false);
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        HTMLAnchorElement anchorElement = (HTMLAnchorElement) event.getCurrentTarget();
        String href = anchorElement.getHref();

        try {
            JabRefDesktop.openBrowser(href);
        } catch (IOException e) {
            LOGGER.error("Problem opening browser", e);
        }
        event.preventDefault();
    }

}
