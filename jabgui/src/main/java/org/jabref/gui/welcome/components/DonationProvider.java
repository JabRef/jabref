package org.jabref.gui.welcome.components;

import java.time.LocalDate;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.util.URLs;
import org.jabref.logic.l10n.Localization;

public class DonationProvider {
    private static final int DONATION_INTERVAL_DAYS = 365;
    private static final double DONATION_POPUP_ANIM_MS = 200;
    private static final double DONATION_POPUP_TIMEOUT_SECONDS = 15;
    private static final String DONATION_URL = URLs.DONATE_URL;

    private final StackPane rootPane;
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private HBox donationToast;
    private DelayedExecution scheduledShow;
    private DelayedExecution autoHide;

    public DonationProvider(StackPane rootPane, GuiPreferences preferences, DialogService dialogService) {
        this.rootPane = rootPane;
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    public void showIfNeeded() {
        if (preferences.getDonationPreferences().isNeverShowAgain()) {
            return;
        }
        int lastShown = preferences.getDonationPreferences().getLastShownEpochDay();
        scheduleAfterDays(calculateDaysUntilNextPopup(lastShown));
    }

    public int calculateDaysUntilNextPopup(int lastShownEpochDay) {
        int today = (int) LocalDate.now().toEpochDay();
        if (lastShownEpochDay < 0) {
            return 7; // 7 days after first-launch, show the donation popup
        }
        return Math.max(0, DONATION_INTERVAL_DAYS - (today - lastShownEpochDay));
    }

    public void showToast() {
        if (donationToast != null && rootPane.getChildren().contains(donationToast)) {
            return;
        }

        preferences.getDonationPreferences().setLastShownEpochDay((int) LocalDate.now().toEpochDay());

        Label title = new Label(Localization.lang("Support JabRef"));
        title.getStyleClass().add("donation-toast-title");
        Label subtitle = new Label(Localization.lang("Help us improve JabRef by donating."));
        subtitle.getStyleClass().add("donation-toast-desc");
        VBox textBox = new VBox(title, subtitle);
        textBox.getStyleClass().add("donation-toast-text");

        Node iconNode = IconTheme.JabRefIcons.DONATE.getGraphicNode();
        HBox leftContent = new HBox(10, iconNode, textBox);
        leftContent.setAlignment(Pos.CENTER_LEFT);

        Button neverButton = new Button(Localization.lang("Never show again"));
        neverButton.getStyleClass().add("donation-btn-ghost");
        neverButton.setOnAction(_ -> {
            preferences.getDonationPreferences().setNeverShowAgain(true);
            hideToast();
        });

        Button cancelButton = new Button(Localization.lang("Cancel"));
        cancelButton.getStyleClass().add("donation-btn-secondary");
        cancelButton.setOnAction(_ -> hideToast());

        Button donateButton = new Button(Localization.lang("Donate"));
        donateButton.getStyleClass().add("donation-btn-primary");
        donateButton.setDefaultButton(true);
        donateButton.setOnAction(_ -> {
            new OpenBrowserAction(DONATION_URL, dialogService, preferences.getExternalApplicationsPreferences()).execute();
            hideToast();
        });

        HBox rightButtons = new HBox(8, neverButton, cancelButton, donateButton);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);

        Region textSpacer = new Region();
        HBox.setHgrow(textSpacer, Priority.ALWAYS);

        donationToast = new HBox(leftContent, textSpacer, rightButtons);
        donationToast.getStyleClass().add("donation-toast");
        donationToast.setMaxWidth(Region.USE_PREF_SIZE);
        donationToast.setMinWidth(Region.USE_PREF_SIZE);
        donationToast.setTranslateY(-40);

        StackPane.setAlignment(donationToast, Pos.TOP_CENTER);
        StackPane.setMargin(donationToast, new Insets(16));
        rootPane.getChildren().add(donationToast);

        TranslateTransition slideDown = new TranslateTransition(Duration.millis(DONATION_POPUP_ANIM_MS), donationToast);
        slideDown.setFromY(-40);
        slideDown.setToY(0);
        slideDown.play();

        if (autoHide != null) {
            autoHide.cancel();
        }
        autoHide = new DelayedExecution(Duration.seconds(DONATION_POPUP_TIMEOUT_SECONDS), this::hideToast);
        autoHide.start();
    }

    private void hideToast() {
        if (donationToast == null) {
            return;
        }
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(DONATION_POPUP_ANIM_MS), donationToast);
        slideUp.setFromY(donationToast.getTranslateY());
        slideUp.setToY(-40);
        slideUp.setOnFinished(_ -> {
            rootPane.getChildren().remove(donationToast);
            donationToast = null;
            if (autoHide != null) {
                autoHide.cancel();
                autoHide = null;
            }
        });
        slideUp.play();
    }

    /// Schedules the next donation popup after a specified number of days to take care
    /// of situation where the user may leave the application open for an extended
    /// period.
    private void scheduleAfterDays(int days) {
        cancelScheduled();
        if (days <= 0) {
            showToast();
            scheduledShow = new DelayedExecution(Duration.millis(DONATION_INTERVAL_DAYS), this::showIfNeeded);
        } else {
            scheduledShow = new DelayedExecution(Duration.hours(days * 24), this::showIfNeeded);
        }
        scheduledShow.start();
    }

    private void cancelScheduled() {
        if (scheduledShow != null) {
            scheduledShow.cancel();
            scheduledShow = null;
        }
    }

    public void cleanUp() {
        cancelScheduled();
        if (autoHide != null) {
            autoHide.cancel();
            autoHide = null;
        }
        if (donationToast != null) {
            rootPane.getChildren().remove(donationToast);
            donationToast = null;
        }
    }
}
