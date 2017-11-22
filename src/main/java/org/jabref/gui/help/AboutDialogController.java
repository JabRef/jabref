package org.jabref.gui.help;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.jabref.gui.AbstractController;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.logic.util.BuildInfo;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;

public class AboutDialogController extends AbstractController<AboutDialogViewModel> {

    @FXML protected ImageView iconImage;
    @Inject private DialogService dialogService;
    @Inject private ClipBoardManager clipBoardManager;
    @Inject private BuildInfo buildInfo;

    @FXML private TextArea textAreaVersions;

    @FXML
    private void initialize() {
        viewModel = new AboutDialogViewModel(dialogService, clipBoardManager, buildInfo);

        SvgImageLoaderFactory.install();
        Image icon = new Image(this.getClass().getResourceAsStream("/icons/jabref.svg"));
        iconImage.setImage(icon);
        textAreaVersions.setText(viewModel.getVersionInfo());

    }

    @FXML
    private void closeAboutDialog() {
        getStage().close();
    }

    @FXML
    private void copyVersionToClipboard() {
        viewModel.copyVersionToClipboard();
    }

    @FXML
    private void openJabrefWebsite() {
        viewModel.openJabrefWebsite();
    }

    @FXML
    private void openExternalLibrariesWebsite() {
        viewModel.openExternalLibrariesWebsite();
    }

    @FXML
    private void openGithub() {
        viewModel.openGithub();
    }

    @FXML
    public void openChangeLog() {
        viewModel.openChangeLog();
    }

    @FXML
    public void openLicense() {
        viewModel.openLicense();
    }

    @FXML
    public void openDonation() {
        viewModel.openDonation();
    }

}
