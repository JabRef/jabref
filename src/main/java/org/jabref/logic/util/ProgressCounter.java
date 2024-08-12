package org.jabref.logic.util;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.l10n.Localization;

import ai.djl.util.Progress;

public class ProgressCounter implements Progress {
    private final IntegerProperty workDone = new SimpleIntegerProperty(0);
    private final IntegerProperty workMax = new SimpleIntegerProperty(0);
    private final StringProperty message = new SimpleStringProperty("");
    private Instant oneWorkTimeStart = Instant.now();
    private Duration lastEtaCalculation = Duration.ofDays(1);

    public ProgressCounter() {
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
        Instant oneWorkTimeEnd = Instant.now();
        Duration duration = Duration.between(oneWorkTimeStart, oneWorkTimeEnd);
        oneWorkTimeStart = oneWorkTimeEnd;

        Duration eta = duration.multipliedBy(workMax.get() - workDone.get());

        if (lastEtaCalculation.minus(eta).isPositive()) {
            updateMessage(eta);

            lastEtaCalculation = eta;
        }
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

    private record ProgressMessage(int maxTime, String message) { }

    // The list should be sorted by ProgressMessage.maxTime, smaller first.
    private final List<ProgressMessage> PROGRESS_MESSAGES = List.of(
            new ProgressMessage(5, Localization.lang("Estimated time left: 5 seconds")),
            new ProgressMessage(15, Localization.lang("Estimated time left: 15 seconds")),
            new ProgressMessage(30, Localization.lang("Estimated time left: 30 seconds")),
            new ProgressMessage(60, Localization.lang("Estimated time left: approx. 1 minute")),
            new ProgressMessage(120, Localization.lang("Estimated time left: approx. 2 minutes")),
            new ProgressMessage(Integer.MAX_VALUE, Localization.lang("Estimated time left: more than 2 minutes"))
    );

    private void updateMessage(Duration eta) {
        for (ProgressMessage progressMessage : PROGRESS_MESSAGES) {
            if (eta.getSeconds() <= progressMessage.maxTime()) {
                message.set(progressMessage.message());
                break;
            }
        }
    }
}
