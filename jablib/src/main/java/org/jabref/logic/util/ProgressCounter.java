package org.jabref.logic.util;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.l10n.Localization;

import ai.djl.util.Progress;

/**
 * Convenient class for managing ETA for background tasks.
 * <p>
 * Always call {@link ProgressCounter#stop()} when your task is done, because there is a background timer that
 * periodically updates the ETA.
 */
public class ProgressCounter implements Progress {

    private record ProgressMessage(int maxTime, String message) {
    }

    // The list should be sorted by ProgressMessage.maxTime, smaller first.
    private static final List<ProgressMessage> PROGRESS_MESSAGES = List.of(
            new ProgressMessage(5, Localization.lang("Estimated time left: 5 seconds.")),
            new ProgressMessage(15, Localization.lang("Estimated time left: 15 seconds.")),
            new ProgressMessage(30, Localization.lang("Estimated time left: 30 seconds.")),
            new ProgressMessage(60, Localization.lang("Estimated time left: approx. 1 minute.")),
            new ProgressMessage(120, Localization.lang("Estimated time left: approx. 2 minutes.")),
            new ProgressMessage(Integer.MAX_VALUE, Localization.lang("Estimated time left: more than 2 minutes."))
    );

    private static final Duration PERIODIC_UPDATE_DURATION = Duration.ofSeconds(PROGRESS_MESSAGES.getFirst().maxTime);

    private final IntegerProperty workDone = new SimpleIntegerProperty(0);
    private final IntegerProperty workMax = new SimpleIntegerProperty(0);
    private final StringProperty message = new SimpleStringProperty("");

    // Progress counter is updated on two events:
    // 1) When workDone or workMax changes: this in normal behavior.
    // 2) When PERIODIC_UPDATE_DURATION passes: it is used in situations where one piece of work takes too much time
    //    (and so there are no events 1), and so the message could be "approx. 5 seconds", while it should be more.
    private final Timeline periodicUpdate = new Timeline(new KeyFrame(new javafx.util.Duration(PERIODIC_UPDATE_DURATION.getSeconds() * 1000), e -> update()));

    private final Instant workStartTime = Instant.now();

    public ProgressCounter() {
        periodicUpdate.setCycleCount(Timeline.INDEFINITE);
        periodicUpdate.play();

        workDone.addListener(obs -> update());
        workMax.addListener(obs -> update());
    }

    public void increaseWorkDone(int incr) {
        workDone.set(workDone.get() + incr);
    }

    public void increaseWorkMax(int incr) {
        workMax.set(workMax.get() + incr);
    }

    public IntegerProperty workDoneProperty() {
        return workDone;
    }

    public int getWorkDone() {
        return workDone.get();
    }

    public IntegerProperty workMaxProperty() {
        return workMax;
    }

    public int getWorkMax() {
        return workMax.get();
    }

    public ReadOnlyStringProperty messageProperty() {
        return message;
    }

    public String getMessage() {
        return message.get();
    }

    public void listenToAllProperties(Runnable runnable) {
        workDoneProperty().addListener(obs -> runnable.run());
        workMaxProperty().addListener(obs -> runnable.run());
        messageProperty().addListener(obs -> runnable.run());
    }

    private void update() {
        Duration workTime = Duration.between(workStartTime, Instant.now());
        Duration oneWorkTime = workTime.dividedBy(workDone.get() == 0 ? 1 : workDone.get());
        Duration eta = oneWorkTime.multipliedBy(workMax.get() - workDone.get() <= 0 ? 1 : workMax.get() - workDone.get());

        updateMessage(eta);
    }

    @Override
    public void reset(String message, long max, String trailingMessage) {
        workMax.set((int) max);
        workDone.set(0);
        // Ignoring message, because 1) it's not localized, 2) we supply our own message in updateMessage().
    }

    @Override
    public void start(long initialProgress) {
        workDone.set((int) initialProgress);
    }

    @Override
    public void end() {
        // Do nothing.
    }

    @Override
    public void increment(long increment) {
        workDone.set(workDone.get() + (int) increment);
    }

    @Override
    public void update(long progress, String message) {
        workDone.set((int) progress);
        // Ignoring message, because 1) it's not localized, 2) we supply our own message in updateMessage().
    }

    private void updateMessage(Duration eta) {
        for (ProgressMessage progressMessage : PROGRESS_MESSAGES) {
            if (eta.getSeconds() <= progressMessage.maxTime()) {
                message.set(progressMessage.message());
                break;
            }
        }
    }

    public void stop() {
        periodicUpdate.stop();
    }
}
