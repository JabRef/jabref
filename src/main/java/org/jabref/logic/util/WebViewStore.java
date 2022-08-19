package org.jabref.logic.util;

import java.util.ArrayDeque;
import java.util.Queue;

import javafx.application.Platform;
import javafx.scene.web.WebView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dynamic web view store. This is used primarily to prevent UI freezes while constructing web view instances.
 */
public class WebViewStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebViewStore.class);
    private final static Queue<WebView> WEB_VIEWS = new ArrayDeque<>();
    private static boolean isInitialized = false;
    private static Configuration config;

    /**
     * Initialize {@code WebViewStore} and preload web view instances.
     * <p> Note that this method must be called at application startup. </p>
     */
    public static void init(Configuration config) {
        WebViewStore.config = config;
        for (int i = 0; i < config.getNumberOfPreloadedInstances(); i++) {
            addWebViewLater();
        }
        isInitialized = true;
    }

    /**
     * Initialize {@code WebViewStore} and preload web view instance.
     * <p> Note that this method must be called at application startup. </p>
     */
    public static void init() {
        init(new Configuration(4, 2));
    }

    /**
     * Returns a preloaded web view instance if available; And it will create a new one if not.
     *
     * @return {@code WebView} instance
     * @throws IllegalStateException if the webViewStore has not been initialized
     */
    public static WebView get() {
        if (!isInitialized) {
            throw new IllegalStateException("WebViewStore is uninitialized");
        }
        if (WEB_VIEWS.size() <= config.getMinimumNumberOfInstances()) {
            addWebViewLater();
        }
        if (hasMore()) {
            return WEB_VIEWS.poll();
        } else {
            return new WebView();
        }
    }

    private static void addWebViewLater() {
        Platform.runLater(() -> {
            WEB_VIEWS.add(new WebView());
            LOGGER.info("Cached Web views: {}", WEB_VIEWS.size());
        });
    }

    /**
     * @return {@code true} if the store has at least one web view instance available; {@code false} otherwise
     */
    public static boolean hasMore() {
        return !WEB_VIEWS.isEmpty();
    }

    public record Configuration(
            int numberOfPreloadedInstances,
            int minimumNumberOfInstances) {

        /**
         * @return The number of web view instances to be loaded at application startup
         */
        public int getNumberOfPreloadedInstances() {
            return numberOfPreloadedInstances;
        }

        /**
         * @return The minimum number of web views the store can reach. The store needs to load more instances once it reaches this threshold
         */
        public int getMinimumNumberOfInstances() {
            return minimumNumberOfInstances;
        }
    }
}
