package com.auction.client.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class CountdownTimerUtil {

    private final Label daysLabel;
    private final Label hoursLabel;
    private final Label minsLabel;
    private final Label secsLabel;

    private Timeline timeline;

    public CountdownTimerUtil(Label daysLabel, Label hoursLabel, Label minsLabel, Label secsLabel) {
        this.daysLabel  = daysLabel;
        this.hoursLabel = hoursLabel;
        this.minsLabel  = minsLabel;
        this.secsLabel  = secsLabel;
    }
    public void startFor(LocalDateTime targetTime, Runnable onExpire) {
        stop();
        tick(targetTime, onExpire);

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick(targetTime, onExpire)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.playFromStart();
    }
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        updateLabels(0, 0, 0, 0);
    }

    //  Private helpers
    private void tick(LocalDateTime targetTime, Runnable onExpire) {
        LocalDateTime now = LocalDateTime.now();

        if (targetTime == null || now.isAfter(targetTime)) {
            updateLabels(0, 0, 0, 0);
            stop();
            if (onExpire != null) onExpire.run();
            return;
        }

        long days    = ChronoUnit.DAYS.between(now, targetTime);
        long hours   = ChronoUnit.HOURS.between(now, targetTime)   % 24;
        long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

        updateLabels(days, hours, minutes, seconds);
    }

    private void updateLabels(long d, long h, long m, long s) {
        if (daysLabel  != null) daysLabel.setText(String.format("%02d", d));
        if (hoursLabel != null) hoursLabel.setText(String.format("%02d", h));
        if (minsLabel  != null) minsLabel.setText(String.format("%02d", m));
        if (secsLabel  != null) secsLabel.setText(String.format("%02d", s));
    }
}