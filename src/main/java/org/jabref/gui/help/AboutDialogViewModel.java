package org.jabref.gui.help;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutDialogViewModel extends AbstractViewModel {

    private static final String HOMEPAGE_URL = "https://www.jabref.org";
    private static final String DONATION_URL = "https://donations.jabref.org";
    private static final String LIBRARIES_URL = "https://github.com/JabRef/jabref/blob/master/external-libraries.txt";
    private static final String GITHUB_URL = "https://github.com/JabRef/jabref";
    private static final String LICENSE_URL = "https://github.com/JabRef/jabref/blob/master/LICENSE.md";
    private final String changelogUrl;
    private final String versionInfo;
    private final ReadOnlyStringWrapper environmentInfo = new ReadOnlyStringWrapper();
    private final Logger logger = LoggerFactory.getLogger(AboutDialogViewModel.class);
    private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper authors = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper developers = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper license = new ReadOnlyStringWrapper();
    private final ReadOnlyBooleanWrapper isDevelopmentVersion = new ReadOnlyBooleanWrapper();
    private final DialogService dialogService;
    private final ReadOnlyStringWrapper developmentVersion = new ReadOnlyStringWrapper();
    private final ClipBoardManager clipBoardManager;

    public AboutDialogViewModel(DialogService dialogService, ClipBoardManager clipBoardManager, BuildInfo buildInfo) {
        this.dialogService = Objects.requireNonNull(dialogService);
        this.clipBoardManager = Objects.requireNonNull(clipBoardManager);
        String[] version = buildInfo.getVersion().getFullVersion().split("--");
        heading.set("JabRef " + version[0]);

        if (version.length == 1) {
            isDevelopmentVersion.set(false);
        } else {
            isDevelopmentVersion.set(true);
            String dev = Lists.newArrayList(version).stream().filter(string -> !string.equals(version[0])).collect(
                    Collectors.joining("--"));
            developmentVersion.set(dev);
        }
        developers.set(buildInfo.getDevelopers());
        authors.set(buildInfo.getAuthors());
        license.set(Localization.lang("License") + ":");
        changelogUrl = buildInfo.getVersion().getChangelogUrl();
        versionInfo = String.format("JabRef %s%n%s %s %s %nJava %s", buildInfo.getVersion(), BuildInfo.OS,
                BuildInfo.OS_VERSION, BuildInfo.OS_ARCH, BuildInfo.JAVA_VERSION);
    }

    public String getDevelopmentVersion() {
        return developmentVersion.get();
    }

    public ReadOnlyStringProperty developmentVersionProperty() {
        return developmentVersion.getReadOnlyProperty();
    }

    public boolean isIsDevelopmentVersion() {
        return isDevelopmentVersion.get();
    }

    public ReadOnlyBooleanProperty isDevelopmentVersionProperty() {
        return isDevelopmentVersion.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty authorsProperty() {
        return authors.getReadOnlyProperty();
    }

    public String getAuthors() {
        return authors.get();
    }

    public String getVersionInfo() {
        return versionInfo;
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

    public ReadOnlyStringProperty licenseProperty() {
        return license.getReadOnlyProperty();
    }

    public String getLicense() {
        return license.get();
    }

    public String getEnvironmentInfo() {
        return environmentInfo.get();
    }

    public void copyVersionToClipboard() {
        clipBoardManager.setClipboardContents(versionInfo);
        dialogService.notify(Localization.lang("Copied version to clipboard"));
    }

    public void openJabrefWebsite() {
        openWebsite(HOMEPAGE_URL);
    }

    public void openExternalLibrariesWebsite() {
        openWebsite(LIBRARIES_URL);
    }

    public void openGithub() {
        openWebsite(GITHUB_URL);
    }

    public void openChangeLog() {
        openWebsite(changelogUrl);
    }

    public void openLicense() {
        openWebsite(LICENSE_URL);
    }

    public void openDonation() {
        openWebsite(DONATION_URL);
    }

    private void openWebsite(String url) {
        try {
            JabRefDesktop.openBrowser(url);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not open website."), e);
            logger.error("Could not open default browser.", e);
        }
    }

}
