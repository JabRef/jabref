package org.jabref.gui.help;

import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.URLs;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

import com.google.common.collect.Lists;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutDialogViewModel extends AbstractViewModel {
    private final String changelogUrl;
    private final String versionInfo;
    private final ReadOnlyStringWrapper environmentInfo = new ReadOnlyStringWrapper();
    private final Logger logger = LoggerFactory.getLogger(AboutDialogViewModel.class);
    private final ReadOnlyStringWrapper heading = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper maintainers = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper license = new ReadOnlyStringWrapper();
    private final ReadOnlyBooleanWrapper isDevelopmentVersion = new ReadOnlyBooleanWrapper();
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final ReadOnlyStringWrapper developmentVersion = new ReadOnlyStringWrapper();
    private final ClipBoardManager clipBoardManager;

    public AboutDialogViewModel(@NonNull DialogService dialogService,
                                @NonNull GuiPreferences preferences,
                                @NonNull ClipBoardManager clipBoardManager,
                                BuildInfo buildInfo) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.clipBoardManager = clipBoardManager;
        String[] version = buildInfo.version.getFullVersion().split("--");
        heading.set("JabRef " + version[0]);

        if (version.length == 1) {
            isDevelopmentVersion.set(false);
        } else {
            isDevelopmentVersion.set(true);
            String dev = Lists.newArrayList(version).stream().filter(string -> !string.equals(version[0])).collect(
                    Collectors.joining("--"));
            developmentVersion.set(dev);
        }
        maintainers.set(buildInfo.maintainers);
        license.set(Localization.lang("License") + ":");
        changelogUrl = buildInfo.version.getChangelogUrl();

        String javafx_version = System.getProperty("javafx.runtime.version", BuildInfo.UNKNOWN_VERSION).toLowerCase(Locale.ROOT);

        versionInfo = "JabRef %s%n%s %s %s %nJava %s %nJavaFX %s".formatted(buildInfo.version, BuildInfo.OS,
                BuildInfo.OS_VERSION, BuildInfo.OS_ARCH, BuildInfo.JAVA_VERSION, javafx_version);
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

    public String getVersionInfo() {
        return versionInfo;
    }

    public ReadOnlyStringProperty maintainersProperty() {
        return maintainers.getReadOnlyProperty();
    }

    public String getMaintainers() {
        return maintainers.get();
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
        clipBoardManager.setContent(versionInfo);
        dialogService.notify(Localization.lang("Copied version to clipboard"));
    }

    public void openJabrefWebsite() {
        openWebsite(URLs.HOMEPAGE_URL);
    }

    public void openExternalLibrariesWebsite() {
        openWebsite(URLs.LIBRARIES_URL);
    }

    public void openGitHub() {
        openWebsite(URLs.GITHUB_URL);
    }

    public void openChangeLog() {
        openWebsite(changelogUrl);
    }

    public void openLicense() {
        openWebsite(URLs.LICENSE_URL);
    }

    public void openContributors() {
        openWebsite(URLs.CONTRIBUTORS_URL);
    }

    public void openDonation() {
        openWebsite(URLs.DONATION_URL);
    }

    private void openWebsite(String url) {
        try {
            NativeDesktop.openBrowser(url, preferences.getExternalApplicationsPreferences());
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not open website."), e);
            logger.error("Could not open default browser.", e);
        }
    }

    public void openPrivacyPolicy() {
        openWebsite(URLs.PRIVACY_POLICY_URL);
    }
}
