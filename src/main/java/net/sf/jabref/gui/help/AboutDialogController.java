package net.sf.jabref.gui.help;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;

public class AboutDialogController {

    private final Log logger = LogFactory.getLog(AboutDialogController.class);

    @FXML
    private Label aboutHeading;

    @FXML
    private Label aboutYear;

    @FXML
    private Hyperlink aboutWebsite;

    @FXML
    private Label aboutLicense;

    @FXML
    private Label developersHeading;

    @FXML
    private Label developersContent;

    @FXML
    private Label authorsHeading;

    @FXML
    private Label authorsContent;

    @FXML
    private Label librariesHeading;

    @FXML
    private Hyperlink librariesContent;

    @FXML
    private Label codeHeading;

    @FXML
    private Hyperlink codeContent;

    @FXML
    private ImageView jabrefLogo;

    @FXML
    private Button closeButton;

    @FXML
    private Button copyButton;


    public AboutDialogController() {
        // TODO Auto-generated constructor stub
    }

    @FXML
    private void initialize() {
        String header = String.format("JabRef %s", Globals.BUILD_INFO.getVersion());
        aboutHeading.setText(header);

        try (InputStream imageStream = JabRef.class.getResourceAsStream("/images/external/JabRef-icon-48.png");) {
            jabrefLogo.setImage(new Image(imageStream));
        } catch (Exception e) {
            logger.debug("Could not find icon.", e);
        }

        String year = String.format("2003-%s", Globals.BUILD_INFO.getYear());
        aboutYear.setText(year);

        aboutWebsite.setText("http://www.jabref.org");

        aboutLicense.setText("GNU General Public License v2 or later");

        developersHeading.setText("Developers:");
        developersContent.setText(Globals.BUILD_INFO.getDevelopers());

        authorsHeading.setText("Authors:");
        authorsContent.setText(Globals.BUILD_INFO.getAuthors());

        librariesHeading.setText("External Libraries:");
        librariesContent.setText("https://github.com/JabRef/jabref/blob/master/external-libraries.txt");

        codeHeading.setText("Code:");
        codeContent.setText("https://github.com/JabRef/jabref");

        closeButton.setText(Localization.lang("Close"));

        copyButton.setText(Localization.lang("Copy_version_to_clipboard"));
    }

    @FXML
    private void closeAboutDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void copyVersionToClipboard() {
        ClipBoardManager.CLIPBOARD.setClipboardContents(Globals.BUILD_INFO.getVersion());
        String message = String.format("%s - %s", Localization.lang("Copy_version_to_clipboard"),
                Globals.BUILD_INFO.getVersion());
        JabRef.jrf.output(message);
    }

    @FXML
    private void openJabrefWebsite() {
        try {
            JabRefDesktop.openBrowser(aboutWebsite.getText());
        } catch (IOException e) {
            JabRef.jrf.output(Localization.lang("Error") + ": " + e.getLocalizedMessage());
            logger.debug("Could not open default browser.", e);
        }
    }

    @FXML
    private void openExternalLibrariesWebsite() {
        try {
            JabRefDesktop.openBrowser(librariesContent.getText());
        } catch (IOException e) {
            JabRef.jrf.output(Localization.lang("Error") + ": " + e.getLocalizedMessage());
            logger.debug("Could not open default browser.", e);
        }
    }

    @FXML
    private void openGithub() {
        try {
            JabRefDesktop.openBrowser(codeContent.getText());
        } catch (IOException e) {
            JabRef.jrf.output(Localization.lang("Error") + ": " + e.getLocalizedMessage());
            logger.debug("Could not open default browser.", e);
        }
    }

}
