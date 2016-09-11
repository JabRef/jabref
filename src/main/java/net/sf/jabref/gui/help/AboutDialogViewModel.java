package net.sf.jabref.gui.help;

import java.io.IOException;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.BuildInfo;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AboutDialogViewModel {

    private final String HOMEPAGE = "http://www.jabref.org";
    private final String DONATION = "http://www.jabref.org/#donations";
    private final String LIBRARIES = "https://github.com/JabRef/jabref/blob/master/external-libraries.txt";
    private final String GITHUB = "https://github.com/JabRef/jabref";
    private final String CHANGELOG = "https://github.com/JabRef/jabref/blob/master/CHANGELOG.md";
    private final String LICENSE = "https://opensource.org/licenses/mit-license.php";
    private final Log logger = LogFactory.getLog(AboutDialogViewModel.class);
    private final ReadOnlyStringWrapper license = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper authors = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper developers = new ReadOnlyStringWrapper();

    @FXML
    private Button closeButton;
    @FXML
    private ImageView iconImage;


    @FXML
    private void initialize() {

        heading.set("JabRef " + Globals.BUILD_INFO.getVersion());
        developers.set(Globals.BUILD_INFO.getDevelopers());
        authors.set(Globals.BUILD_INFO.getAuthors());

        String licenseText = String.format("MIT License (2003 - %s)", Globals.BUILD_INFO.getYear());
        license.set(licenseText);

        SvgImageLoaderFactory.install();
        Image icon = new Image(this.getClass().getResourceAsStream("/images/icons/JabRef-icon.svg"));
        iconImage.setImage(icon);
    }

    public ReadOnlyStringProperty licenseProperty() {
        return license.getReadOnlyProperty();
    }

    public String getLicense() {
        return license.get();
    }

    public ReadOnlyStringProperty authorsProperty() {
        return authors.getReadOnlyProperty();
    }

    public String getAuthors() {
        return authors.get();
    }

    public ReadOnlyStringProperty developersProperty() {
        return developers.getReadOnlyProperty();
    }

    public String getDevelopers() {
        return developers.get();
    }

    public ReadOnlyStringProperty headingProperty() {
        return heading.getReadOnlyProperty();
    }

    public String getHeading() {
        return heading.get();
    }

    @FXML
    private void closeAboutDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void copyVersionToClipboard() {
        String info = String.format("JabRef %s%n%s %s %s %nJava %s", Globals.BUILD_INFO.getVersion(), BuildInfo.OS,
                BuildInfo.OS_VERSION, BuildInfo.OS_ARCH, BuildInfo.JAVA_VERSION);
        new ClipBoardManager().setClipboardContents(info);
        JabRefGUI.getMainFrame().output(Localization.lang("Copied version to clipboard"));
    }

    @FXML
    private void openJabrefWebsite() {
        openWebsite(HOMEPAGE);
    }

    @FXML
    private void openExternalLibrariesWebsite() {
        openWebsite(LIBRARIES);
    }

    @FXML
    private void openGithub() {
        openWebsite(GITHUB);
    }

    @FXML
    private void openChangeLog() {
        openWebsite(CHANGELOG);
    }

    @FXML
    public void openLicense() {
        openWebsite(LICENSE);
    }

    @FXML
    public void openDonation() {
        openWebsite(DONATION);
    }

    private void openWebsite(String url) {
        try {
            JabRefDesktop.openBrowser(url);
        } catch (IOException e) {
            JabRefGUI.getMainFrame().output(Localization.lang("Error") + ": " + e.getLocalizedMessage());
            logger.debug("Could not open default browser.", e);
        }
    }

}
