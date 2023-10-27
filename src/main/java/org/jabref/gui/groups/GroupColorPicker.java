package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;

public class GroupColorPicker {
    public static boolean useRandom = false;
    private static final Random RANDOM = new Random();
    private static final List<Color> TOP_LEVEL_COLORS = new ArrayList<>();
    private static final int TRIES = 10000;

    // Get most distinct color
    private static Color getDistinctHue() {
        Color mostDistinct = null;
        double maxOverallDifference = Double.MIN_VALUE;

        for (int i = 0; i < TRIES; i++) {
            Color randomColor = Color.hsb(RANDOM.nextDouble() * 360, 0.4, 0.7);
            double minDifference = Double.MAX_VALUE;

            for (Color pastColor : TOP_LEVEL_COLORS) {
                double difference = Math.abs(randomColor.getHue() - pastColor.getHue());
                if (difference < minDifference) {
                    minDifference = difference;
                }
            }

            if (minDifference > maxOverallDifference) {
                maxOverallDifference = minDifference;
                mostDistinct = randomColor;
            }
        }

        TOP_LEVEL_COLORS.add(mostDistinct);
        return mostDistinct;
    }

    // Generate color for top groups
    public static Color generateTopGroupColor() {
        return useRandom ? getDistinctHue() : IconTheme.getDefaultGroupColor();
    }

    // Generate color for subgroups
    public static Color generateSubGroupColor(Color baseColor) {
        double hue = baseColor.getHue();
        double saturation = baseColor.getSaturation();

        // Increase brightness but smaller than one
        double brightness = Math.min(1, baseColor.getBrightness() + 0.05);

        return Color.hsb(hue, saturation, brightness);
    }
}
