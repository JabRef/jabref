package org.jabref.gui.groups;

import java.util.List;

import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class GroupColorPicker {

    // Generate color for top groups
    public static Color generateColor(List<Color> siblingColors) {
        return generateColor(siblingColors, null);
    }

    /**
     * Algorithm optimized for colors, not for gray-scale (where it does not work)
     */
    public static Color generateColor(List<Color> siblingColors, @Nullable Color parentColor) {
        if (siblingColors.isEmpty()) {
            if (parentColor == null) {
                // We need something colorful to derive other colors based on the color
                return Color.hsb(Math.random() * 360.0, .50, .75);
            }
            return generateSubGroupColor(parentColor);
        }

        double sumSin = 0;
        double sumCos = 0;

        // Calculate the mean angle
        for (Color color : siblingColors) {
            double hue = color.getHue();
            sumSin += Math.sin(Math.toRadians(hue));
            sumCos += Math.cos(Math.toRadians(hue));
        }

        double meanAngle = Math.toDegrees(Math.atan2(sumSin, sumCos));
        meanAngle = (meanAngle + 360) % 360;

        // The opposite angle is potentially the point of maximum average distance
        double newHue = (meanAngle + 180) % 360;

        double sumSaturation = 0;
        double sumBrightness = 0;
        for (Color color : siblingColors) {
            sumSaturation += color.getSaturation();
            sumBrightness += color.getBrightness();
        }

        double averageSaturation = sumSaturation / siblingColors.size();
        double averageBrightness = sumBrightness / siblingColors.size();

        return Color.hsb(newHue, averageSaturation, averageBrightness);
    }

    private static Color generateSubGroupColor(Color baseColor) {
        return baseColor.deriveColor(0.0, 1.0, .9, 1.0);
    }
}
