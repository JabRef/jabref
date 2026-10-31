package org.jabref.http.server.cayw.gui;

import java.io.InputStream;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconThemeHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(IconThemeHelper.class);

    static void applyLogo(Stage dialogStage) {
        try (InputStream inputStream = IconThemeHelper.class.getResourceAsStream("/JabRef-icon-64.png")) {
            if (inputStream == null) {
                LOGGER.warn("Error loading icon for SearchDialog");
            } else {
                Image icon = new Image(inputStream);
                dialogStage.getIcons().add(icon);
            }
        } catch (Exception e) {
            LOGGER.warn("Error loading icon for SearchDialog", e);
        }
    }
}
