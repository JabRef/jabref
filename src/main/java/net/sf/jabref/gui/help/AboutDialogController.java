package net.sf.jabref.gui.help;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import net.sf.jabref.gui.AbstractController;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.DialogService;
import net.sf.jabref.logic.util.BuildInfo;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;

public class AboutDialogController extends AbstractController<AboutDialogViewModel> {

    @FXML protected ImageView iconImage;
    @Inject private DialogService dialogService;
    @Inject private ClipBoardManager clipBoardManager;
    @Inject private BuildInfo buildInfo;

    @FXML
    private void initialize() {
        viewModel = new AboutDialogViewModel(dialogService, clipBoardManager, buildInfo);

        SvgImageLoaderFactory.install();
        Image icon = new Image(this.getClass().getResourceAsStream("/images/icons/JabRef-icon.svg"));
        iconImage.setImage(icon);
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
